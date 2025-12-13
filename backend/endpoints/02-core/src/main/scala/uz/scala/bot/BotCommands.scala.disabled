package uz.scala.bot

import cats.effect.Async
import cats.implicits._
import doobie.syntax.connectionio._
import io.circe.Json
import io.circe.syntax._
import org.typelevel.log4cats.Logger
import telegramium.bots._
import telegramium.bots.high._

import uz.scala.Language
import uz.scala.algebras.ListingsAlgebra
import uz.scala.domain.enums.{BotState, ListingStatus}
import uz.scala.domain.listings.ListingFilters
import uz.scala.domain.telegram.SearchContext
import uz.scala.effects.Calendar
import uz.scala.repos.TelegramSessionsRepository
import uz.scala.repos.TelegramUsersRepository
import uz.scala.repos.dto
import uz.scala.shared.BotMessages

final case class BotCommands[F[_]: Async: Logger: Calendar](
    telegramUsersRepo: TelegramUsersRepository[doobie.ConnectionIO],
    telegramSessionsRepo: TelegramSessionsRepository[doobie.ConnectionIO],
    listingsAlgebra: ListingsAlgebra[F],
  )(implicit
    xa: doobie.Transactor[F],
    api: Api[F],
  ) {

  private val uzbekCities = List(
    "Toshkent", "Samarqand", "Buxoro", "Namangan",
    "Andijon", "Farg'ona", "Qarshi", "Nukus"
  )

  private val priceRanges = List(
    ("0-2000000", "0 - 2 mln UZS"),
    ("2000000-5000000", "2 - 5 mln UZS"),
    ("5000000-10000000", "5 - 10 mln UZS"),
    ("10000000-999999999", "10+ mln UZS")
  )

  def handleStart(msg: Message): F[Unit] = {
    val telegramId = msg.from.map(_.id).getOrElse(0L)
    val firstName = msg.from.flatMap(_.firstName).getOrElse("User")
    val languageCode = msg.from.flatMap(_.languageCode).getOrElse("uz")

    for {
      _ <- Logger[F].info(s"Handling /start for telegram_id: $telegramId")

      // Find or create telegram user
      existingUser <- telegramUsersRepo.findByTelegramId(telegramId).transact(xa)
      now <- Calendar[F].currentZonedDateTime

      _ <- existingUser match {
        case Some(_) =>
          // Update last interaction
          telegramUsersRepo.updateLastInteraction(telegramId).transact(xa)
        case None =>
          // Create new user
          val newUser = dto.TelegramUser(
            telegramId = telegramId,
            userId = None,
            username = msg.from.flatMap(_.username),
            firstName = firstName,
            languageCode = languageCode,
            isRegistered = false,
            createdAt = now,
            lastInteractionAt = now
          )
          telegramUsersRepo.create(newUser)(Language.Uz).transact(xa)
      }

      // Create/reset session to IDLE
      session = dto.TelegramSession(
        telegramId = telegramId,
        state = BotState.Idle,
        context = None,
        updatedAt = now
      )
      _ <- telegramSessionsRepo.upsert(session)(Language.Uz).transact(xa)

      // Send welcome message
      lang = Language.fromString(languageCode)
      welcomeText = BotMessages.WELCOME_NEW(lang)
      _ <- api.execute(SendMessage(ChatIntId(msg.chat.id), welcomeText))

    } yield ()
  }

  def handleSearch(msg: Message): F[Unit] = {
    val telegramId = msg.from.map(_.id).getOrElse(0L)
    val languageCode = msg.from.flatMap(_.languageCode).getOrElse("uz")
    val lang = Language.fromString(languageCode)

    for {
      _ <- Logger[F].info(s"Handling /search for telegram_id: $telegramId")
      now <- Calendar[F].currentZonedDateTime

      // Update session state to AWAITING_CITY
      _ <- telegramSessionsRepo.updateState(
        telegramId,
        BotState.AwaitingCity,
        Some(SearchContext().asJson)
      ).transact(xa)

      // Send city selection keyboard
      keyboard = cityKeyboard()
      _ <- api.execute(SendMessage(
        ChatIntId(msg.chat.id),
        BotMessages.SELECT_CITY(lang),
        replyMarkup = Some(keyboard)
      ))
    } yield ()
  }

  def handleCallbackQuery(query: CallbackQuery): F[Unit] = {
    val telegramId = query.from.id
    val data = query.data.getOrElse("")
    val chatId = query.message.map(_.chat.id).getOrElse(0L)

    for {
      _ <- Logger[F].info(s"Handling callback: $data for telegram_id: $telegramId")

      // Get current session
      sessionOpt <- telegramSessionsRepo.findByTelegramId(telegramId).transact(xa)

      _ <- sessionOpt match {
        case Some(session) =>
          session.state match {
            case BotState.AwaitingCity if data.startsWith("city:") =>
              handleCitySelection(chatId, telegramId, data.stripPrefix("city:"), session)

            case BotState.AwaitingPriceRange if data.startsWith("price:") =>
              handlePriceRangeSelection(chatId, telegramId, data.stripPrefix("price:"), session)

            case _ =>
              Logger[F].warn(s"Unexpected callback in state: ${session.state}")
          }
        case None =>
          Logger[F].warn(s"No session found for telegram_id: $telegramId")
      }

      // Answer callback query (removes loading state)
      _ <- api.execute(AnswerCallbackQuery(query.id))
    } yield ()
  }

  private def handleCitySelection(
      chatId: Long,
      telegramId: Long,
      city: String,
      session: dto.TelegramSession
    ): F[Unit] = {
    for {
      _ <- Logger[F].info(s"City selected: $city")

      // Update context with selected city
      newContext = session.context
        .flatMap(_.as[SearchContext].toOption)
        .getOrElse(SearchContext())
        .copy(city = Some(city))

      // Update session to AWAITING_PRICE_RANGE
      _ <- telegramSessionsRepo.updateState(
        telegramId,
        BotState.AwaitingPriceRange,
        Some(newContext.asJson)
      ).transact(xa)

      // Send price range keyboard
      keyboard = priceRangeKeyboard()
      _ <- api.execute(SendMessage(
        ChatIntId(chatId),
        BotMessages.SELECT_PRICE_RANGE(Language.Uz),
        replyMarkup = Some(keyboard)
      ))
    } yield ()
  }

  private def handlePriceRangeSelection(
      chatId: Long,
      telegramId: Long,
      priceRange: String,
      session: dto.TelegramSession
    ): F[Unit] = {
    val Array(minPriceStr, maxPriceStr) = priceRange.split("-")
    val minPrice = BigDecimal(minPriceStr)
    val maxPrice = BigDecimal(maxPriceStr)

    for {
      _ <- Logger[F].info(s"Price range selected: $minPrice - $maxPrice")

      // Get search context
      context = session.context
        .flatMap(_.as[SearchContext].toOption)
        .getOrElse(SearchContext())

      city = context.city.getOrElse("Toshkent")

      // Search listings
      cityRefined = eu.timepit.refined.refineV[eu.timepit.refined.string.NonEmpty](city).toOption
      filters = ListingFilters(
        city = cityRefined,
        minPrice = Some(minPrice),
        maxPrice = Some(maxPrice),
        status = Some(ListingStatus.Approved),
        page = Some(1),
        size = Some(10)
      )

      results <- listingsAlgebra.search(filters)

      // Send results
      _ <- if (results.items.isEmpty) {
        api.execute(SendMessage(
          ChatIntId(chatId),
          BotMessages.NO_RESULTS(Language.Uz)
        ))
      } else {
        for {
          _ <- api.execute(SendMessage(
            ChatIntId(chatId),
            BotMessages.searchResults(results.items.size, Language.Uz)
          ))
          _ <- results.items.traverse { listing =>
            sendListingCard(chatId, listing)
          }
        } yield ()
      }

      // Reset session to IDLE
      _ <- telegramSessionsRepo.updateState(
        telegramId,
        BotState.Idle,
        None
      ).transact(xa)

    } yield ()
  }

  private def sendListingCard(chatId: Long, listing: uz.scala.domain.listings.ListingOutput): F[Unit] = {
    val caption = s"""🏠 ${listing.title.value}

📍 Shahar: ${listing.city.value}
💰 Narx: ${formatPrice(listing.price)} UZS/oy

📝 ${listing.description.value.take(200)}...

👤 Egasi: ${listing.owner.firstName.value} ${listing.owner.lastName.value}
📧 Email: ${listing.owner.email.value}
"""

    listing.images.headOption match {
      case Some(imageUrl) =>
        api.execute(SendPhoto(
          ChatIntId(chatId),
          InputPartFile(new java.io.File(imageUrl)), // Note: This needs proper file handling
          caption = Some(caption)
        )).void.handleErrorWith { _ =>
          // Fallback to text if image fails
          api.execute(SendMessage(ChatIntId(chatId), caption)).void
        }
      case None =>
        api.execute(SendMessage(ChatIntId(chatId), caption)).void
    }
  }

  private def formatPrice(price: BigDecimal): String = {
    val millions = price / 1000000
    if (millions >= 1) {
      f"$millions%.1f mln"
    } else {
      f"${price}%.0f"
    }
  }

  private def cityKeyboard(): InlineKeyboardMarkup = {
    InlineKeyboardMarkup(
      uzbekCities.grouped(2).map { row =>
        row.map { city =>
          InlineKeyboardButton(city, callbackData = Some(s"city:$city"))
        }
      }.toList
    )
  }

  private def priceRangeKeyboard(): InlineKeyboardMarkup = {
    InlineKeyboardMarkup(
      priceRanges.map { case (value, label) =>
        List(InlineKeyboardButton(label, callbackData = Some(s"price:$value")))
      }
    )
  }
}
