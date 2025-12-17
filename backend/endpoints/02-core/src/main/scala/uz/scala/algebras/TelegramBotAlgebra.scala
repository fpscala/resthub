package uz.scala.algebras

import cats.effect.MonadCancelThrow
import cats.implicits._
import doobie.implicits._
import io.circe.syntax._
import org.typelevel.log4cats.Logger
import telegramium.bots.ChatIntId
import telegramium.bots.InlineKeyboardButton
import telegramium.bots.InlineKeyboardMarkup
import telegramium.bots.InputLinkFile
import telegramium.bots.Message
import telegramium.bots.Update
import telegramium.bots.high.Api
import telegramium.bots.high.Methods
import telegramium.bots.high.implicits._

import uz.scala.Language
import uz.scala.domain.enums.BotState
import uz.scala.domain.enums.ListingStatus
import uz.scala.domain.enums.ListingType
import uz.scala.domain.listings.ListingFilters
import uz.scala.domain.listings.ListingOutput
import uz.scala.domain.telegram.AdminPostingContext
import uz.scala.domain.telegram.ForwardedMessage
import uz.scala.domain.telegram.SearchContext
import uz.scala.effects.Calendar
import uz.scala.effects.GenUUID
import uz.scala.repos.ListingsRepository
import uz.scala.repos.TelegramSessionsRepository
import uz.scala.repos.TelegramUsersRepository
import uz.scala.repos.dto
import uz.scala.shared.BotMessages
import uz.scala.shared.BotMessages.LISTING_PREVIEW_HEADER
import uz.scala.shared.TelegramKeyboards
import uz.scala.syntax.all.circeSyntaxJsonDecoderOps
import uz.scala.syntax.refined._
import uz.scala.utils.ID

trait TelegramBotAlgebra[F[_]] {
  def processUpdate(update: Update): F[Unit]
  def setupWebhook(): F[Unit]
}

object TelegramBotAlgebra {
  def make[F[_]: MonadCancelThrow: Calendar: GenUUID: Logger](
      api: Api[F],
      usersRepo: TelegramUsersRepository[doobie.ConnectionIO],
      sessionsRepo: TelegramSessionsRepository[doobie.ConnectionIO],
      listingsRepo: ListingsRepository[doobie.ConnectionIO],
      listingsAlgebra: ListingsAlgebra[F],
      citiesAlgebra: CitiesAlgebra[F],
      botToken: String,
      webhookBaseUrl: String,
    )(implicit
      xa: doobie.Transactor[F]
    ): TelegramBotAlgebra[F] =
    new Impl[F](
      api,
      usersRepo,
      sessionsRepo,
      listingsRepo,
      listingsAlgebra,
      citiesAlgebra,
      botToken,
      webhookBaseUrl,
    )

  private class Impl[F[_]: MonadCancelThrow: Calendar: GenUUID: Logger](
      api: Api[F],
      usersRepo: TelegramUsersRepository[doobie.ConnectionIO],
      sessionsRepo: TelegramSessionsRepository[doobie.ConnectionIO],
      listingsRepo: ListingsRepository[doobie.ConnectionIO],
      listingsAlgebra: ListingsAlgebra[F],
      citiesAlgebra: CitiesAlgebra[F],
      botToken: String,
      webhookBaseUrl: String,
    )(implicit
      xa: doobie.Transactor[F]
    ) extends TelegramBotAlgebra[F] {
    override def processUpdate(update: Update): F[Unit] =
      (update.message, update.callbackQuery) match {
        case (Some(msg), _) => handleMessage(msg)
        case (_, Some(callback)) => handleCallbackQuery(callback)
        case _ => Logger[F].debug("Received update without message or callback, ignoring")
      }

    private def handleMessage(msg: Message): F[Unit] =
      msg.from match {
        case Some(from) =>
          for {
            _ <- Logger[F].info(s"Processing message from user ${from.id}: ${msg.text}")

            // Get or create telegram user
            user <- getOrCreateUser(from)

            // Get or create session
            session <- getOrCreateSession(user.telegramId)

            // Update last interaction
            _ <- usersRepo.updateLastInteraction(user.telegramId).transact(xa)

            // Route based on message text or session state
            _ <- msg.text match {
              case Some("/start") => handleStartCommand(msg, user)
              case Some("/search") => handleSearchCommand(msg, user)
              case Some("/help") => handleHelpCommand(msg, user)
              case Some("/postadmin") => handleAdminPostingCommand(msg, user)
              case Some(text) => handleStateBasedMessage(msg, user, session, text)
              case None => handleForwardedMessage(msg, user, session)
            }
          } yield ()

        case None =>
          Logger[F].warn("Message without 'from' user, ignoring")
      }

    private def handleCallbackQuery(callback: telegramium.bots.CallbackQuery): F[Unit] =
      callback.message.fold(Logger[F].warn("Callback without message, ignoring")) {
        case msg: telegramium.bots.Message =>
          for {
            user <- usersRepo.findByTelegramId(callback.from.id).transact(xa)
            _ <- user match {
              case Some(u) =>
                callback.data match {
                  case Some(data) => handleCallbackData(msg, u, data)
                  case None => Logger[F].debug("Callback without data, ignoring")
                }
              case None =>
                Logger[F].warn(s"User not found for callback: ${callback.from.id}")
            }
          } yield ()
        case _: telegramium.bots.InaccessibleMessage =>
          Logger[F].warn("Received inaccessible message in callback")
      }

    private def handleCallbackData(
        msg: Message,
        user: dto.TelegramUser,
        data: String,
      ): F[Unit] = {

      Logger[F].info(s"Received callback data: $data from user ${user.telegramId}")

      data match {
        // City selection
        case cityData if cityData.startsWith("city_") =>
          val city = cityData.stripPrefix("city_")
          if (city == "other")
            for {
              _ <- sessionsRepo
                .updateState(user.telegramId, BotState.AwaitingCustomCity, None)
                .transact(xa)
              _ <- Methods
                .sendMessage(
                  chatId = ChatIntId(msg.chat.id),
                  text = BotMessages.ENTER_CUSTOM_CITY(user.languageCode),
                )
                .exec(api)
                .void
            } yield ()
          else
            handleCitySelection(msg, user, city)

        // Price range selection
        case priceData if priceData.startsWith("price_") =>
          priceData.stripPrefix("price_") match {
            case "200_500" => handlePriceSelection(msg, user, Some(200), Some(500))
            case "500_800" => handlePriceSelection(msg, user, Some(500), Some(800))
            case "800_1200" => handlePriceSelection(msg, user, Some(800), Some(1200))
            case "1200_2000" => handlePriceSelection(msg, user, Some(1200), Some(2000))
            case "custom" =>
              for {
                _ <- sessionsRepo
                  .updateState(user.telegramId, BotState.AwaitingCustomPriceMin, None)
                  .transact(xa)
                _ <- Methods
                  .sendMessage(
                    chatId = ChatIntId(msg.chat.id),
                    text =
                      s"${BotMessages.INVALID_PRICE(user.languageCode)}\n\n💰 Minimal price (USD):",
                  )
                  .exec(api)
                  .void
              } yield ()
          }

        // Room selection
        case roomsData if roomsData.startsWith("rooms_") =>
          val rooms = roomsData.stripPrefix("rooms_") match {
            case "skip" => None
            case "4+" => Some(4)
            case num => num.toIntOption
          }
          handleRoomSelection(msg, user, rooms)

        // Results navigation
        case "show_more" => handleShowMore(msg, user)
        case "new_search" => handleSearchCommand(msg, user)

        // Listing actions
        case "contact" => handleContactAction(msg, user)
        case "save" => handleSaveAction(msg, user)
        case "next" => handleNextListing(msg, user)

        // Admin posting actions
        case "admin_post_confirm" => handleAdminPostConfirm(msg, user)
        case "admin_post_cancel" => handleAdminPostCancel(msg, user)

        case _ =>
          Logger[F].debug(s"Unknown callback data: $data")
      }
    }

    private def getOrCreateUser(from: telegramium.bots.User): F[dto.TelegramUser] =
      for {
        userOpt <- usersRepo.findByTelegramId(from.id).transact(xa)
        user <- userOpt match {
          case Some(existing) => existing.pure[F]
          case None =>
            for {
              now <- Calendar[F].currentZonedDateTime
              newUser = dto.TelegramUser(
                telegramId = from.id,
                userId = None,
                username = from.username,
                firstName = from.firstName,
                languageCode =
                  from.languageCode.flatMap(Language.withNameOption).getOrElse(Language.Uz),
                isRegistered = false,
                createdAt = now,
                lastInteractionAt = now,
              )
              implicit0(lang: Language) = newUser.languageCode
              _ <- usersRepo.create(newUser).transact(xa)
              _ <- Logger[F].info(s"Created new telegram user: ${from.id}")
            } yield newUser
        }
      } yield user

    private def getOrCreateSession(telegramId: Long): F[dto.TelegramSession] =
      for {
        sessionOpt <- sessionsRepo.findByTelegramId(telegramId).transact(xa)
        session <- sessionOpt match {
          case Some(existing) => existing.pure[F]
          case None =>
            for {
              now <- Calendar[F].currentZonedDateTime
              newSession = dto.TelegramSession(
                telegramId = telegramId,
                state = BotState.Idle,
                context = None,
                updatedAt = now,
              )
              implicit0(lang: Language) = Language.En
              _ <- sessionsRepo.upsert(newSession).transact(xa)
              _ <- Logger[F].info(s"Created new session for user: $telegramId")
            } yield newSession
        }
      } yield session

    private def handleStartCommand(msg: Message, user: dto.TelegramUser): F[Unit] = {
      val welcomeText = BotMessages.WELCOME_NEW(user.languageCode)

      Methods
        .sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = welcomeText,
        )
        .exec(api)
        .void
    }

