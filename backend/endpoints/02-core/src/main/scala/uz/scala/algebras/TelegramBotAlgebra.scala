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
import telegramium.bots.InputMediaPhoto
import telegramium.bots.Message
import telegramium.bots.Update
import telegramium.bots.high.Api
import telegramium.bots.high.Methods
import telegramium.bots.high.implicits._

import uz.scala.Language
import uz.scala.domain.enums.BotState
import uz.scala.domain.enums.ListingStatus
import uz.scala.domain.listings.ListingFilters
import uz.scala.domain.listings.ListingOutput
import uz.scala.domain.telegram.SearchContext
import uz.scala.effects.Calendar
import uz.scala.repos.TelegramSessionsRepository
import uz.scala.repos.TelegramUsersRepository
import uz.scala.repos.dto
import uz.scala.shared.BotMessages
import uz.scala.shared.TelegramKeyboards
import uz.scala.syntax.all.circeSyntaxJsonDecoderOps
import uz.scala.syntax.refined._

trait TelegramBotAlgebra[F[_]] {
  def processUpdate(update: Update): F[Unit]
  def setupWebhook(): F[Unit]
}

object TelegramBotAlgebra {
  def make[F[_]: MonadCancelThrow: Calendar: Logger](
      api: Api[F],
      usersRepo: TelegramUsersRepository[doobie.ConnectionIO],
      sessionsRepo: TelegramSessionsRepository[doobie.ConnectionIO],
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
      listingsAlgebra,
      citiesAlgebra,
      botToken,
      webhookBaseUrl,
    )

  private class Impl[F[_]: MonadCancelThrow: Calendar: Logger](
      api: Api[F],
      usersRepo: TelegramUsersRepository[doobie.ConnectionIO],
      sessionsRepo: TelegramSessionsRepository[doobie.ConnectionIO],
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
              case Some(text) => handleStateBasedMessage(msg, user, session, text)
              case None => Logger[F].debug("Message without text, ignoring")
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
      val lang = Language.withName(user.languageCode)

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
                  text = BotMessages.ENTER_CUSTOM_CITY(lang),
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
                    text = s"${BotMessages.INVALID_PRICE(lang)}\n\n💰 Minimal price (USD):",
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
                languageCode = from.languageCode.getOrElse("en"),
                isRegistered = false,
                createdAt = now,
                lastInteractionAt = now,
              )
              implicit0(lang: Language) = Language.withName(newUser.languageCode)
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
      val lang = Language.withName(user.languageCode)
      val welcomeText = BotMessages.WELCOME_NEW(lang)

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

        lang = Language.withName(user.languageCode)

        // Get cities from database
        cities <- citiesAlgebra.getAll

        // Create dynamic keyboard from cities
        cityKeyboard = createDynamicCityKeyboard(cities, lang)

