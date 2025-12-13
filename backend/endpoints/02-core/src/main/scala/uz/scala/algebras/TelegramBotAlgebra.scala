package uz.scala.algebras

import cats.effect.MonadCancelThrow
import cats.implicits._
import doobie.implicits._
import io.circe.syntax._
import org.typelevel.log4cats.Logger
import telegramium.bots.ChatIntId
import telegramium.bots.Message
import telegramium.bots.Update
import telegramium.bots.high.Api
import telegramium.bots.high.Methods
import telegramium.bots.high.implicits._

import uz.scala.Language
import uz.scala.domain.enums.BotState
import uz.scala.domain.enums.ListingStatus
import uz.scala.domain.listings.ListingFilters
import uz.scala.domain.telegram.SearchContext
import uz.scala.effects.Calendar
import uz.scala.repos.TelegramSessionsRepository
import uz.scala.repos.TelegramUsersRepository
import uz.scala.repos.dto
import uz.scala.shared.BotMessages
import uz.scala.syntax.all.circeSyntaxJsonDecoderOps
import uz.scala.syntax.refined._

trait TelegramBotAlgebra[F[_]] {
  def processUpdate(update: Update): F[Unit]
  def setupWebhook(webhookUrl: String): F[Unit]
}

object TelegramBotAlgebra {
  def make[F[_]: MonadCancelThrow: Calendar: Logger](
      api: Api[F],
      usersRepo: TelegramUsersRepository[doobie.ConnectionIO],
      sessionsRepo: TelegramSessionsRepository[doobie.ConnectionIO],
      listingsAlgebra: ListingsAlgebra[F],
    )(implicit
      xa: doobie.Transactor[F]
    ): TelegramBotAlgebra[F] =
    new Impl[F](api, usersRepo, sessionsRepo, listingsAlgebra)

  private class Impl[F[_]: MonadCancelThrow: Calendar: Logger](
      api: Api[F],
      usersRepo: TelegramUsersRepository[doobie.ConnectionIO],
      sessionsRepo: TelegramSessionsRepository[doobie.ConnectionIO],
      listingsAlgebra: ListingsAlgebra[F],
    )(implicit
      xa: doobie.Transactor[F]
    ) extends TelegramBotAlgebra[F] {
    override def processUpdate(update: Update): F[Unit] =
      update.message match {
        case Some(msg) => handleMessage(msg)
        case None => Logger[F].debug("Received non-message update, ignoring")
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

        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.SELECT_CITY(lang),
          )
          .exec(api)
          .void
      } yield ()

    private def handleHelpCommand(msg: Message, user: dto.TelegramUser): F[Unit] = {
      val lang = Language.withName(user.languageCode)
      val helpText = BotMessages.WELCOME_NEW(lang) // Reuse welcome message for now

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
        case BotState.AwaitingPriceRange => handlePriceInput(msg, user, session, text)
        case BotState.Registering => Logger[F].debug("Registration not implemented yet")
      }

    private def handleCityInput(
        msg: Message,
        user: dto.TelegramUser,
        cityName: String,
      ): F[Unit] = {
      val context = SearchContext(city = Some(cityName))

      for {
        _ <- sessionsRepo
          .updateState(
            user.telegramId,
            BotState.AwaitingPriceRange,
            Some(context.asJson),
          )
          .transact(xa)

        lang = Language.withName(user.languageCode)

        _ <- Methods
          .sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = BotMessages.SELECT_PRICE_RANGE(lang),
          )
          .exec(api)
          .void
      } yield ()
    }

    private def handlePriceInput(
        msg: Message,
        user: dto.TelegramUser,
        session: dto.TelegramSession,
        priceRange: String,
      ): F[Unit] =
      for {
        // Parse existing context
        searchContext <- session
          .context
          .map(_.decodeAsF[F, SearchContext])
          .getOrElse(SearchContext().pure[F])

        // Parse price range (e.g., "1000-2000" or just "1000")
        (minPrice, maxPrice) = parsePriceRange(priceRange)

        // Search listings
        filters = ListingFilters(
          city = searchContext.city,
          minPrice = minPrice.map(BigDecimal(_)),
          maxPrice = maxPrice.map(BigDecimal(_)),
          status = Some(ListingStatus.Approved),
          page = Some(1),
          size = Some(10),
        )

        result <- listingsAlgebra.search(filters)

        // Send results
        lang = Language.withName(user.languageCode)
        _ <- sendSearchResults(msg.chat.id, result.data.size, lang)

        // Reset state to Idle
        _ <- sessionsRepo
          .updateState(user.telegramId, BotState.Idle, None)
          .transact(xa)
      } yield ()

    private def sendSearchResults(
        chatId: Long,
        count: Int,
        lang: Language,
      ): F[Unit] = {
      val message =
        if (count == 0) BotMessages.NO_RESULTS(lang)
        else BotMessages.searchResults(count, lang)

      Methods
        .sendMessage(
          chatId = ChatIntId(chatId),
          text = message,
        )
        .exec(api)
        .void
    }

    private def parsePriceRange(text: String): (Option[Int], Option[Int]) =
      text.split("-").map(_.trim).filter(_.nonEmpty) match {
        case Array(min, max) => (min.toIntOption, max.toIntOption)
        case Array(min) => (min.toIntOption, None)
        case _ => (None, None)
      }

    override def setupWebhook(webhookUrl: String): F[Unit] =
      for {
        _ <- Logger[F].info(s"Setting up webhook for URL: $webhookUrl")
        response <- Methods.setWebhook(url = webhookUrl).exec(api)
        _ <- if (response)
          Logger[F].info("Webhook setup successful")
        else
          Logger[F].error("Webhook setup failed")
      } yield ()
  }
}