    private def handleSearchCommand(msg: Message, user: dto.TelegramUser): F[Unit] =
      for {
        // Update session state to AwaitingCity
        _ <- sessionsRepo
          .updateState(user.telegramId, BotState.AwaitingCity, None)
          .transact(xa)

        // Get cities from database
        cities <- citiesAlgebra.getAll

        // Create dynamic keyboard from cities
        cityKeyboard = createDynamicCityKeyboard(cities, user.languageCode)

        // Send city selection with inline keyboard
        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.SELECT_CITY(user.languageCode),
            replyMarkup = Some(cityKeyboard),
          )
          .exec(api)
          .void
      } yield ()

    private def handleHelpCommand(msg: Message, user: dto.TelegramUser): F[Unit] = {
      val helpText = BotMessages.WELCOME_NEW(user.languageCode)

      Methods
        .sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = helpText,
        )
        .exec(api)
        .void
    }

    private def handleStateBasedMessage(
        msg: Message,
        user: dto.TelegramUser,
        session: dto.TelegramSession,
        text: String,
      ): F[Unit] =
      session.state match {
        case BotState.Idle => Logger[F].debug("User is idle, ignoring message")
        case BotState.AwaitingCity => handleCityInput(msg, user, text)
        case BotState.AwaitingCustomCity => handleCustomCityInput(msg, user, text)
        case BotState.AwaitingPriceRange =>
          Logger[F].debug("Should use inline keyboard for price selection")
        case BotState.AwaitingCustomPriceMin => handleCustomPriceMin(msg, user, session, text)
        case BotState.AwaitingCustomPriceMax => handleCustomPriceMax(msg, user, session, text)
        case BotState.ViewingResults => handleResultsMessage(msg, user)
        case BotState.Registering => Logger[F].debug("Registration not implemented yet")
        // Admin posting states
        case BotState.AwaitingListingType => handleListingTypeInput(msg, user, text)
        case BotState.AwaitingPrice => handlePriceInput(msg, user, text)
        case BotState.AwaitingCityForPosting => handleCityForPostingInput(msg, user, text)
        case BotState.AwaitingRooms => handleRoomsInput(msg, user, text)
        case BotState.AwaitingPhone => handlePhoneInput(msg, user, text)
        case BotState.AwaitingDistrict => handleDistrictInput(msg, user, text)
        case BotState.AwaitingFloor => handleFloorInput(msg, user, text)
        case BotState.AwaitingTotalFloors => handleTotalFloorsInput(msg, user, text)
        case BotState.AwaitingBuildingType => handleBuildingTypeInput(msg, user, text)
        case BotState.AwaitingCondition => handleConditionInput(msg, user, text)
        case BotState.AwaitingConfirmation => handleConfirmationInput(msg, user, text)
        case BotState.AwaitingChannelSelection => handleChannelSelectionInput(msg, user, text)
      }

    private def handleCitySelection(
        msg: Message,
        user: dto.TelegramUser,
        city: String,
      ): F[Unit] = {
      val context = SearchContext(city = Some(city))

      for {
        _ <- sessionsRepo
          .updateState(
            user.telegramId,
            BotState.AwaitingPriceRange,
            Some(context.asJson),
          )
          .transact(xa)

        // Send price selection with inline keyboard
        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.SELECT_PRICE_RANGE(user.languageCode),
            replyMarkup = Some(TelegramKeyboards.priceRangeSelectionKeyboard(user.languageCode)),
          )
          .exec(api)
          .void
      } yield ()
    }

    private def handleCityInput(
        msg: Message,
        user: dto.TelegramUser,
        cityName: String,
      ): F[Unit] =
      handleCitySelection(msg, user, cityName.trim)

    private def handleCustomCityInput(
        msg: Message,
        user: dto.TelegramUser,
        cityName: String,
      ): F[Unit] = {
      val trimmedCity = cityName.trim
      if (trimmedCity.nonEmpty)
        handleCitySelection(msg, user, trimmedCity)
      else
        Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.ENTER_CUSTOM_CITY(user.languageCode),
          )
          .exec(api)
          .void
    }

    private def handlePriceSelection(
        msg: Message,
        user: dto.TelegramUser,
        minPrice: Option[Int],
        maxPrice: Option[Int],
      ): F[Unit] =
      for {
        // Get existing context
        session <- sessionsRepo.findByTelegramId(user.telegramId).transact(xa)
        searchContext <- session
          .flatMap(_.context)
          .map(_.decodeAsF[F, SearchContext])
          .getOrElse(SearchContext().pure[F])

        // Update context with price
        updatedContext = searchContext.copy(
          minPrice = minPrice.map(BigDecimal(_)),
          maxPrice = maxPrice.map(BigDecimal(_)),
        )

        // Update session to room selection
        _ <- sessionsRepo
          .updateState(
            user.telegramId,
            BotState.AwaitingPriceRange, // Reusing this state for room selection
            Some(updatedContext.asJson),
          )
          .transact(xa)

        // Send room selection with inline keyboard
        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.SELECT_ROOMS(user.languageCode),
            replyMarkup = Some(TelegramKeyboards.roomsSelectionKeyboard(user.languageCode)),
          )
          .exec(api)
          .void
      } yield ()

    private def handleCustomPriceMin(
        msg: Message,
        user: dto.TelegramUser,
        session: dto.TelegramSession,
        text: String,
      ): F[Unit] =
      text.toIntOption match {
        case Some(minPrice) =>
          for {
            searchContext <- session
              .context
              .map(_.decodeAsF[F, SearchContext])
              .getOrElse(SearchContext().pure[F])

            updatedContext = searchContext.copy(minPrice = Some(BigDecimal(minPrice)))

            _ <- sessionsRepo
              .updateState(
                user.telegramId,
                BotState.AwaitingCustomPriceMax,
                Some(updatedContext.asJson),
              )
              .transact(xa)

            _ <- Methods
              .sendMessage(
                chatId = ChatIntId(msg.chat.id),
                text = BotMessages.PROMPT_MAX_PRICE(user.languageCode),
              )
              .exec(api)
              .void
          } yield ()
        case None =>
          Methods
            .sendMessage(
              chatId = ChatIntId(msg.chat.id),
              text = BotMessages.INVALID_PRICE(user.languageCode),
            )
            .exec(api)
            .void
      }

    private def handleCustomPriceMax(
        msg: Message,
        user: dto.TelegramUser,
        session: dto.TelegramSession,
        text: String,
      ): F[Unit] =
      text.toIntOption match {
        case Some(maxPrice) =>
          for {
            searchContext <- session
              .context
              .map(_.decodeAsF[F, SearchContext])
              .getOrElse(SearchContext().pure[F])

            // Validate min < max
            minValid = searchContext.minPrice.forall(_ < maxPrice)

            _ <-
              if (minValid) {
                val updatedContext = searchContext.copy(maxPrice = Some(BigDecimal(maxPrice)))
                for {
                  _ <- sessionsRepo
                    .updateState(
                      user.telegramId,
                      BotState.AwaitingPriceRange,
                      Some(updatedContext.asJson),
                    )
                    .transact(xa)

                  _ <- Methods
                    .sendMessage(
                      chatId = ChatIntId(msg.chat.id),
                      text = BotMessages.SELECT_ROOMS(user.languageCode),
                      replyMarkup =
                        Some(TelegramKeyboards.roomsSelectionKeyboard(user.languageCode)),
                    )
                    .exec(api)
                    .void
                } yield ()
              }
              else
                Methods
                  .sendMessage(
                    chatId = ChatIntId(msg.chat.id),
                    text = BotMessages.INVALID_PRICE_RANGE(user.languageCode),
                  )
                  .exec(api)
                  .void
          } yield ()
        case None =>
          Methods
            .sendMessage(
              chatId = ChatIntId(msg.chat.id),
              text = BotMessages.INVALID_PRICE(user.languageCode),
            )
            .exec(api)
            .void
      }

    private def handleRoomSelection(
        msg: Message,
        user: dto.TelegramUser,
        rooms: Option[Int],
      ): F[Unit] =
      for {
        session <- sessionsRepo.findByTelegramId(user.telegramId).transact(xa)
        searchContext <- session
          .flatMap(_.context)
          .map(_.decodeAsF[F, SearchContext])
          .getOrElse(SearchContext().pure[F])

        updatedContext = searchContext.copy(
          rooms = rooms,
          page = 1,
        )

        _ <- performSearch(msg, user, updatedContext)
      } yield ()

    private def performSearch(
        msg: Message,
        user: dto.TelegramUser,
        searchContext: SearchContext,
      ): F[Unit] =
      for {
        // Show searching message
        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.SEARCHING(user.languageCode),
          )
          .exec(api)

        // Search listings
        filters = ListingFilters(
          city = searchContext.city,
          minPrice = searchContext.minPrice,
          maxPrice = searchContext.maxPrice,
          status = Some(ListingStatus.Approved),
          page = Some(searchContext.page),
          size = Some(5), // Show 5 results per page
        )

        result <- listingsAlgebra.search(filters)

        // Update context with results
        updatedContext = searchContext.copy(totalResults = result.total)

        _ <-
          if (result.data.nonEmpty)
            for {
              // Send results header
              _ <- Methods
                .sendMessage(
                  chatId = ChatIntId(msg.chat.id),
                  text = BotMessages.searchResultsHeader(result.total, user.languageCode),
                )
                .exec(api)

              // Send first listing with proper navigation
              _ <- sendListingWithNavigation(
                msg.chat.id,
                result.data.head,
                user,
                updatedContext,
                result.total,
              )

              // Update session
              _ <- sessionsRepo
                .updateState(
                  user.telegramId,
                  BotState.ViewingResults,
                  Some(updatedContext.asJson),
                )
                .transact(xa)
            } yield ()
          else
            for {
              _ <- Methods
                .sendMessage(
                  chatId = ChatIntId(msg.chat.id),
                  text = BotMessages.NO_RESULTS(user.languageCode),
                  replyMarkup = Some(
                    TelegramKeyboards.resultsNavigationKeyboard(hasMore = false, user.languageCode)
                  ),
                )
                .exec(api)

              // Reset state
              _ <- sessionsRepo
                .updateState(user.telegramId, BotState.Idle, None)
                .transact(xa)
            } yield ()
      } yield ()

    private def sendListingWithNavigation(
        chatId: Long,
        listing: ListingOutput,
        user: dto.TelegramUser,
        context: SearchContext,
        totalResults: Long,
      ): F[Unit] = {

      val listingText = BotMessages.formatListing(
        title = listing.title.value,
        city = listing.city.value,
        price = listing.price.amount,
        rooms = 2, // Default value since not in model
        size = None, // Not in current model
        furnished = true, // Default value
        contact = listing.owner.firstName.value,
        lang = user.languageCode,
      )

      // Send photos if available
      val photoEffect = if (listing.images.nonEmpty) {
        // Send all photos as a media group with the last one having caption
        val mediaInputs = listing.images.zipWithIndex.map {
          case (imgUrl, index) =>
            val isLast = index == listing.images.size - 1
            telegramium
              .bots
              .InputMediaPhoto(
                media = InputLinkFile(imgUrl),
                caption = if (isLast) Some(listingText) else None,
                parseMode = None,
              )
        }
        for {
          _ <- Logger[F].info(s"Sending listing with ${listing.images.size} photos")
          _ <- Methods
            .sendMediaGroup(
              chatId = ChatIntId(chatId),
              media = mediaInputs,
            )
            .exec(api)
          hasMore = totalResults > context.page * 5 // 5 results per page
          actionKeyboard = TelegramKeyboards.listingActionButtons(user.languageCode)
          navKeyboard = TelegramKeyboards.resultsNavigationKeyboard(hasMore, user.languageCode)

          // Combine keyboards - action buttons first, then navigation
          combinedKeyboard = telegramium
            .bots
            .InlineKeyboardMarkup(
              actionKeyboard.inlineKeyboard ++ navKeyboard.inlineKeyboard
            )

          _ <- Methods
            .sendMessage(
              chatId = ChatIntId(chatId),
              text = BotMessages.WHAT_WOULD_YOU_LIKE_TO_DO(user.languageCode),
              replyMarkup = Some(combinedKeyboard),
            )
            .exec(api)
        } yield {}
      }
      else
        // No photos, send text message
        Methods
          .sendMessage(
            chatId = ChatIntId(chatId),
            text = listingText,
            replyMarkup = Some(TelegramKeyboards.listingActionButtons(user.languageCode)),
          )
          .exec(api)
          .void

      // Create combined keyboard with listing actions and navigation
      val hasMore = totalResults > context.page * 5 // 5 results per page
      val navKeyboard = TelegramKeyboards.resultsNavigationKeyboard(hasMore, user.languageCode)

      photoEffect.flatTap(_ =>
        Methods
          .sendMessage(
            chatId = ChatIntId(chatId),
            text = BotMessages.NEED_MORE_OPTIONS(user.languageCode),
            replyMarkup = Some(navKeyboard),
          )
          .exec(api)
      )
    }

    private def handleResultsMessage(msg: Message, user: dto.TelegramUser): F[Unit] =
      Methods
        .sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = BotMessages.USE_BUTTONS_TO_NAVIGATE(user.languageCode),
        )
        .exec(api)
        .void

    private def handleShowMore(msg: Message, user: dto.TelegramUser): F[Unit] =
      for {
        session <- sessionsRepo.findByTelegramId(user.telegramId).transact(xa)
        _ <- session.flatMap(_.context) match {
          case Some(contextJson) =>
            for {
              searchContext <- contextJson.decodeAsF[F, SearchContext]
              newContext = searchContext.copy(page = searchContext.page + 1)
              _ <- Logger[F].info(s"Showing more results, page: ${newContext.page}")
              _ <- performSearch(msg, user, newContext)
            } yield ()
          case None =>
            Logger[F].warn("No search context found for show more")
        }
      } yield ()

    private def handleContactAction(msg: Message, user: dto.TelegramUser): F[Unit] =
      Methods
        .sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = BotMessages.CONTACT_FEATURE_COMING_SOON(user.languageCode),
        )
        .exec(api)
        .void

    private def handleSaveAction(msg: Message, user: dto.TelegramUser): F[Unit] =
      Methods
        .sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = BotMessages.SAVE_FEATURE_COMING_SOON(user.languageCode),
        )
        .exec(api)
        .void

    private def handleNextListing(msg: Message, user: dto.TelegramUser): F[Unit] =
      for {
        session <- sessionsRepo.findByTelegramId(user.telegramId).transact(xa)
        _ <- session.flatMap(_.context) match {
          case Some(contextJson) =>
            for {
              searchContext <- contextJson.decodeAsF[F, SearchContext]
              _ <- Logger[F].info(s"Next listing requested, current page: ${searchContext.page}")

              // Get next listing on current page
              filters = ListingFilters(
                city = searchContext.city,
                minPrice = searchContext.minPrice,
                maxPrice = searchContext.maxPrice,
                status = Some(ListingStatus.Approved),
                page = Some(searchContext.page),
                size = Some(5),
              )

              result <- listingsAlgebra.search(filters)

              _ <-
                if (result.data.size > 1) {
                  // Find current listing index and show next one
                  val currentIndex = (searchContext.page - 1) * 5 % result.data.size
                  val nextIndex = (currentIndex + 1)              % result.data.size
                  val nextListing = result.data(nextIndex)

                  // Send next listing with navigation
                  sendListingWithNavigation(
                    msg.chat.id,
                    nextListing,
                    user,
                    searchContext,
                    result.total,
                  )
                }
                else
                  // No more listings on this page

                  Methods
                    .sendMessage(
                      chatId = ChatIntId(msg.chat.id),
                      text = BotMessages.LAST_LISTING_ON_PAGE(user.languageCode),
                      replyMarkup = Some(
                        TelegramKeyboards.resultsNavigationKeyboard(
                          hasMore = result.total > searchContext.page * 5,
                          user.languageCode,
                        )
                      ),
                    )
                    .exec(api)
                    .void
            } yield ()
          case None =>
            Logger[F].warn("No search context found for next listing")
        }
      } yield ()

    private def createDynamicCityKeyboard(
        cities: List[uz.scala.domain.cities.City],
        language: Language,
      ): InlineKeyboardMarkup = {
      // Get city names and filter out empty names
      val cityNames = cities.map(_.name).filter(_.trim.nonEmpty)

      val otherCityText = BotMessages.OTHER_CITY(language)

      // Create city buttons (max 5 per row to avoid overcrowding)
      val cityButtons = cityNames.map { cityName =>
        InlineKeyboardButton(cityName, callbackData = Some(s"city_$cityName"))
      }

      // Add "Other city" button
      val otherButton = InlineKeyboardButton(otherCityText, callbackData = Some("city_other"))

      // Group buttons in rows of 2 for better UX
      val allButtons = (cityButtons :+ otherButton).grouped(2).toList

      InlineKeyboardMarkup(allButtons)
    }

    private def handleAdminPostingCommand(msg: Message, user: dto.TelegramUser): F[Unit] =
      Methods
        .sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = BotMessages.ADMIN_POST_PROMPT(user.languageCode),
        )
        .exec(api)
        .void

    private def handleForwardedMessage(
        msg: Message,
        user: dto.TelegramUser,
        session: dto.TelegramSession,
      ): F[Unit] =
      // Check if this is a forwarded message
      msg.senderChat match {
        case Some(chat) =>
          // This is a forwarded message from a channel
          val context = AdminPostingContext(
            forwardedMessage = ForwardedMessage(
              text = msg.text,
              images = List.empty, // TODO: Extract images from forwarded message
              originalAuthor = chat.title,
            ),
            listingType = None,
            price = None,
            city = None,
            rooms = None,
            phone = None,
            district = None,
            floor = None,
            totalFloors = None,
            buildingType = None,
            condition = None,
            selectedChannelId = Some(chat.id),
            step = 1,
          )

          val confirmText = BotMessages.ADMIN_POST_CONFIRM(user.languageCode)

          for {
            _ <- sessionsRepo
              .updateState(user.telegramId, BotState.Registering, Some(context.asJson))
              .transact(xa)
            _ <- Methods
              .sendMessage(
                chatId = ChatIntId(msg.chat.id),
                text = confirmText,
                replyMarkup = Some(
                  InlineKeyboardMarkup(
                    List(
                      List(
                        InlineKeyboardButton(
                          BotMessages.BUTTON_CONFIRM_POST(user.languageCode),
                          callbackData = Some("admin_post_confirm"),
                        ),
                        InlineKeyboardButton(
                          BotMessages.BUTTON_CANCEL_POST(user.languageCode),
                          callbackData = Some("admin_post_cancel"),
                        ),
                      )
                    )
                  )
                ),
              )
              .exec(api)
              .void
          } yield ()
        case None =>
          // Not a forwarded message, check if user is in admin posting flow
          session.state match {
            case BotState.Registering =>
              Logger[F].debug("User is in admin posting flow but received non-forwarded message")
            case _ =>
              Logger[F].debug(s"Ignoring non-forwarded message from user ${user.telegramId}")
          }
      }

    private def handleListingTypeInput(
        msg: Message,
        user: dto.TelegramUser,
        text: String,
      ): F[Unit] = {

      val normalized = text.toLowerCase.trim

      val listingType = normalized match {
        case s if s.contains("ijar") || s.contains("rent") || s.contains("аренд") =>
          Some(ListingType.ForRent)
        case s if s.contains("sotil") || s.contains("sale") || s.contains("прода") =>
          Some(ListingType.ForSale)
        case _ => None
      }

      listingType match {
        case Some(lt) =>
          updateAdminContextAndNextStep(
            user.telegramId,
            msg.chat.id,
            _.copy(listingType = Some(lt), step = 2),
            BotState.AwaitingPrice,
            user.languageCode,
          )
        case None =>
          val errorText = BotMessages.ERROR_INVALID_LISTING_TYPE(user.languageCode)
          Methods
            .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
            .exec(api)
            .void
      }
    }

    private def handlePriceInput(
        msg: Message,
        user: dto.TelegramUser,
        text: String,
      ): F[Unit] =
      text.replaceAll("[^0-9.]", "").toDoubleOption match {
        case Some(price) =>
          updateAdminContextAndNextStep(
            user.telegramId,
            msg.chat.id,
            _.copy(price = Some(BigDecimal(price)), step = 3),
            BotState.AwaitingCityForPosting,
            user.languageCode,
          )
        case None =>
          val errorText = BotMessages.ERROR_INVALID_PRICE(user.languageCode)
          Methods
            .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
            .exec(api)
            .void
      }

    private def handleCityForPostingInput(
        msg: Message,
        user: dto.TelegramUser,
        text: String,
      ): F[Unit] = {

      val city = text.trim

      if (city.nonEmpty)
        updateAdminContextAndNextStep(
          user.telegramId,
          msg.chat.id,
          _.copy(city = Some(city), step = 4),
          BotState.AwaitingRooms,
          user.languageCode,
        )
      else {
        val errorText = BotMessages.ERROR_EMPTY_CITY(user.languageCode)
        Methods
          .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
          .exec(api)
          .void
      }
    }

    private def handleRoomsInput(
        msg: Message,
        user: dto.TelegramUser,
        text: String,
      ): F[Unit] = {

      val normalized = text.toLowerCase.trim

      if (normalized == "skip" || normalized == "tashlab" || normalized == "пропустить")
        updateAdminContextAndNextStep(
          user.telegramId,
          msg.chat.id,
          _.copy(rooms = None, step = 5),
          BotState.AwaitingPhone,
          user.languageCode,
        )
      else
        normalized.toIntOption match {
          case Some(rooms) if rooms > 0 =>
            updateAdminContextAndNextStep(
              user.telegramId,
              msg.chat.id,
              _.copy(rooms = Some(rooms), step = 5),
              BotState.AwaitingPhone,
              user.languageCode,
            )
          case _ =>
            val errorText = BotMessages.ERROR_INVALID_ROOMS(user.languageCode)
            Methods
              .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
              .exec(api)
              .void
        }
    }

    private def handlePhoneInput(
        msg: Message,
        user: dto.TelegramUser,
        text: String,
      ): F[Unit] = {

      val phone = text.replaceAll("[^0-9+]", "")

      if (phone.nonEmpty && phone.length >= 9)
        updateAdminContextAndNextStep(
          user.telegramId,
          msg.chat.id,
          _.copy(phone = Some(phone), step = 6),
          BotState.AwaitingDistrict,
          user.languageCode,
        )
      else {
        val errorText = BotMessages.ERROR_INVALID_PHONE(user.languageCode)
        Methods
          .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
          .exec(api)
          .void
      }
    }

    private def handleDistrictInput(
        msg: Message,
        user: dto.TelegramUser,
        text: String,
      ): F[Unit] = {

      val normalized = text.toLowerCase.trim

      if (normalized == "skip" || normalized == "tashlab" || normalized == "пропустить")
        updateAdminContextAndNextStep(
          user.telegramId,
          msg.chat.id,
          _.copy(district = None, step = 7),
          BotState.AwaitingFloor,
          user.languageCode,
        )
      else {
        val district = text.trim
        if (district.nonEmpty)
          updateAdminContextAndNextStep(
            user.telegramId,
            msg.chat.id,
            _.copy(district = Some(district), step = 7),
            BotState.AwaitingFloor,
            user.languageCode,
          )
        else {
          val errorText = BotMessages.ERROR_INVALID_DISTRICT(user.languageCode)
          Methods
            .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
            .exec(api)
            .void
        }
      }
    }

    private def handleFloorInput(
        msg: Message,
        user: dto.TelegramUser,
        text: String,
      ): F[Unit] = {

      val normalized = text.toLowerCase.trim

      if (normalized == "skip" || normalized == "tashlab" || normalized == "пропустить")
        updateAdminContextAndNextStep(
          user.telegramId,
          msg.chat.id,
          _.copy(floor = None, step = 8),
          BotState.AwaitingTotalFloors,
          user.languageCode,
        )
      else
        normalized.toIntOption match {
          case Some(floor) if floor > 0 =>
            updateAdminContextAndNextStep(
              user.telegramId,
              msg.chat.id,
              _.copy(floor = Some(floor), step = 8),
              BotState.AwaitingTotalFloors,
              user.languageCode,
            )
          case _ =>
            val errorText = BotMessages.ERROR_INVALID_FLOOR(user.languageCode)
            Methods
              .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
              .exec(api)
              .void
        }
    }

    private def handleTotalFloorsInput(
        msg: Message,
        user: dto.TelegramUser,
        text: String,
      ): F[Unit] = {

      val normalized = text.toLowerCase.trim

      if (normalized == "skip" || normalized == "tashlab" || normalized == "пропустить")
        updateAdminContextAndNextStep(
          user.telegramId,
          msg.chat.id,
          _.copy(totalFloors = None, step = 9),
          BotState.AwaitingBuildingType,
          user.languageCode,
        )
      else
        normalized.toIntOption match {
          case Some(totalFloors) if totalFloors > 0 =>
            updateAdminContextAndNextStep(
              user.telegramId,
              msg.chat.id,
              _.copy(totalFloors = Some(totalFloors), step = 9),
              BotState.AwaitingBuildingType,
              user.languageCode,
            )
          case _ =>
            val errorText = BotMessages.ERROR_INVALID_TOTAL_FLOORS(user.languageCode)
            Methods
              .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
              .exec(api)
              .void
        }
    }

    private def handleBuildingTypeInput(
        msg: Message,
        user: dto.TelegramUser,
        text: String,
      ): F[Unit] = {

      val normalized = text.toLowerCase.trim

      if (normalized == "skip" || normalized == "tashlab" || normalized == "пропустить")
        updateAdminContextAndNextStep(
          user.telegramId,
          msg.chat.id,
          _.copy(buildingType = None, step = 10),
          BotState.AwaitingCondition,
          user.languageCode,
        )
      else {
        val buildingType = normalized match {
          case s if s.contains("kvartir") || s.contains("квартир") => Some("Kvartira")
          case s if s.contains("hovli") || s.contains("дом") => Some("Hovli")
          case s if s.contains("ofis") || s.contains("офис") => Some("Ofis")
          case _ => None
        }

        buildingType match {
          case Some(bt) =>
            updateAdminContextAndNextStep(
              user.telegramId,
              msg.chat.id,
              _.copy(buildingType = Some(bt), step = 10),
              BotState.AwaitingCondition,
              user.languageCode,
            )
          case None =>
            val errorText = BotMessages.ERROR_INVALID_BUILDING_TYPE(user.languageCode)
            Methods
              .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
              .exec(api)
              .void
        }
      }
    }

    private def handleConditionInput(
        msg: Message,
        user: dto.TelegramUser,
        text: String,
      ): F[Unit] = {

      val normalized = text.toLowerCase.trim

      if (normalized == "skip" || normalized == "tashlab" || normalized == "пропустить")
        // All fields collected, show preview
        showListingPreview(user.telegramId, msg.chat.id, user.languageCode)
      else {
        val condition = normalized match {
          case s if s.contains("yaxshi") || s.contains("хорош") => Some("Yaxshi")
          case s if s.contains("z'or") || s.contains("отлич") => Some("Zo‘r")
          case s if s.contains("ta'mir") || s.contains("ремонт") => Some("Ta'mirlangan")
          case _ => None
        }

        condition match {
          case Some(c) =>
            updateAdminContextAndNextStep(
              user.telegramId,
              msg.chat.id,
              _.copy(condition = Some(c), step = 11),
              BotState.AwaitingConfirmation,
              user.languageCode,
            )
          case None =>
            val errorText = BotMessages.ERROR_INVALID_CONDITION(user.languageCode)
            Methods
              .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
              .exec(api)
              .void
        }
      }
    }

    private def handleConfirmationInput(
        msg: Message,
        user: dto.TelegramUser,
        text: String,
      ): F[Unit] = {

      val normalized = text.toLowerCase.trim

      if (normalized == "ha" || normalized == "yes" || normalized == "да") {
        // TODO: Create listing and check permissions
        val successText = BotMessages.LISTING_POSTED_SUCCESSFULLY(user.languageCode)
        Methods
          .sendMessage(chatId = ChatIntId(msg.chat.id), text = successText)
          .exec(api)
          .void
      }
      else if (normalized == "yo'q" || normalized == "no" || normalized == "нет")
        // Cancel posting
        sessionsRepo.updateState(user.telegramId, BotState.Idle, None).transact(xa) *>
          Methods
            .sendMessage(
              chatId = ChatIntId(msg.chat.id),
              text = BotMessages.CANCELLED(user.languageCode),
            )
            .exec(api)
            .void
      else {
        val errorText = BotMessages.PROMPT_CONFIRM_YES_NO(user.languageCode)
        Methods
          .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
          .exec(api)
          .void
      }
    }

    private def handleChannelSelectionInput(
        msg: Message,
        user: dto.TelegramUser,
        text: String,
      ): F[Unit] = {

      val channelId = text.trim.toLongOption

      channelId match {
        case Some(id) =>
          updateAdminContextAndNextStep(
            user.telegramId,
            msg.chat.id,
            _.copy(selectedChannelId = Some(id)),
            BotState.AwaitingListingType,
            user.languageCode,
          )
        case None =>
          val errorText = BotMessages.ERROR_INVALID_CHANNEL_ID(user.languageCode)
          Methods
            .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
            .exec(api)
            .void
      }
    }

    private def updateAdminContextAndNextStep(
        telegramId: Long,
        chatId: Long,
        updateFn: AdminPostingContext => AdminPostingContext,
        nextState: BotState,
        lang: Language,
      ): F[Unit] =
      for {
        session <- sessionsRepo.findByTelegramId(telegramId).transact(xa)
        context <- session
          .flatMap(_.context)
          .map(_.decodeAsF[F, AdminPostingContext])
          .getOrElse(
            AdminPostingContext(
              forwardedMessage = ForwardedMessage(None, List.empty, None),
              listingType = None,
              price = None,
              city = None,
              rooms = None,
              phone = None,
              district = None,
              floor = None,
              totalFloors = None,
              buildingType = None,
              condition = None,
              selectedChannelId = None,
            ).pure[F]
          )

        updatedContext = updateFn(context)

        _ <- sessionsRepo
          .updateState(telegramId, nextState, Some(updatedContext.asJson))
          .transact(xa)

        promptText = getNextStepPrompt(nextState, lang)
        _ <- Methods
          .sendMessage(chatId = ChatIntId(chatId), text = promptText)
          .exec(api)
          .void
      } yield ()

    private def getNextStepPrompt(state: BotState, lang: Language): String =
      state match {
        case BotState.AwaitingListingType => BotMessages.PROMPT_LISTING_TYPE(lang)
        case BotState.AwaitingPrice => BotMessages.PROMPT_PRICE(lang)
        case BotState.AwaitingCityForPosting => BotMessages.PROMPT_CITY(lang)
        case BotState.AwaitingRooms => BotMessages.PROMPT_ROOMS(lang)
        case BotState.AwaitingPhone => BotMessages.PROMPT_PHONE(lang)
        case BotState.AwaitingDistrict => BotMessages.PROMPT_DISTRICT(lang)
        case BotState.AwaitingFloor => BotMessages.PROMPT_FLOOR(lang)
        case BotState.AwaitingTotalFloors => BotMessages.PROMPT_TOTAL_FLOORS(lang)
        case BotState.AwaitingBuildingType => BotMessages.PROMPT_BUILDING_TYPE(lang)
        case BotState.AwaitingCondition => BotMessages.PROMPT_CONDITION(lang)
        case _ => ""
      }

    private def showListingPreview(
        telegramId: Long,
        chatId: Long,
        lang: Language,
      ): F[Unit] =
      for {
        session <- sessionsRepo.findByTelegramId(telegramId).transact(xa)
        context <- session
          .flatMap(_.context)
          .map(_.decodeAsF[F, AdminPostingContext])
          .getOrElse(
            AdminPostingContext(
              forwardedMessage = ForwardedMessage(None, List.empty, None),
              listingType = None,
              price = None,
              city = None,
              rooms = None,
              phone = None,
              district = None,
              floor = None,
              totalFloors = None,
              buildingType = None,
              condition = None,
              selectedChannelId = None,
            ).pure[F]
          )

        previewText = formatPreviewText(context, lang)

        _ <- sessionsRepo
          .updateState(telegramId, BotState.AwaitingConfirmation, Some(context.asJson))
          .transact(xa)

        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(chatId),
            text = previewText,
            replyMarkup = Some(
              InlineKeyboardMarkup(
                List(
                  List(
                    InlineKeyboardButton(
                      BotMessages.BUTTON_CONFIRM_POST(lang),
                      callbackData = Some("admin_post_confirm"),
                    ),
                    InlineKeyboardButton(
                      BotMessages.BUTTON_CANCEL_POST(lang),
                      callbackData = Some("admin_post_cancel"),
                    ),
                  )
                )
              )
            ),
          )
          .exec(api)
          .void
      } yield ()

    private def handleAdminPostConfirm(msg: Message, user: dto.TelegramUser): F[Unit] =
      for {
        session <- sessionsRepo.findByTelegramId(user.telegramId).transact(xa)
        contextOpt <- session
          .flatMap(_.context)
          .map(_.decodeAsF[F, AdminPostingContext])
          .sequence

        _ <- contextOpt match {
          case Some(context) =>
            // Check if admin has linked user account
            user.userId match {
              case Some(userId) =>
                // Check permissions with Telegram API
                checkAndCreateListing(
                  user.telegramId,
                  userId,
                  context,
                  msg.chat.id,
                  user.languageCode,
                )
              case None =>
                val errorText = BotMessages.NEED_TO_REGISTER_FIRST(user.languageCode)
                Methods
                  .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
                  .exec(api)
                  .void
            }
          case None =>
            Logger[F].warn(s"No admin posting context found for user ${user.telegramId}") *>
              Methods
                .sendMessage(
                  chatId = ChatIntId(msg.chat.id),
                  text = BotMessages.ERROR_NO_CONTEXT_FOUND(user.languageCode),
                )
                .exec(api)
                .void
        }
      } yield ()

    private def handleAdminPostCancel(msg: Message, user: dto.TelegramUser): F[Unit] = {

      val cancelText = BotMessages.LISTING_POSTING_CANCELLED(user.languageCode)

      for {
        _ <- sessionsRepo
          .updateState(user.telegramId, BotState.Idle, None)
          .transact(xa)
        _ <- Methods
          .sendMessage(chatId = ChatIntId(msg.chat.id), text = cancelText)
          .exec(api)
          .void
      } yield ()
    }

    private def checkAndCreateListing(
        telegramId: Long,
        userId: uz.scala.domain.UserId,
        context: AdminPostingContext,
        chatId: Long,
        lang: Language,
      ): F[Unit] =
      // Check if user has admin privileges
      // TODO: Check user privileges from database

      // For now, proceed with permission check using Telegram API
      context.selectedChannelId match {
        case Some(channelId) =>
          for {
            // Check chat member permissions
            memberResult <- Methods
              .getChatMember(
                chatId = ChatIntId(channelId),
                userId = telegramId,
              )
              .exec(api)

            _ <- memberResult match {
              case iozhik.OpenEnum.Known(chatMember) =>
                chatMember match {
                  case owner: telegramium.bots.ChatMemberOwner =>
                    // Owner always has posting rights
                    createListingFromContext(userId, owner.user.id, context, chatId, lang)
                  case admin: telegramium.bots.ChatMemberAdministrator =>
                    // Administrator can post if canPostMessages is true or not set
                    if (admin.canPostMessages.getOrElse(true))
                      createListingFromContext(userId, admin.user.id, context, chatId, lang)
                    else {
                      val errorText = BotMessages.PERMISSION_DENIED(lang)
                      Methods
                        .sendMessage(chatId = ChatIntId(chatId), text = errorText)
                        .exec(api)
                        .void
                    }
                  case _ =>
                    // Regular members, restricted, left, or banned users cannot post
                    val errorText = lang match {
                      case Language.Uz => "Sizda ushbu kanalda e'lon joylash uchun ruxsat yo'q!"
                      case Language.Ru => "У вас нет прав для размещения объявлений в этом канале!"
                      case _ => "You don't have permission to post in this channel!"
                    }
                    Methods
                      .sendMessage(chatId = ChatIntId(chatId), text = errorText)
                      .exec(api)
                      .void
                }
              case iozhik.OpenEnum.Unknown(member) =>
                // Unknown member type, err on the side of caution
                Logger[F].warn(
                  s"Unknown chat member type for user $telegramId in channel $channelId: $member"
                ) *>
                  Methods
                    .sendMessage(
                      chatId = ChatIntId(chatId),
                      text = BotMessages.ERROR_CHECKING_PERMISSIONS(lang),
                    )
                    .exec(api)
                    .void
            }
          } yield ()
        case None =>
          Logger[F].warn(s"No channel selected for admin posting by user $telegramId") *>
            Methods
              .sendMessage(chatId = ChatIntId(chatId), text = BotMessages.NO_CHANNEL_SELECTED(lang))
              .exec(api)
              .void
      }

    private def createListingFromContext(
        userId: uz.scala.domain.UserId,
        telegramUserId: Long,
        context: AdminPostingContext,
        chatId: Long,
        lang: Language,
      ): F[Unit] = {
      // Build title from original message or use a default
      val titleText = context
        .forwardedMessage
        .text
        .map(text => if (text.length > 255) text.take(252) + "..." else text)
        .getOrElse(BotMessages.DEFAULT_LISTING_TITLE(lang))

      // Create the listing with all structured data
      context.listingType match {
        case Some(listingType) =>
          // Use Money type for price
          val priceAmount = context.price.getOrElse(BigDecimal(0))
          val price = squants.market.USD(priceAmount)

          for {
            // Generate listing ID
            listingId <- ID.make[F, uz.scala.domain.ListingId]
            now <- Calendar[F].currentZonedDateTime
            city = context.city.getOrElse("")
            description = context.forwardedMessage.text.getOrElse("")
            // Create listing DTO with proper refined types
            listingDto =
              uz.scala
                .repos
                .dto
                .Listing(
                  id = listingId,
                  ownerId = userId,
                  title = titleText,
                  description = description,
                  price = price,
                  city = city,
                  images = context.forwardedMessage.images,
                  status = uz.scala.domain.enums.ListingStatus.Pending,
                  rejectionReason = None,
                  listingType = listingType,
                  rooms = context.rooms,
                  district = context.district,
                  floor = context.floor,
                  totalFloors = context.totalFloors,
                  buildingType = context.buildingType,
                  condition = context.condition,
                  createdAt = now,
                  updatedAt = now,
                  approvedAt = None,
                  approvedBy = None,
                )

            // Save to database
            _ <- listingsRepo.create(listingDto)(lang).transact(xa)

            successText = BotMessages.LISTING_POSTED_PENDING_MODERATION(lang)

            _ <- sessionsRepo
              .updateState(telegramUserId, BotState.Idle, None)
              .transact(xa)

            _ <- Methods
              .sendMessage(chatId = ChatIntId(chatId), text = successText)
              .exec(api)
          } yield ()
        case None =>
          val errorText = BotMessages.ERROR_LISTING_TYPE_NOT_SPECIFIED(lang)
          Methods
            .sendMessage(chatId = ChatIntId(chatId), text = errorText)
            .exec(api)
            .void
      }
    }

    private def formatPreviewText(
        context: uz.scala.domain.telegram.AdminPostingContext,
        lang: Language,
      ): String = {
      val header = LISTING_PREVIEW_HEADER(lang)

      val typeStr = context.listingType.map(_.valueUz).getOrElse("")
      val priceStr = context.price.map(p => s"$$$p").getOrElse("")
      val cityStr = context.city.getOrElse("")
      val roomsStr = context.rooms.map(r => s"$r xona").getOrElse("")
      val phoneStr = context.phone.getOrElse("")
      val districtStr = context.district.getOrElse("")
      val floorStr = context.floor.map(f => s"$f-qavat").getOrElse("")
      val totalFloorsStr = context.totalFloors.map(tf => s"$tf qavatli").getOrElse("")
      val buildingTypeStr = context.buildingType.getOrElse("")
      val conditionStr = context.condition.getOrElse("")

      val originalText =
        context.forwardedMessage.text.map(text => s"📄 Asl matn:\n$text\n").getOrElse("")
      val districtLine = if (districtStr.nonEmpty) s"📍 Tuman: $districtStr\n" else ""
      val floorLine = if (floorStr.nonEmpty) s"🏢 Qavat: $floorStr\n" else ""
      val totalFloorsLine = if (totalFloorsStr.nonEmpty) s"🏢 Bino: $totalFloorsStr\n" else ""
      val buildingTypeLine = if (buildingTypeStr.nonEmpty) s"🏢 Turi: $buildingTypeStr\n" else ""
      val conditionLine = if (conditionStr.nonEmpty) s"✨ Holati: $conditionStr\n" else ""

      val confirmText = BotMessages.CONFIRM_ASK(lang)

      s"""$header
         |
         |$originalText
         |🏠 Turi: $typeStr
         |💰 Narx: $priceStr
         |📍 Shahar: $cityStr
         |🏠 Xonalar: $roomsStr
         |📞 Telefon: $phoneStr
         |$districtLine
         |$floorLine
         |$totalFloorsLine
         |$buildingTypeLine
         |$conditionLine
         |
         |$confirmText""".stripMargin
    }

    override def setupWebhook(): F[Unit] = {
      val fullWebhookUrl = s"$webhookBaseUrl/$botToken"

      for {
        _ <- Logger[F].info(s"Setting up webhook for URL: $webhookBaseUrl/***")
        response <- Methods.setWebhook(url = fullWebhookUrl).exec(api)
        _ <-
          if (response)
            Logger[F].info("Webhook setup successful")
          else
            Logger[F].error("Webhook setup failed")
      } yield ()
    }
  }
}