        // Send city selection with inline keyboard
        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.SELECT_CITY(lang),
            replyMarkup = Some(cityKeyboard),
          )
          .exec(api)
          .void
      } yield ()

    private def handleHelpCommand(msg: Message, user: dto.TelegramUser): F[Unit] = {
      val lang = Language.withName(user.languageCode)
      val helpText = BotMessages.WELCOME_NEW(lang)

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

        lang = Language.withName(user.languageCode)

        // Send price selection with inline keyboard
        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.SELECT_PRICE_RANGE(lang),
            replyMarkup = Some(TelegramKeyboards.priceRangeSelectionKeyboard(lang.toString)),
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
      else {
        val lang = Language.withName(user.languageCode)
        Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.ENTER_CUSTOM_CITY(lang),
          )
          .exec(api)
          .void
      }
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

        lang = Language.withName(user.languageCode)

        // Send room selection with inline keyboard
        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.SELECT_ROOMS(lang),
            replyMarkup = Some(TelegramKeyboards.roomsSelectionKeyboard(lang.toString)),
          )
          .exec(api)
          .void
      } yield ()

    private def handleCustomPriceMin(
        msg: Message,
        user: dto.TelegramUser,
        session: dto.TelegramSession,
        text: String,
      ): F[Unit] = {
      val lang = Language.withName(user.languageCode)

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
                text = s"💰 Maximum price (USD):",
              )
              .exec(api)
              .void
          } yield ()
        case None =>
          Methods
            .sendMessage(
              chatId = ChatIntId(msg.chat.id),
              text = BotMessages.INVALID_PRICE(lang),
            )
            .exec(api)
            .void
      }
    }

    private def handleCustomPriceMax(
        msg: Message,
        user: dto.TelegramUser,
        session: dto.TelegramSession,
        text: String,
      ): F[Unit] = {
      val lang = Language.withName(user.languageCode)

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
                      text = BotMessages.SELECT_ROOMS(lang),
                      replyMarkup = Some(TelegramKeyboards.roomsSelectionKeyboard(lang.toString)),
                    )
                    .exec(api)
                    .void
                } yield ()
              }
              else
                Methods
                  .sendMessage(
                    chatId = ChatIntId(msg.chat.id),
                    text = BotMessages.INVALID_PRICE_RANGE(lang),
                  )
                  .exec(api)
                  .void
          } yield ()
        case None =>
          Methods
            .sendMessage(
              chatId = ChatIntId(msg.chat.id),
              text = BotMessages.INVALID_PRICE(lang),
            )
            .exec(api)
            .void
      }
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
      ): F[Unit] = {
      val lang = Language.withName(user.languageCode)
      for {
        // Show searching message
        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.SEARCHING(lang),
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
                  text = BotMessages.searchResultsHeader(result.total, lang),
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
                  text = BotMessages.NO_RESULTS(lang),
                  replyMarkup = Some(
                    TelegramKeyboards.resultsNavigationKeyboard(hasMore = false, lang.toString)
                  ),
                )
                .exec(api)

              // Reset state
              _ <- sessionsRepo
                .updateState(user.telegramId, BotState.Idle, None)
                .transact(xa)
            } yield ()
      } yield ()
    }

    private def sendListing(
        chatId: Long,
        listing: ListingOutput,
        user: dto.TelegramUser,
        context: SearchContext,
      ): F[Unit] = {
      val lang = Language.withName(user.languageCode)

      val listingText = BotMessages.formatListing(
        title = listing.title.value,
        city = listing.city.value,
        price = listing.price.amount,
        rooms = 2, // Default value since not in model
        size = None, // Not in current model
        furnished = true, // Default value
        contact = listing.owner.firstName.value,
        lang = lang,
      )

      Methods
        .sendMessage(
          chatId = ChatIntId(chatId),
          text = listingText,
          replyMarkup = Some(TelegramKeyboards.listingActionButtons(lang.toString)),
        )
        .exec(api)
        .void
    }

    private def sendListingWithNavigation(
        chatId: Long,
        listing: ListingOutput,
        user: dto.TelegramUser,
        context: SearchContext,
        totalResults: Long,
      ): F[Unit] = {
      val lang = Language.withName(user.languageCode)

      val listingText = BotMessages.formatListing(
        title = listing.title.value,
        city = listing.city.value,
        price = listing.price.amount,
        rooms = 2, // Default value since not in model
        size = None, // Not in current model
        furnished = true, // Default value
        contact = listing.owner.firstName.value,
        lang = lang,
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
          actionKeyboard = TelegramKeyboards.listingActionButtons(lang.toString)
          navKeyboard = TelegramKeyboards.resultsNavigationKeyboard(hasMore, lang.toString)

          // Combine keyboards - action buttons first, then navigation
          combinedKeyboard = telegramium
            .bots
            .InlineKeyboardMarkup(
              actionKeyboard.inlineKeyboard ++ navKeyboard.inlineKeyboard
            )

          _ <- Methods
            .sendMessage(
              chatId = ChatIntId(chatId),
              text = "What would you like to do?",
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
            replyMarkup = Some(TelegramKeyboards.listingActionButtons(lang.toString)),
          )
          .exec(api)
          .void

      // Create combined keyboard with listing actions and navigation
      val hasMore = totalResults > context.page * 5 // 5 results per page
      val navKeyboard = TelegramKeyboards.resultsNavigationKeyboard(hasMore, lang.toString)

      photoEffect.flatTap(_ =>
        Methods
          .sendMessage(
            chatId = ChatIntId(chatId),
            text = "Need more options?",
            replyMarkup = Some(navKeyboard),
          )
          .exec(api)
      )
    }

    private def handleResultsMessage(msg: Message, user: dto.TelegramUser): F[Unit] = {
      val lang = Language.withName(user.languageCode)
      Methods
        .sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = "Use the buttons below to navigate through results.",
        )
        .exec(api)
        .void
    }

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

    private def handleContactAction(msg: Message, user: dto.TelegramUser): F[Unit] = {
      val lang = Language.withName(user.languageCode)
      Methods
        .sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text =
            "📞 Contact feature coming soon! Please call the number listed in the property details.",
        )
        .exec(api)
        .void
    }

    private def handleSaveAction(msg: Message, user: dto.TelegramUser): F[Unit] = {
      val lang = Language.withName(user.languageCode)
      Methods
        .sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = "❤️ Save feature coming soon! You'll be able to save your favorite properties.",
        )
        .exec(api)
        .void
    }

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
                else {
                  // No more listings on this page
                  val lang = Language.withName(user.languageCode)
                  Methods
                    .sendMessage(
                      chatId = ChatIntId(msg.chat.id),
                      text =
                        "This is the last listing on this page. Use 'Show more' to see more properties.",
                      replyMarkup = Some(
                        TelegramKeyboards.resultsNavigationKeyboard(
                          hasMore = result.total > searchContext.page * 5,
                          lang.toString,
                        )
                      ),
                    )
                    .exec(api)
                    .void
                }
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

      // Get "Other city" text based on language
      val otherCityText = language match {
        case Language.Uz => "Boshqa shahar"
        case Language.Ru => "Другой город"
        case _ => "Other city"
      }

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
