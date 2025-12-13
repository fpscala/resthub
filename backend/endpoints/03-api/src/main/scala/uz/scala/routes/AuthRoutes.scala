package uz.scala.routes

import cats.MonadThrow
import cats.implicits.catsSyntaxApplyOps
import cats.implicits.toFlatMapOps
import org.http4s.AuthedRoutes
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.typelevel.log4cats.Logger

import uz.scala.Language
import uz.scala.algebras.AuthAlgebra
import uz.scala.auth.impl.Auth
import uz.scala.domain.AuthedUser
import uz.scala.domain.auth._
import uz.scala.domain.auth.RegisterInput
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

final case class AuthRoutes[F[_]: Logger: JsonDecoder: MonadThrow](
    auth: Auth[F, AuthedUser],
    authAlgebra: AuthAlgebra[F],
  ) extends Routes[F, AuthedUser] {
  override val path = "/auth"

  override val public: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "login" =>
        implicit val language: Language = req.lang
        req.decodeR[UserCredentials] { credentials =>
          // TODO: Extract DeviceInfo from request headers if needed
          auth
            .loginByPassword(credentials, deviceInfo = None)
            .flatMap(Ok(_))
        }

      case req @ POST -> Root / "refresh" =>
        implicit val language: Language = req.lang
        req.decodeR[RefreshTokenRequest] { refreshReq =>
          auth.refresh(refreshReq.refreshToken, deviceInfo = None).flatMap(Ok(_))
        }

      case req @ POST -> Root / "register" =>
        implicit val language: Language = req.lang
        req.decodeR[RegisterInput] { input =>
          authAlgebra
            .register(input)
            .flatMap(tokens => Ok(tokens))
        }
    }

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case GET -> Root / "me" as user =>
      Ok(user)

    case ar @ POST -> Root / "logout" as _ =>
      ar.req.decodeR[RefreshTokenRequest] { refreshReq =>
        auth.logout(refreshReq.refreshToken) *> NoContent()
      }
  }
}
