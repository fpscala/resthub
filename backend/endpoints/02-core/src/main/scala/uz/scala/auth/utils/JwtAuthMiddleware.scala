package uz.scala.auth.utils

import cats.Applicative
import cats.MonadThrow
import cats.data.EitherT
import cats.data.Kleisli
import cats.data.OptionT
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import dev.profunktor.auth.jwt.JwtToken
import org.http4s.AuthedRequest
import org.http4s.AuthedRoutes
import org.http4s.Challenge
import org.http4s.Request
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`WWW-Authenticate`
import org.http4s.server

import uz.scala.Language
import uz.scala.exception.AError.AuthError
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.shared.ResponseMessages.AUTHENTICATION_REQUIRED
import uz.scala.shared.ResponseMessages.INVALID_TOKEN

object JwtAuthMiddleware {
  def apply[F[_]: MonadThrow, A](
      jwtAuth: JwtSymmetricAuth,
      authenticate: JwtToken => F[Option[A]],
    ): server.AuthMiddleware[F, A] = { routes: AuthedRoutes[A, F] =>
    val dsl = new Http4sDsl[F] {}; import dsl._

    val onFailure: AuthedRoutes[String, F] =
      Kleisli(ar =>
        OptionT.liftF(
          Unauthorized(
            `WWW-Authenticate`(
              Challenge(
                "Bearer",
                AUTHENTICATION_REQUIRED(ar.req.lang),
              )
            ),
            AuthError.Unauthorized(ar.context).json,
          )
        )
      )

    def getUser(
        token: JwtToken
      )(implicit
        language: Language
      ): EitherT[F, String, A] =
      EitherT.fromOptionF(authenticate(token), INVALID_TOKEN(language))

    Kleisli { (req: Request[F]) =>
      implicit val language: Language = req.lang
      OptionT {
        EitherT(
          AuthMiddleware.getAndValidateJwtToken[F](jwtAuth, _ => Applicative[F].unit).apply(req)
        )
          .flatMap(getUser)
          .foldF(
            err => onFailure(AuthedRequest(err, req)).value,
            user => routes(AuthedRequest(user, req)).value,
          )
      }
    }

  }
}
