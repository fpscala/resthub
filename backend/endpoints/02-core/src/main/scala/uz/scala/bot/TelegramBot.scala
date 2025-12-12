package uz.scala.bot

import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import org.http4s.Uri
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import telegramium.bots.high._

import uz.scala.algebras.ListingsAlgebra
import uz.scala.effects.Calendar
import uz.scala.repos.TelegramSessionsRepository
import uz.scala.repos.TelegramUsersRepository

case class TelegramConfig(
    botToken: String,
    baseUrl: String,
    useWebhook: Boolean,
    webhookUrl: Option[String]
  )

object TelegramBot {

  def make[F[_]: Async: Logger: Calendar](
      config: TelegramConfig,
      telegramUsersRepo: TelegramUsersRepository[doobie.ConnectionIO],
      telegramSessionsRepo: TelegramSessionsRepository[doobie.ConnectionIO],
      listingsAlgebra: ListingsAlgebra[F],
      httpClient: Client[F]
    )(implicit
      xa: doobie.Transactor[F]
    ): Resource[F, Unit] = {

    for {
      _ <- Resource.eval(Logger[F].info("Initializing Telegram Bot..."))

      // Create API instance
      baseUri <- Resource.eval(
        Async[F].fromEither(Uri.fromString(config.baseUrl))
      )

      implicit0(api: Api[F]) <- BotApi.fromUri[F](
        baseUri,
        config.botToken
      )(httpClient)

      // Create command handlers
      commands = BotCommands[F](
        telegramUsersRepo,
        telegramSessionsRepo,
        listingsAlgebra
      )

      // Start bot
      _ <- if (config.useWebhook) {
        // Webhook mode (for production)
        config.webhookUrl match {
          case Some(url) =>
            Resource.eval(
              api.execute(telegramium.bots.SetWebhook(
                url = url,
                allowedUpdates = Some(List("message", "callback_query"))
              ))
            ) *> Resource.eval(Logger[F].info(s"Webhook set to: $url"))
          case None =>
            Resource.eval(Logger[F].error("Webhook URL not provided") *>
              Async[F].raiseError(new Exception("Webhook URL required for webhook mode")))
        }
      } else {
        // Long polling mode (for development)
        LongPollBot.polling[F](
          api,
          onMessage = handleMessage(commands, _),
          onCallbackQuery = handleCallback(commands, _)
        ).background.void
      }

      _ <- Resource.eval(Logger[F].info("Telegram Bot started successfully"))

    } yield ()
  }

  private def handleMessage[F[_]: Async: Logger](
      commands: BotCommands[F],
      msg: telegramium.bots.Message
    ): F[Unit] = {
    msg.text match {
      case Some("/start") =>
        commands.handleStart(msg).handleErrorWith { err =>
          Logger[F].error(err)(s"Error handling /start: ${err.getMessage}")
        }

      case Some("/search") =>
        commands.handleSearch(msg).handleErrorWith { err =>
          Logger[F].error(err)(s"Error handling /search: ${err.getMessage}")
        }

      case Some("/help") =>
        implicit val api = commands.api
        api.execute(telegramium.bots.SendMessage(
          telegramium.bots.ChatIntId(msg.chat.id),
          """NestHub Bot Commands:

/start - Start bot and show welcome
/search - Search for rental homes
/help - Show this help message

Developed with ❤️ using Scala + Telegramium"""
        )).void

      case Some(text) =>
        Logger[F].info(s"Unhandled message: $text")

      case None =>
        Logger[F].debug("Received message without text")
    }
  }

  private def handleCallback[F[_]: Async: Logger](
      commands: BotCommands[F],
      query: telegramium.bots.CallbackQuery
    ): F[Unit] = {
    commands.handleCallbackQuery(query).handleErrorWith { err =>
      Logger[F].error(err)(s"Error handling callback query: ${err.getMessage}")
    }
  }
}
