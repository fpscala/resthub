package uz.scala.algebras

import cats.effect.MonadCancel
import cats.effect.MonadCancelThrow
import cats.implicits._
import doobie.ConnectionIO
import doobie.implicits._
import io.circe.syntax._
import org.typelevel.log4cats.Logger
import telegramium.bots.ChatIntId
import telegramium.bots.Html
import telegramium.bots.InlineKeyboardButton
import telegramium.bots.InlineKeyboardMarkup
import telegramium.bots.InputLinkFile
import telegramium.bots.Message
import telegramium.bots.Update
import telegramium.bots.high.Api
import telegramium.bots.high.Methods
import telegramium.bots.high.implicits._

import uz.scala.Language
import uz.scala.domain.enums.BotMode
import uz.scala.domain.enums.BotState
import uz.scala.domain.enums.ListingStatus
import uz.scala.domain.enums.ListingType
import uz.scala.domain.listings.ListingFilters
import uz.scala.domain.listings.ListingOutput
import uz.scala.domain.telegram.AdminPostingContext
import uz.scala.domain.telegram.BotContext
import uz.scala.domain.telegram.BrokerFlowContext
import uz.scala.domain.telegram.BrokerStep
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
import uz.scala.syntax.option._
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
      authAlgebra: AuthAlgebra[F],
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
      authAlgebra,
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
      authAlgebra: AuthAlgebra[F],
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
          city match {
            // Broker posting - specific city buttons
            case "tashkent" => handleCityPostingSelection(msg, user, Some("Toshkent"))
            case "samarqand" => handleCityPostingSelection(msg, user, Some("Samarqand"))
            case "andijan" => handleCityPostingSelection(msg, user, Some("Andijon"))
            case "bukhara" => handleCityPostingSelection(msg, user, Some("Buxoro"))
            case "custom" =>
              for {
                _ <- updateStatePreservingContext(
                  user.telegramId,
                  BotState.BrokerAwaitingCity,
                  user.languageCode,
                )
                _ <- Methods
                  .sendMessage(
                    chatId = ChatIntId(msg.chat.id),
                    text = BotMessages.PROMPT_CITY_CUSTOM(user.languageCode),
                  )
                  .exec(api)
                  .void
              } yield ()
            // Buyer search - dynamic city names or "other"
            case "other" =>
              for {
                _ <- updateStatePreservingContext(
                  user.telegramId,
                  BotState.AwaitingCustomCity,
                  user.languageCode,
                )
                _ <- Methods
                  .sendMessage(
                    chatId = ChatIntId(msg.chat.id),
                    text = BotMessages.ENTER_CUSTOM_CITY(user.languageCode),
                  )
                  .exec(api)
                  .void
              } yield ()
            // Buyer search - pass through any other city name
            case _ => handleCitySelection(msg, user, city)
          }

        // Price range selection
        case priceData if priceData.startsWith("price_") =>
          priceData.stripPrefix("price_") match {
            // Buyer search - price ranges with underscore
            case "200_500" => handlePriceSelection(msg, user, Some(200), Some(500))
            case "500_800" => handlePriceSelection(msg, user, Some(500), Some(800))
            case "800_1200" => handlePriceSelection(msg, user, Some(800), Some(1200))
            case "1200_2000" => handlePriceSelection(msg, user, Some(1200), Some(2000))
            // Broker posting - single prices and custom/skip
            case "200" => handlePricePostingSelection(msg, user, Some(BigDecimal(200)))
            case "300" => handlePricePostingSelection(msg, user, Some(BigDecimal(300)))
            case "500" => handlePricePostingSelection(msg, user, Some(BigDecimal(500)))
            case "skip" => handlePricePostingSelection(msg, user, None)
            // Buyer custom price (for search)
            case "custom" =>
              for {
                _ <- updateStatePreservingContext(
                  user.telegramId,
                  BotState.AwaitingCustomPriceMin,
                  user.languageCode,
                )
                _ <- Methods
                  .sendMessage(
                    chatId = ChatIntId(msg.chat.id),
                    text =
                      s"${BotMessages.INVALID_PRICE(user.languageCode)}\n\n💰 Minimal price (USD):",
                  )
                  .exec(api)
                  .void
              } yield ()
            // Handle any other unknown price patterns
            case _ =>
              Logger[F].warn(s"Unknown price pattern: $priceData") *>
                Methods
                  .sendMessage(
                    chatId = ChatIntId(msg.chat.id),
                    text = BotMessages.INVALID_PRICE(user.languageCode),
                  )
                  .exec(api)
                  .void
          }

        // Room selection - MODE-SPECIFIC ROUTING
        case roomsData if roomsData.startsWith("rooms_") =>
          val rooms = roomsData.stripPrefix("rooms_") match {
            case "skip" => None
            case "4+" => Some(4)
            case num => num.toIntOption
          }
          // Route based on mode - BUYER searches, BROKER posts
          for {
            session <- sessionsRepo.findByTelegramId(user.telegramId).transact(xa)
            botContext <- session.flatMap(_.context).traverse(_.decodeAsF[F, BotContext])
            _ <- botContext match {
              case Some(context) if context.mode == BotMode.Broker =>
                // BROKER: Update broker posting context with rooms
                val roomValue = rooms match {
                  case None => "skip"
                  case Some(r) => r.toString
                }
                handleRoomsInput(msg, user, roomValue)
              case _ =>
                // BUYER or no context: Search for listings
                handleRoomSelection(msg, user, rooms)
            }
          } yield {}

        // Results navigation
        case "show_more" => handleShowMore(msg, user)
        case "new_search" => handleSearchCommand(msg, user)

        // Listing actions
        case "contact" => handleContactAction(msg, user)
        case "save" => handleSaveAction(msg, user)
        case "next" => handleNextListing(msg, user)

        // Main menu actions
        case "start_search" => handleSearchCommand(msg, user)
        case "show_help" => handleHelpCommand(msg, user)
        case "back_to_menu" => handleStartCommand(msg, user)

        // Mode selection actions
        case "select_buyer_mode" => handleSelectBuyerMode(msg, user)
        case "select_broker_mode" => handleSelectBrokerMode(msg, user)

        // Settings actions
        case "open_settings" => handleOpenSettings(msg, user)
        case "change_mode" => handleChangeMode(msg, user)
        case "confirm_change_mode" => handleConfirmChangeMode(msg, user)
        case "cancel_change_mode" => handleCancelChangeMode(msg, user)
        case "change_language" => handleChangeLanguage(msg, user)

        // Broker actions
        case "admin_post" => handleAdminPost(msg, user)
        case "manage_drafts" => handleManageDrafts(msg, user)
        case "post_to_channel" => handlePostToChannel(msg, user)

        // Listing type selection
        case "listing_type_apartment" => handleListingTypeSelection(msg, user, ListingType.ForRent)
        case "listing_type_house" => handleListingTypeSelection(msg, user, ListingType.ForSale)

        // ============================================================
        // BROKER POSTING - BUTTON-FIRST UX CALLBACKS
        // ============================================================

        // Phone selection
        case phoneData if phoneData.startsWith("phone_") =>
          phoneData.stripPrefix("phone_") match {
            case "share_contact" =>
              for {
                _ <- updateStatePreservingContext(
                  user.telegramId,
                  BotState.BrokerAwaitingPhone,
                  user.languageCode,
                )
                _ <- Methods
                  .sendMessage(
                    chatId = ChatIntId(msg.chat.id),
                    text = BotMessages.PROMPT_PHONE_BUTTON(user.languageCode),
                    replyMarkup = Some(
                      telegramium
                        .bots
                        .ReplyKeyboardMarkup(
                          keyboard = List(
                            List(
                              telegramium
                                .bots
                                .KeyboardButton(
                                  text = BotMessages.SHARE_CONTACT_BUTTON(user.languageCode),
                                  requestContact = Some(true),
                                )
                            )
                          ),
                          oneTimeKeyboard = Some(true),
                          resizeKeyboard = Some(true),
                        )
                    ),
                  )
                  .exec(api)
                  .void
              } yield ()
            case "manual" =>
              for {
                _ <- updateStatePreservingContext(
                  user.telegramId,
                  BotState.BrokerAwaitingPhone,
                  user.languageCode,
                )
                _ <- Methods
                  .sendMessage(
                    chatId = ChatIntId(msg.chat.id),
                    text = BotMessages.PROMPT_PHONE_MANUAL(user.languageCode),
                  )
                  .exec(api)
                  .void
              } yield ()
            case "skip" => handlePhonePostingSelection(msg, user, None)
          }

        // District selection
        case districtData if districtData.startsWith("district_") =>
          districtData.stripPrefix("district_") match {
            case "chilonzor" => handleDistrictPostingSelection(msg, user, Some("Chilonzor"))
            case "sergeli" => handleDistrictPostingSelection(msg, user, Some("Sergeli"))
            case "yunusobod" => handleDistrictPostingSelection(msg, user, Some("Yunusobod"))
            case "custom" =>
              for {
                _ <- updateStatePreservingContext(
                  user.telegramId,
                  BotState.BrokerAwaitingDistrict,
                  user.languageCode,
                )
                _ <- Methods
                  .sendMessage(
                    chatId = ChatIntId(msg.chat.id),
                    text = BotMessages.PROMPT_DISTRICT_CUSTOM(user.languageCode),
                  )
                  .exec(api)
                  .void
              } yield ()
            case "skip" => handleDistrictPostingSelection(msg, user, None)
          }

        // Floor selection
        case floorData if floorData.startsWith("floor_") =>
          floorData.stripPrefix("floor_") match {
            case "1" => handleFloorPostingSelection(msg, user, Some(1))
            case "2" => handleFloorPostingSelection(msg, user, Some(2))
            case "3" => handleFloorPostingSelection(msg, user, Some(3))
            case "4+" => handleFloorPostingSelection(msg, user, Some(4))
            case "skip" => handleFloorPostingSelection(msg, user, None)
          }

        // Total floors selection
        case totalFloorsData if totalFloorsData.startsWith("total_floors_") =>
          totalFloorsData.stripPrefix("total_floors_") match {
            case "3" => handleTotalFloorsPostingSelection(msg, user, Some(3))
            case "5" => handleTotalFloorsPostingSelection(msg, user, Some(5))
            case "9" => handleTotalFloorsPostingSelection(msg, user, Some(9))
            case "skip" => handleTotalFloorsPostingSelection(msg, user, None)
          }

        // Building type selection
        case buildingData if buildingData.startsWith("building_") =>
          buildingData.stripPrefix("building_") match {
            case "apartment" => handleBuildingTypePostingSelection(msg, user, Some("Kvartira"))
            case "house" => handleBuildingTypePostingSelection(msg, user, Some("Hovli"))
            case "office" => handleBuildingTypePostingSelection(msg, user, Some("Ofis"))
            case "skip" => handleBuildingTypePostingSelection(msg, user, None)
          }

        // Condition selection
        case conditionData if conditionData.startsWith("condition_") =>
          conditionData.stripPrefix("condition_") match {
            case "good" => handleConditionPostingSelection(msg, user, Some("Yaxshi"))
            case "excellent" => handleConditionPostingSelection(msg, user, Some("Zo'r"))
            case "needs_repair" => handleConditionPostingSelection(msg, user, Some("Ta'mir talab"))
            case "skip" => handleConditionPostingSelection(msg, user, None)
          }

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
                phoneNumber = None, // Phone number not available during initial message
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

    private def handleStartCommand(msg: Message, user: dto.TelegramUser): F[Unit] =
      for {
        // STEP 1: ENSURE USER EXISTS IN BOTH TABLES (MANDATORY)
        _ <- ensureUserExists(user)

        _ <- proceedWithBotLogic(msg, user)
      } yield ()

    // MANDATORY: ENSURE USER EXISTS IN BOTH TABLES
    private def ensureUserExists(telegramUser: dto.TelegramUser): F[Unit] =
      for {
        // STEP 1: UPSERT TELEGRAM_USER RECORD
        existingTelegramUser <- usersRepo.findByTelegramId(telegramUser.telegramId).transact(xa)
        now <- Calendar[F].currentZonedDateTime

        updatedTelegramUser <- existingTelegramUser match {
          case Some(tgUser) =>
            // UPDATE: Update last_interaction_at only
            for {
              _ <- Logger[F].info(s"telegram_user_found: ${tgUser.telegramId}")
              _ <- usersRepo.updateLastInteraction(tgUser.telegramId).transact(xa)
            } yield tgUser
          case None =>
            // INSERT: Create new telegram_users record
            for {
              _ <- Logger[F].info(s"telegram_user_created: ${telegramUser.telegramId}")
              newUser <- newTelegramUser(telegramUser, now)
            } yield newUser
        }

        // STEP 2: CHECK IF USERS TABLE RECORD IS NEEDED
        _ <- createAndLinkUser(updatedTelegramUser, now).whenA(
          updatedTelegramUser.userId.isEmpty
        )

      } yield {}

    // CREATE NEW TELEGRAM USER RECORD
    private def newTelegramUser(
        telegramUser: dto.TelegramUser,
        now: java.time.ZonedDateTime,
      ): F[dto.TelegramUser] = {
      val newTelegramUser = dto.TelegramUser(
        telegramId = telegramUser.telegramId,
        userId = None, // Will be set after creating users record
        username = telegramUser.username,
        firstName = telegramUser.firstName,
        languageCode = telegramUser.languageCode,
        phoneNumber = telegramUser.phoneNumber,
        isRegistered = false, // Will be true after users table link
        createdAt = now,
        lastInteractionAt = now,
      )

      // Insert into database and return the user
      usersRepo.create(newTelegramUser)(telegramUser.languageCode).transact(xa).as(newTelegramUser)
    }

    // CREATE USER IN USERS TABLE AND LINK TO TELEGRAM_USER
    private def createAndLinkUser(
        telegramUser: dto.TelegramUser,
        now: java.time.ZonedDateTime,
      ): F[Unit] =
      for {
        // Use AuthAlgebra to create user
        userId <- authAlgebra.createUserFromTelegram(telegramUser)(telegramUser.languageCode)

        // Link telegram_user to users table
        _ <- usersRepo.updateUserId(telegramUser.telegramId, userId).transact(xa)

        // Mark telegram_user as registered
        _ <- usersRepo
          .create(
            telegramUser.copy(
              userId = Some(userId),
              isRegistered = true,
              lastInteractionAt = now,
            )
          )(telegramUser.languageCode)
          .transact(xa)

        _ <- Logger[F].info(
          s"users_created: telegram_id=${telegramUser.telegramId} -> user_id=$userId"
        )

      } yield {}

    private def proceedWithBotLogic(msg: Message, user: dto.TelegramUser): F[Unit] =
      for {
        // Check if user already has a mode selected
        session <- sessionsRepo.findByTelegramId(user.telegramId).transact(xa)
        botContext <- session.flatMap(_.context).traverse(_.decodeAsF[F, BotContext])

        _ <- botContext match {
          case Some(context) =>
            // User already has a mode, show appropriate home screen
            val welcomeText = context.mode match {
              case BotMode.Buyer => BotMessages.BUYER_HOME_WELCOME(user.languageCode)
              case BotMode.Broker => BotMessages.BROKER_HOME_WELCOME(user.languageCode)
            }
            val keyboard = context.mode match {
              case BotMode.Buyer => TelegramKeyboards.buyerHomeKeyboard(user.languageCode)
              case BotMode.Broker => TelegramKeyboards.brokerHomeKeyboard(user.languageCode)
            }

            Methods
              .sendMessage(
                chatId = ChatIntId(msg.chat.id),
                text = welcomeText,
                replyMarkup = Some(keyboard),
              )
              .exec(api)
              .void
          case None =>
            // No mode selected, show mode selection
            Methods
              .sendMessage(
                chatId = ChatIntId(msg.chat.id),
                text = BotMessages.MODE_SELECTION_PROMPT(user.languageCode),
                replyMarkup = Some(TelegramKeyboards.modeSelectionKeyboard(user.languageCode)),
              )
              .exec(api)
              .void
        }
      } yield ()

    // Helper method to get mode-specific keyboard
    private def getModeSpecificKeyboard(mode: BotMode, language: Language): InlineKeyboardMarkup =
      mode match {
        case BotMode.Buyer => TelegramKeyboards.buyerModeKeyboard(language)
        case BotMode.Broker => TelegramKeyboards.brokerModeKeyboard(language)
      }

    // Helper method to save mode to session context
    private def saveModeToSession(telegramId: Long, mode: BotMode): F[Unit] = {
      val context = BotContext(mode = mode)
      sessionsRepo
        .updateState(telegramId, BotState.Idle, Some(context.asJson))
        .transact(xa)
        .void
    }

    private def handleSelectBuyerMode(msg: Message, user: dto.TelegramUser): F[Unit] =
      for {
        _ <- saveModeToSession(user.telegramId, BotMode.Buyer)
        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.MODE_CHANGED_SUCCESSFULLY(user.languageCode),
            replyMarkup = Some(TelegramKeyboards.buyerHomeKeyboard(user.languageCode)),
          )
          .exec(api)
          .void
      } yield ()

    private def handleSelectBrokerMode(msg: Message, user: dto.TelegramUser): F[Unit] =
      for {
        _ <- saveModeToSession(user.telegramId, BotMode.Broker)
        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.MODE_CHANGED_SUCCESSFULLY(user.languageCode),
            replyMarkup = Some(TelegramKeyboards.brokerHomeKeyboard(user.languageCode)),
          )
          .exec(api)
          .void
      } yield ()

    private def handleOpenSettings(msg: Message, user: dto.TelegramUser): F[Unit] =
      Methods
        .sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = BotMessages.SETTINGS_MENU(user.languageCode),
          replyMarkup = Some(TelegramKeyboards.settingsKeyboard(user.languageCode)),
        )
        .exec(api)
        .void

    private def handleChangeMode(msg: Message, user: dto.TelegramUser): F[Unit] =
      Methods
        .sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = BotMessages.MODE_CHANGE_CONFIRM(user.languageCode),
          replyMarkup = Some(TelegramKeyboards.modeChangeConfirmationKeyboard(user.languageCode)),
        )
        .exec(api)
        .void

    private def handleChangeLanguage(msg: Message, user: dto.TelegramUser): F[Unit] =
      // Language change disabled - show message
      Methods
        .sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = "Language change is currently disabled.",
        )
        .exec(api)
        .void

    private def handleConfirmChangeMode(msg: Message, user: dto.TelegramUser): F[Unit] =
      for {
        // Clear the session context (except telegram_id and reset state)
        now <- Calendar[F].currentZonedDateTime
        _ <- sessionsRepo
          .upsert(
            dto.TelegramSession(
              telegramId = user.telegramId,
              state = BotState.Idle,
              context = None, // Clear mode completely
              updatedAt = now,
            )
          )(user.languageCode)
          .transact(xa)

        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.MODE_CHANGED_SUCCESSFULLY(user.languageCode),
          )
          .exec(api)
          .void

        // Show mode selection again
        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.MODE_SELECTION_PROMPT(user.languageCode),
            replyMarkup = Some(TelegramKeyboards.modeSelectionKeyboard(user.languageCode)),
          )
          .exec(api)
          .void
      } yield ()

    private def handleCancelChangeMode(msg: Message, user: dto.TelegramUser): F[Unit] =
      for {
        // Get current session to show appropriate home screen
        session <- sessionsRepo.findByTelegramId(user.telegramId).transact(xa)
        botContext <- session.flatMap(_.context).traverse(_.decodeAsF[F, BotContext])

        _ <- botContext match {
          case Some(context) =>
            // Show current mode home screen
            val welcomeText = context.mode match {
              case BotMode.Buyer => BotMessages.BUYER_HOME_WELCOME(user.languageCode)
              case BotMode.Broker => BotMessages.BROKER_HOME_WELCOME(user.languageCode)
            }
            val keyboard = context.mode match {
              case BotMode.Buyer => TelegramKeyboards.buyerHomeKeyboard(user.languageCode)
              case BotMode.Broker => TelegramKeyboards.brokerHomeKeyboard(user.languageCode)
            }

            Methods
              .sendMessage(
                chatId = ChatIntId(msg.chat.id),
                text = welcomeText,
                replyMarkup = Some(keyboard),
              )
              .exec(api)
              .void
          case None =>
            // No mode, show mode selection
            Methods
              .sendMessage(
                chatId = ChatIntId(msg.chat.id),
                text = BotMessages.MODE_SELECTION_PROMPT(user.languageCode),
                replyMarkup = Some(TelegramKeyboards.modeSelectionKeyboard(user.languageCode)),
              )
              .exec(api)
              .void
        }
      } yield ()

    private def handleHelpCommand(msg: Message, user: dto.TelegramUser): F[Unit] =
      for {
        session <- sessionsRepo.findByTelegramId(user.telegramId).transact(xa)

        botContext <- session
          .flatMap(_.context)
          .traverse(_.decodeAsF[F, BotContext])

        helpText = botContext match {
          case Some(context) =>
            context.mode match {
              case BotMode.Buyer =>
                s"""${BotMessages.HELP_HEADER(user.languageCode)}
                   |
                   |${BotMessages.HELP_SEARCH_SECTION(user.languageCode)}
                   |${BotMessages.HELP_SEARCH_STEPS(user.languageCode)}
                   |
                   |${BotMessages.HELP_COMMANDS_SECTION(user.languageCode)}
                   |${BotMessages.HELP_COMMANDS_LIST(user.languageCode)}
                   |
                   |${BotMessages.HELP_CONTACT(user.languageCode)}""".stripMargin
              case BotMode.Broker =>
                s"""${BotMessages.HELP_HEADER(user.languageCode)}
                   |
                   |🏢 **Broker Mode Help**
                   |
                   |${BotMessages.HELP_ADMIN_SECTION(user.languageCode)}
                   |${BotMessages.HELP_ADMIN_STEPS(user.languageCode)}
                   |
                   |${BotMessages.HELP_COMMANDS_SECTION(user.languageCode)}
                   |/postadmin - Create new listing
                   |/start - Show broker home
                   |/help - Show this help
                   |
                   |${BotMessages.HELP_CONTACT(user.languageCode)}""".stripMargin
            }
          case None =>
            s"""${BotMessages.HELP_HEADER(user.languageCode)}
               |
               |Please select a mode first to see relevant help.
               |
               |${BotMessages.HELP_CONTACT(user.languageCode)}""".stripMargin
        }

        keyboard = botContext match {
          case Some(context) =>
            context.mode match {
              case BotMode.Buyer => TelegramKeyboards.buyerHomeKeyboard(user.languageCode)
              case BotMode.Broker => TelegramKeyboards.brokerHomeKeyboard(user.languageCode)
            }
          case None =>
            TelegramKeyboards.mainMenuKeyboard(user.languageCode)
        }

        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = helpText,
            replyMarkup = Some(keyboard),
          )
          .exec(api)
          .void
      } yield ()

    private def handleSearchCommand(msg: Message, user: dto.TelegramUser): F[Unit] =
      checkUserMode(
        user.telegramId,
        BotMode.Buyer,
        for {
          // Update session state to AwaitingCity while preserving mode
          _ <- updateStatePreservingContext(
            user.telegramId,
            BotState.AwaitingCity,
            user.languageCode,
          )

          // Get cities from database
          cities <- citiesAlgebra.getAll

          // Create dynamic keyboard from cities with quick actions
          cityKeyboard = createDynamicCityKeyboard(cities, user.languageCode)
          quickKeyboard = TelegramKeyboards.quickActionsKeyboard(user.languageCode)

          // Send city selection with inline keyboard
          _ <- Methods
            .sendMessage(
              chatId = ChatIntId(msg.chat.id),
              text = BotMessages.SELECT_CITY(user.languageCode),
              replyMarkup = Some(
                InlineKeyboardMarkup(
                  cityKeyboard.inlineKeyboard ++ quickKeyboard.inlineKeyboard
                )
              ),
            )
            .exec(api)
            .void
        } yield (),
      )

    private def handleStateBasedMessage(
        msg: Message,
        user: dto.TelegramUser,
        session: dto.TelegramSession,
        text: String,
      ): F[Unit] = {
      // First, get the mode to determine which states are allowed
      val botContext = session.context.traverse(_.decodeAsF[F, BotContext])

      botContext.flatMap {
        case Some(context) =>
          context.mode match {
            case BotMode.Buyer =>
              // Buyer can use all search-related states
              session.state match {
                case BotState.Idle => Logger[F].debug("User is idle, ignoring message")
                case BotState.AwaitingCity => handleCityInput(msg, user, text)
                case BotState.AwaitingCustomCity => handleCustomCityInput(msg, user, text)
                case BotState.AwaitingPriceRange =>
                  Logger[F].debug("Should use inline keyboard for price selection")
                case BotState.AwaitingCustomPriceMin =>
                  handleCustomPriceMin(msg, user, session, text)
                case BotState.AwaitingCustomPriceMax =>
                  handleCustomPriceMax(msg, user, session, text)
                case BotState.ViewingResults => handleResultsMessage(msg, user)
                case _ =>
                  Logger[F].warn("Buyer in admin posting state, ignoring")
              }
            case BotMode.Broker =>
              // Broker uses BrokerAwaiting* states only
              session.state match {
                case BotState.Idle =>
                  Logger[F].debug("Broker is idle, ignoring message")
                case BotState.BrokerAwaitingListingType => handleListingTypeInput(msg, user, text)
                case BotState.BrokerAwaitingPrice => handlePriceInput(msg, user, text)
                case BotState.BrokerAwaitingCity => handleCityForPostingInput(msg, user, text)
                case BotState.BrokerAwaitingRooms => handleRoomsInput(msg, user, text)
                case BotState.BrokerAwaitingPhone => handlePhoneInput(msg, user, text)
                case BotState.BrokerAwaitingDistrict => handleDistrictInput(msg, user, text)
                case BotState.BrokerAwaitingFloor => handleFloorInput(msg, user, text)
                case BotState.BrokerAwaitingTotalFloors => handleTotalFloorsInput(msg, user, text)
                case BotState.BrokerAwaitingBuildingType => handleBuildingTypeInput(msg, user, text)
                case BotState.BrokerAwaitingCondition => handleConditionInput(msg, user, text)
                case BotState.BrokerAwaitingChannelSelection =>
                  handleChannelSelectionInput(msg, user, text)
                case BotState.BrokerAwaitingConfirmation =>
                  handleConfirmationInput(msg, user, text)
                case state =>
                  Logger[F].warn(s"Broker in invalid state: $state, ignoring")
              }
          }
        case None =>
          Logger[F].warn("No bot context found, ignoring message")
      }
    }

    private def handleCitySelection(
        msg: Message,
        user: dto.TelegramUser,
        city: String,
      ): F[Unit] =
      for {
        _ <- sessionsRepo
          .updateState(user.telegramId) { session =>
            session
              .context
              .fold(BotContext(BotMode.Buyer).pure[ConnectionIO])(
                _.decodeAsF[ConnectionIO, BotContext]
              )
              .map { context =>
                val updatedContext = context.copy(
                  search = SearchContext(city = city.some).some
                )
                session.copy(
                  state = BotState.AwaitingPriceRange,
                  context = Some(updatedContext.asJson),
                )
              }
          }
          .transact(xa)

        // Send price selection with inline keyboard
        // Create combined keyboard with price ranges and quick actions
        priceKeyboard = TelegramKeyboards.priceRangeSelectionKeyboard(user.languageCode)
        quickKeyboard = TelegramKeyboards.quickActionsKeyboard(user.languageCode)

        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.SELECT_PRICE_RANGE(user.languageCode),
            replyMarkup = Some(
              InlineKeyboardMarkup(
                priceKeyboard.inlineKeyboard ++ quickKeyboard.inlineKeyboard
              )
            ),
          )
          .exec(api)
          .void
      } yield ()

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
        // Get existing BotContext
        _ <- sessionsRepo
          .updateState(user.telegramId) { session =>
            session
              .context
              .fold(BotContext(BotMode.Buyer).pure[ConnectionIO])(
                _.decodeAsF[ConnectionIO, BotContext]
              )
              .map { context =>
                val searchContext = context.search.getOrElse(SearchContext())

                val updatedSearchContext = searchContext.copy(
                  minPrice = minPrice.map(BigDecimal(_)),
                  maxPrice = maxPrice.map(BigDecimal(_)),
                )
                val updatedContext = context.copy(
                  search = updatedSearchContext.some
                )
                session.copy(
                  state = BotState.BrokerAwaitingRooms,
                  context = Some(updatedContext.asJson),
                )
              }
          }
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
              .fold(SearchContext().pure[F])(
                _.decodeAsF[F, SearchContext]
              )

            updatedSearchContext = searchContext.copy(minPrice = Some(BigDecimal(minPrice)))

            _ <- sessionsRepo
              .updateState(user.telegramId) { s =>
                s.context
                  .fold(BotContext(BotMode.Buyer).pure[ConnectionIO])(
                    _.decodeAsF[ConnectionIO, BotContext]
                  )
                  .map { ctx =>
                    ctx.copy(search = updatedSearchContext.some)
                  }
                  .map { updatedContext =>
                    s.copy(
                      state = BotState.AwaitingCustomPriceMax,
                      context = Some(updatedContext.asJson),
                    )
                  }
              }
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
              .fold(SearchContext().pure[F])(
                _.decodeAsF[F, SearchContext]
              )

            // Validate min < max
            minValid = searchContext.minPrice.forall(_ < maxPrice)

            _ <-
              if (minValid) {
                val updatedSearchContext = searchContext.copy(maxPrice = Some(BigDecimal(maxPrice)))
                for {
                  _ <- sessionsRepo
                    .updateState(user.telegramId) { s =>
                      s.context
                        .fold(BotContext(BotMode.Buyer).pure[ConnectionIO])(
                          _.decodeAsF[ConnectionIO, BotContext]
                        )
                        .map { ctx =>
                          ctx.copy(search = updatedSearchContext.some)
                        }
                        .map { updatedContext =>
                          s.copy(
                            state = BotState.AwaitingPriceRange,
                            context = Some(updatedContext.asJson),
                          )
                        }
                    }
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

        botContext <- session
          .flatMap(_.context)
          .traverse(_.decodeAsF[F, BotContext])
          .map(_.getOrElse(BotContext(mode = BotMode.Buyer)))

        _ <- botContext.search match {
          case Some(searchContext) =>
            val updatedSearchContext = searchContext.copy(
              rooms = rooms,
              page = 1,
            )

            sessionsRepo
              .updateState(user.telegramId) { s =>
                s.context
                  .fold(BotContext(BotMode.Buyer).pure[ConnectionIO])(
                    _.decodeAsF[ConnectionIO, BotContext]
                  )
                  .map { ctx =>
                    ctx.copy(search = Some(updatedSearchContext))
                  }
                  .map { updatedContext =>
                    s.copy(
                      state = BotState.ViewingResults,
                      context = Some(updatedContext.asJson),
                    )
                  }
              }
              .transact(xa)
              .flatTap(_ => performSearch(msg, user, updatedSearchContext))
          case None =>
            Methods
              .sendMessage(
                chatId = ChatIntId(msg.chat.id),
                text = BotMessages.searchContextRequired(user.languageCode),
                replyMarkup = Some(
                  botContext.mode match {
                    case BotMode.Buyer => TelegramKeyboards.buyerHomeKeyboard(user.languageCode)
                    case BotMode.Broker => TelegramKeyboards.brokerHomeKeyboard(user.languageCode)
                  }
                ),
              )
              .exec(api)
              .void
        }
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

              // Reset state - preserve mode
              _ <- updateStatePreservingContext(user.telegramId, BotState.Idle, user.languageCode)
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

        botContext <- session
          .flatMap(_.context)
          .traverse(_.decodeAsF[F, BotContext])
          .map(_.getOrElse(BotContext(mode = BotMode.Buyer)))

        _ <- botContext.search match {
          case Some(searchContext) =>
            val newSearchContext = searchContext.copy(page = searchContext.page + 1)

            for {
              _ <- Logger[F].info(s"Showing more results, page: ${newSearchContext.page}")
              _ <- sessionsRepo
                .updateState(user.telegramId) { s =>
                  s.context
                    .fold(BotContext(BotMode.Buyer).pure[ConnectionIO])(
                      _.decodeAsF[ConnectionIO, BotContext]
                    )
                    .map { ctx =>
                      ctx.copy(search = Some(newSearchContext))
                    }
                    .map { updatedContext =>
                      s.copy(
                        state = BotState.ViewingResults,
                        context = Some(updatedContext.asJson),
                      )
                    }
                }
                .transact(xa)
                .flatTap(_ => performSearch(msg, user, newSearchContext))
            } yield ()
          case None =>
            Logger[F].warn("No search context found for show more") *>
              Methods
                .sendMessage(
                  chatId = ChatIntId(msg.chat.id),
                  text = BotMessages.searchContextRequired(user.languageCode),
                  replyMarkup = Some(
                    botContext.mode match {
                      case BotMode.Buyer => TelegramKeyboards.buyerHomeKeyboard(user.languageCode)
                      case BotMode.Broker => TelegramKeyboards.brokerHomeKeyboard(user.languageCode)
                    }
                  ),
                )
                .exec(api)
                .void
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
        botContext <- session
          .flatMap(_.context)
          .traverse(_.decodeAsF[F, BotContext])
          .map(_.getOrElse(BotContext(mode = BotMode.Buyer)))

        _ <- botContext.search match {
          case Some(searchContext) =>
            for {
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
            Logger[F].warn("No search context found for next listing") *>
              Methods
                .sendMessage(
                  chatId = ChatIntId(msg.chat.id),
                  text = BotMessages.searchContextRequired(user.languageCode),
                  replyMarkup = Some(
                    botContext.mode match {
                      case BotMode.Buyer => TelegramKeyboards.buyerHomeKeyboard(user.languageCode)
                      case BotMode.Broker => TelegramKeyboards.brokerHomeKeyboard(user.languageCode)
                    }
                  ),
                )
                .exec(api)
                .void
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
      checkUserMode(
        user.telegramId,
        BotMode.Broker,
        for {
          _ <- sessionsRepo
            .updateState(user.telegramId) { session =>
              Calendar[ConnectionIO].currentZonedDateTime.map { now =>
                session.copy(
                  state = BotState.BrokerAwaitingListingType,
                  updatedAt = now,
                )
              }
            }
            .transact(xa)
          _ <- Methods
            .sendMessage(
              chatId = ChatIntId(msg.chat.id),
              text = BotMessages.SELECT_LISTING_TYPE(user.languageCode),
              replyMarkup = Some(TelegramKeyboards.listingTypeKeyboard(user.languageCode)),
            )
            .exec(api)
            .void
        } yield (),
      )

    private def handleForwardedMessage(
        msg: Message,
        user: dto.TelegramUser,
        session: dto.TelegramSession,
      ): F[Unit] =
      checkUserMode(
        user.telegramId,
        BotMode.Broker,
        // Check if this is a forwarded message
        msg.senderChat match {
          case Some(chat) =>
            // This is a forwarded message from a channel
            val adminContext = AdminPostingContext(
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
            )

            val confirmText = BotMessages.ADMIN_POST_CONFIRM(user.languageCode)

            for {
              now <- Calendar[F].currentZonedDateTime
              botContext = BotContext(
                mode = BotMode.Broker,
                broker = Some(BrokerFlowContext(adminContext, BrokerStep.Confirmation)),
              )
              _ <- sessionsRepo
                .updateState(user.telegramId) { s =>
                  s.copy(
                    state = BotState.BrokerAwaitingConfirmation,
                    context = Some(botContext.asJson),
                    updatedAt = now,
                  ).pure[ConnectionIO]
                }
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
              case BotState.BrokerAwaitingConfirmation =>
                Logger[F].debug(
                  "User is in broker confirmation flow but received non-forwarded message"
                )
              case _ =>
                Logger[F].debug(s"Ignoring non-forwarded message from user ${user.telegramId}")
            }
        },
      )

    private def handleListingTypeInput(
        msg: Message,
        user: dto.TelegramUser,
        text: String,
      ): F[Unit] =
      checkUserMode(
        user.telegramId,
        BotMode.Broker, {

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
                _.copy(listingType = Some(lt)),
                BotState.BrokerAwaitingPrice,
                user.languageCode,
              )
            case None =>
              val errorText = BotMessages.ERROR_INVALID_LISTING_TYPE(user.languageCode)
              Methods
                .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
                .exec(api)
                .void
          }
        },
      )

    private def handlePriceInput(
        msg: Message,
        user: dto.TelegramUser,
        text: String,
      ): F[Unit] =
      checkUserMode(
        user.telegramId,
        BotMode.Broker,
        text.replaceAll("[^0-9.]", "").toDoubleOption match {
          case Some(price) =>
            updateAdminContextAndNextStep(
              user.telegramId,
              msg.chat.id,
              _.copy(price = Some(BigDecimal(price))),
              BotState.BrokerAwaitingCity,
              user.languageCode,
            )
          case None =>
            val errorText = BotMessages.ERROR_INVALID_PRICE(user.languageCode)
            Methods
              .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
              .exec(api)
              .void
        },
      )

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
          _.copy(city = Some(city)),
          BotState.BrokerAwaitingRooms,
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
          _.copy(rooms = None),
          BotState.BrokerAwaitingPhone,
          user.languageCode,
        )
      else
        normalized.toIntOption match {
          case Some(rooms) if rooms > 0 =>
            updateAdminContextAndNextStep(
              user.telegramId,
              msg.chat.id,
              _.copy(rooms = Some(rooms)),
              BotState.BrokerAwaitingPhone,
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
          _.copy(phone = Some(phone)),
          BotState.BrokerAwaitingDistrict,
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
          _.copy(district = None),
          BotState.BrokerAwaitingFloor,
          user.languageCode,
        )
      else {
        val district = text.trim
        if (district.nonEmpty)
          updateAdminContextAndNextStep(
            user.telegramId,
            msg.chat.id,
            _.copy(district = Some(district)),
            BotState.BrokerAwaitingFloor,
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
          _.copy(floor = None),
          BotState.BrokerAwaitingTotalFloors,
          user.languageCode,
        )
      else
        normalized.toIntOption match {
          case Some(floor) if floor > 0 =>
            updateAdminContextAndNextStep(
              user.telegramId,
              msg.chat.id,
              _.copy(floor = Some(floor)),
              BotState.BrokerAwaitingTotalFloors,
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
          _.copy(totalFloors = None),
          BotState.BrokerAwaitingBuildingType,
          user.languageCode,
        )
      else
        normalized.toIntOption match {
          case Some(totalFloors) if totalFloors > 0 =>
            updateAdminContextAndNextStep(
              user.telegramId,
              msg.chat.id,
              _.copy(totalFloors = Some(totalFloors)),
              BotState.BrokerAwaitingBuildingType,
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
          _.copy(buildingType = None),
          BotState.BrokerAwaitingCondition,
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
              _.copy(buildingType = Some(bt)),
              BotState.BrokerAwaitingCondition,
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
              _.copy(condition = Some(c)),
              BotState.BrokerAwaitingConfirmation,
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

      if (normalized == "ha" || normalized == "yes" || normalized == "да")
        // REAL POSTING LOGIC - NO FALSE SUCCESS
        for {
          session <- sessionsRepo.findByTelegramId(user.telegramId).transact(xa)
          botContext <- session.flatMap(_.context).traverse(_.decodeAsF[F, BotContext])

          result <- botContext match {
            case Some(context) =>
              context.broker match {
                case Some(brokerFlow) =>
                  brokerFlow.draft.selectedChannelId match {
                    case Some(channelId) =>
                      checkAndCreateListing(
                        telegramId = user.telegramId,
                        userId = user
                          .userId
                          .getOrElse(
                            throw new RuntimeException("No user ID found for telegram user")
                          ),
                        context = brokerFlow.draft,
                        chatId = msg.chat.id,
                        lang = user.languageCode,
                      )
                    case None =>
                      val errorText = "❌ Kanal tanlanmagan. Iltimos, qaytadan urinib ko'ring."
                      Methods
                        .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
                        .exec(api)
                        .void
                  }
                case None =>
                  val errorText = "❌ E'lon ma'lumotlari topilmadi. Iltimos, qaytadan boshlang."
                  Methods
                    .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
                    .exec(api)
                    .void
              }
            case None =>
              val errorText = "❌ Kontekst topilmadi. Iltimos, qaytadan boshlang."
              Methods
                .sendMessage(chatId = ChatIntId(msg.chat.id), text = errorText)
                .exec(api)
                .void
          }

          // Reset state after processing
          _ <- updateStatePreservingContext(user.telegramId, BotState.Idle, user.languageCode)
        } yield result
      else if (normalized == "yo'q" || normalized == "no" || normalized == "нет")
        // Cancel posting - preserve mode
        updateStatePreservingContext(user.telegramId, BotState.Idle, user.languageCode) *>
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
            BotState.BrokerAwaitingListingType,
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
        _ <- sessionsRepo
          .updateState(telegramId) { session =>
            for {
              now <- Calendar[ConnectionIO].currentZonedDateTime
              updated <- session
                .context
                .fold(BotContext(BotMode.Broker).pure[ConnectionIO])(
                  _.decodeAsF[ConnectionIO, BotContext]
                )
                .map { context =>
                  val brokerFlow = context
                    .broker
                    .getOrElse(
                      BrokerFlowContext(
                        draft = AdminPostingContext(
                          forwardedMessage = ForwardedMessage(None, List.empty, None)
                        ),
                        currentStep = BrokerStep.Start,
                      )
                    )
                  val updatedDraft = updateFn(brokerFlow.draft)
                  session.copy(
                    state = nextState,
                    context = Some(
                      context.copy(broker = Some(brokerFlow.copy(draft = updatedDraft))).asJson
                    ),
                    updatedAt = now,
                  )
                }
            } yield updated
          }
          .transact(xa)

        (promptText, keyboard) = getNextStepPromptAndKeyboard(nextState, lang)
        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(chatId),
            text = promptText,
            replyMarkup = keyboard,
          )
          .exec(api)
          .void
      } yield ()

    private def getNextStepPromptAndKeyboard(
        state: BotState,
        lang: Language,
      ): (String, Option[InlineKeyboardMarkup]) =
      state match {
        case BotState.BrokerAwaitingPrice =>
          BotMessages.PROMPT_PRICE_BUTTON(lang) -> Some(TelegramKeyboards.priceKeyboard(lang))
        case BotState.BrokerAwaitingCity =>
          BotMessages.PROMPT_CITY_BUTTON(lang) -> Some(TelegramKeyboards.cityKeyboard(lang))
        case BotState.BrokerAwaitingRooms =>
          BotMessages.PROMPT_ROOMS(lang) -> Some(TelegramKeyboards.roomsSelectionKeyboard(lang))
        case BotState.BrokerAwaitingPhone =>
          BotMessages.PROMPT_PHONE_BUTTON(lang) -> Some(TelegramKeyboards.phoneKeyboard(lang))
        case BotState.BrokerAwaitingDistrict =>
          BotMessages.PROMPT_DISTRICT_BUTTON(lang) -> Some(TelegramKeyboards.districtKeyboard(lang))
        case BotState.BrokerAwaitingFloor =>
          BotMessages.PROMPT_FLOOR_BUTTON(lang) -> Some(TelegramKeyboards.floorKeyboard(lang))
        case BotState.BrokerAwaitingTotalFloors =>
          BotMessages.PROMPT_TOTAL_FLOORS_BUTTON(lang) -> Some(
            TelegramKeyboards.totalFloorsKeyboard(lang)
          )
        case BotState.BrokerAwaitingBuildingType =>
          BotMessages.PROMPT_BUILDING_TYPE_BUTTON(lang) -> Some(
            TelegramKeyboards.buildingTypeKeyboard(lang)
          )
        case BotState.BrokerAwaitingCondition =>
          BotMessages.PROMPT_CONDITION_BUTTON(lang) -> Some(
            TelegramKeyboards.conditionKeyboard(lang)
          )
        case _ => "" -> None
      }

    private def showListingPreview(
        telegramId: Long,
        chatId: Long,
        lang: Language,
      ): F[Unit] =
      for {
        session <- sessionsRepo.findByTelegramId(telegramId).transact(xa)
        botContext <- session
          .flatMap(_.context)
          .traverse(_.decodeAsF[F, BotContext])
          .map(_.getOrElse(BotContext(mode = BotMode.Buyer)))

        brokerFlow = botContext
          .broker
          .getOrElse(
            BrokerFlowContext(
              draft = AdminPostingContext(
                forwardedMessage = ForwardedMessage(None, List.empty, None)
              ),
              currentStep = BrokerStep.Start,
            )
          )
        context = brokerFlow.draft

        previewText = formatPreviewText(context, lang)

        _ <- sessionsRepo
          .updateState(telegramId) { session =>
            session
              .context
              .fold(BotContext(BotMode.Broker).pure[ConnectionIO])(
                _.decodeAsF[ConnectionIO, BotContext]
              )
              .map { ctx =>
                ctx.copy(broker = Some(brokerFlow.copy(draft = context)))
              }
              .map { updatedContext =>
                session.copy(
                  state = BotState.BrokerAwaitingConfirmation,
                  context = Some(updatedContext.asJson),
                )
              }
          }
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
        botContextOpt <- session
          .flatMap(_.context)
          .traverse(_.decodeAsF[F, BotContext])

        _ <- botContextOpt.flatMap(_.broker.map(_.draft)) match {
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

    private def handleAdminPost(msg: Message, user: dto.TelegramUser): F[Unit] =
      handleAdminPostingCommand(msg, user)

    private def handleManageDrafts(msg: Message, user: dto.TelegramUser): F[Unit] =
      Methods
        .sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = BotMessages.NO_DRAFTS_MESSAGE(user.languageCode),
          replyMarkup = Some(TelegramKeyboards.brokerHomeKeyboard(user.languageCode)),
        )
        .exec(api)
        .void

    private def handlePostToChannel(msg: Message, user: dto.TelegramUser): F[Unit] =
      Methods
        .sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = BotMessages.FEATURE_COMING_SOON(user.languageCode),
          replyMarkup = Some(TelegramKeyboards.brokerHomeKeyboard(user.languageCode)),
        )
        .exec(api)
        .void

    private def handleListingTypeSelection(
        msg: Message,
        user: dto.TelegramUser,
        listingType: ListingType,
      ): F[Unit] =
      for {
        _ <- updateAdminContextAndNextStep(
          telegramId = user.telegramId,
          chatId = msg.chat.id,
          updateFn = _.copy(listingType = Some(listingType)),
          nextState = BotState.BrokerAwaitingPrice,
          lang = user.languageCode,
        )
      } yield ()

    // ============================================================
    // BROKER POSTING - BUTTON-FIRST UX HANDLERS
    // ============================================================

    private def handlePricePostingSelection(
        msg: Message,
        user: dto.TelegramUser,
        price: Option[BigDecimal],
      ): F[Unit] =
      updateAdminContextAndNextStep(
        telegramId = user.telegramId,
        chatId = msg.chat.id,
        updateFn = _.copy(price = price),
        nextState = BotState.BrokerAwaitingCity,
        lang = user.languageCode,
      )

    private def handleCityPostingSelection(
        msg: Message,
        user: dto.TelegramUser,
        city: Option[String],
      ): F[Unit] =
      updateAdminContextAndNextStep(
        telegramId = user.telegramId,
        chatId = msg.chat.id,
        updateFn = _.copy(city = city),
        nextState = BotState.BrokerAwaitingRooms,
        lang = user.languageCode,
      )

    private def handlePhonePostingSelection(
        msg: Message,
        user: dto.TelegramUser,
        phone: Option[String],
      ): F[Unit] =
      updateAdminContextAndNextStep(
        telegramId = user.telegramId,
        chatId = msg.chat.id,
        updateFn = _.copy(phone = phone),
        nextState = BotState.BrokerAwaitingDistrict,
        lang = user.languageCode,
      )

    private def handleDistrictPostingSelection(
        msg: Message,
        user: dto.TelegramUser,
        district: Option[String],
      ): F[Unit] =
      updateAdminContextAndNextStep(
        telegramId = user.telegramId,
        chatId = msg.chat.id,
        updateFn = _.copy(district = district),
        nextState = BotState.BrokerAwaitingFloor,
        lang = user.languageCode,
      )

    private def handleFloorPostingSelection(
        msg: Message,
        user: dto.TelegramUser,
        floor: Option[Int],
      ): F[Unit] =
      updateAdminContextAndNextStep(
        telegramId = user.telegramId,
        chatId = msg.chat.id,
        updateFn = _.copy(floor = floor),
        nextState = BotState.BrokerAwaitingTotalFloors,
        lang = user.languageCode,
      )

    private def handleTotalFloorsPostingSelection(
        msg: Message,
        user: dto.TelegramUser,
        totalFloors: Option[Int],
      ): F[Unit] =
      updateAdminContextAndNextStep(
        telegramId = user.telegramId,
        chatId = msg.chat.id,
        updateFn = _.copy(totalFloors = totalFloors),
        nextState = BotState.BrokerAwaitingBuildingType,
        lang = user.languageCode,
      )

    private def handleBuildingTypePostingSelection(
        msg: Message,
        user: dto.TelegramUser,
        buildingType: Option[String],
      ): F[Unit] =
      updateAdminContextAndNextStep(
        telegramId = user.telegramId,
        chatId = msg.chat.id,
        updateFn = _.copy(buildingType = buildingType),
        nextState = BotState.BrokerAwaitingCondition,
        lang = user.languageCode,
      )

    private def handleConditionPostingSelection(
        msg: Message,
        user: dto.TelegramUser,
        condition: Option[String],
      ): F[Unit] =
      // All fields collected, show preview
      showListingPreview(user.telegramId, msg.chat.id, user.languageCode)

    private def handleAdminPostCancel(msg: Message, user: dto.TelegramUser): F[Unit] = {

      val cancelText = BotMessages.LISTING_POSTING_CANCELLED(user.languageCode)

      for {
        _ <- updateStatePreservingContext(user.telegramId, BotState.Idle, user.languageCode)
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
                  telegramChannelId = None, // Will be set after successful Telegram post
                  telegramMessageId = None, // Will be set after successful Telegram post
                  createdAt = now,
                  updatedAt = now,
                  approvedAt = None,
                  approvedBy = None,
                )

            // STEP 2: Telegram Posting FIRST
            telegramPostResult <- postToTelegramChannel(
              context = context,
              listingId = listingId,
              chatId = chatId,
              lang = lang,
            )

            // STEP 3: Database Write ONLY after successful Telegram post
            _ <- telegramPostResult match {
              case Some(messageId) =>
                // Telegram post succeeded - now save to database with message_id
                val listingWithTelegramId = listingDto.copy(
                  telegramChannelId = Some(context.selectedChannelId.get),
                  telegramMessageId = Some(messageId.toString),
                )
                listingsRepo.create(listingWithTelegramId)(lang).transact(xa)
              case None =>
                // Telegram post failed - do NOT save to database
                Logger[F].error(s"Telegram post failed for listing $listingId") *>
                  MonadCancel[F].raiseError[Unit](new RuntimeException("Telegram post failed"))
            }

            // ONLY HERE - both Telegram and Database succeeded
            _ <- Logger[F].info(
              s"SUCCESS: Listing $listingId posted to Telegram and saved to database"
            )

            _ <- updateStatePreservingContext(telegramUserId, BotState.Idle, lang)

            _ <- Methods
              .sendMessage(
                chatId = ChatIntId(chatId),
                text = BotMessages.LISTING_POSTED_SUCCESSFULLY(lang),
              )
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

    private def postToTelegramChannel(
        context: AdminPostingContext,
        listingId: uz.scala.domain.ListingId,
        chatId: Long,
        lang: Language,
      ): F[Option[String]] =
      for {
        channelId <- context.selectedChannelId match {
          case Some(id) => id.pure[F]
          case None =>
            Logger[F].error("No channel selected for posting") *>
              -1L.pure[F] // placeholder that will cause early return
        }

        // Early return if no valid channel
        _ <-
          if (channelId == -1L)
            Logger[F].error("No channel ID available") *> Option.empty[String].pure[F]
          else ().pure[F]

        // Format the listing message
        messageText = formatListingForTelegram(context, lang)

        // Send to Telegram channel
        message <- Methods
          .sendMessage(
            chatId = ChatIntId(channelId),
            text = messageText,
            parseMode = Some(Html),
          )
          .exec(api)

        _ <- Logger[F].info(
          s"Successfully posted to Telegram channel, message_id: ${message.messageId}"
        )

      } yield Some(message.messageId.toString)

    private def formatListingForTelegram(
        context: AdminPostingContext,
        lang: Language,
      ): String = {
      val title = context.forwardedMessage.text.getOrElse("")
      val price = context.price.map(p => s"💰 $$p").getOrElse("Narx kelishilmagan")
      val city = context.city.map(c => s"🏙️ $c").getOrElse("")
      val rooms = context.rooms.map(r => s"🏠 $r xonali").getOrElse("")
      val phone = context.phone.map(p => s"📞 $p").getOrElse("")
      val district = context.district.map(d => s"📍 $d").getOrElse("")

      val typeEmoji = context.listingType match {
        case Some(ListingType.ForRent) => "🔑 Ijaraga"
        case Some(ListingType.ForSale) => "💰 Sotishga"
        case None => "🏠 E'lon"
      }

      s"""<b>$typeEmoji</b>

$title

$price
$city
$rooms
$district

$phone"""
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

    // Mode-based feature blocking helpers
    private def checkUserMode(
        telegramId: Long,
        requiredMode: BotMode,
        action: F[Unit],
      )(implicit
        xa: doobie.Transactor[F]
      ): F[Unit] =
      for {
        currentMode <- getCurrentMode(telegramId)
        _ <-
          if (currentMode == requiredMode)
            action
          else
            usersRepo.findByTelegramId(telegramId).transact(xa).flatMap {
              case Some(userSession) =>
                val lang = userSession.languageCode
                val errorText =
                  if (requiredMode == BotMode.Buyer)
                    BotMessages.ERROR_BROKER_FEATURE_IN_BUYER_MODE(lang)
                  else
                    BotMessages.ERROR_BUYER_FEATURE_IN_BROKER_MODE(lang)
                Methods
                  .sendMessage(chatId = ChatIntId(telegramId), text = errorText)
                  .exec(api)
                  .void
              case None =>
                Logger[F].error(s"User session not found for telegramId: $telegramId").void
            }
      } yield ()

    private def getCurrentMode(telegramId: Long)(implicit xa: doobie.Transactor[F]): F[BotMode] =
      sessionsRepo
        .findByTelegramId(telegramId)
        .transact(xa)
        .asOptionT
        .subflatMap(_.context)
        .foldF(
          Logger[F]
            .warn(s"No session found for telegramId: $telegramId, defaulting to Buyer mode")
            .as[BotMode](BotMode.Buyer)
        )(_.decodeAsF[F, BotContext].map(_.mode))

    // Helper method to update state while preserving existing context
    private def updateStatePreservingContext(
        telegramId: Long,
        newState: BotState,
        language: Language,
      )(implicit
        xa: doobie.Transactor[F]
      ): F[Unit] =
      for {
        // Get current session to preserve all existing context
        currentSession <- sessionsRepo.findByTelegramId(telegramId).transact(xa)
        implicit0(lang: Language) = language
        // Update state while keeping existing context unchanged
        now <- Calendar[F].currentZonedDateTime
        _ <- sessionsRepo
          .upsert(
            currentSession match {
              case Some(session) =>
                session.copy(
                  state = newState,
                  updatedAt = now,
                )
              case None =>
                // No session exists, create new one (shouldn't happen in normal flow)
                dto.TelegramSession(
                  telegramId = telegramId,
                  state = newState,
                  context = None,
                  updatedAt = now,
                )
            }
          )
          .transact(xa)
      } yield ()

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
