package uz.scala.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.typelevel.log4cats.Logger
import telegramium.bots.Update

import uz.scala.algebras.TelegramBotAlgebra
import uz.scala.domain.AuthedUser
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes
import telegramium.bots.CirceImplicits.updateDecoder

final case class TelegramBotRoutes[F[_]: Logger: JsonDecoder: MonadThrow](
    botAlgebra: TelegramBotAlgebra[F],
    botToken: String,
  ) extends Routes[F, AuthedUser] {
  override val path = "/bot"

  override val public: HttpRoutes[F] = HttpRoutes.of[F] {
    // POST /bot/webhook/{token}
    case req @ POST -> Root / "webhook" / token if token == botToken =>
      req.decodeR[Update] { update =>
        for {
          _ <- Logger[F].info(s"Received webhook update ID: ${update.updateId}")
          _ <- botAlgebra.processUpdate(update)
          resp <- Ok("OK")
        } yield resp
      }
  }
}
