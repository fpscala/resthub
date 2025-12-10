package uz.scala.auth.utils

import cats.MonadThrow
import cats.data.EitherT
import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all._
import dev.profunktor.auth.jwt._
import io.circe.Decoder
import org.http4s.Credentials.Token
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.headers.`WWW-Authenticate`

import uz.scala.Language
import uz.scala.exception.AError.AuthError
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.shared.ResponseMessages._

object AuthMiddleware {
  def getBearerToken[F[_]: MonadThrow]: Kleisli[F, Request[F], Option[JwtToken]] =
    Kleisli { request =>
      MonadThrow[F].pure(
        request
          .headers
          .get[Authorization]
          .collect {
            case Authorization(Token(AuthScheme.Bearer, token)) => JwtToken(token)
          }
          .orElse {
            request.params.get("X-Access-Token").map(JwtToken.apply)
          }
      )
    }

  def validateAndDecodeJwt[F[_]: MonadThrow, A: Decoder](
      token: JwtToken,
      jwtAuth: JwtSymmetricAuth,
    )(implicit
      language: Language
    ): F[Either[String, A]] =
    jwtDecode(token, jwtAuth)
      .flatMap { claim =>
        io.circe.parser.decode[A](claim.content) match {
          case Right(user) => MonadThrow[F].pure(user.asRight[String])
          case Left(err) =>
            MonadThrow[F].pure(s"Failed to decode JWT payload: ${err.getMessage}".asLeft[A])
        }
      }
      .handleErrorWith { _ =>
        MonadThrow[F].pure(INVALID_TOKEN(language).asLeft[A])
      }

  def getAndValidateJwtToken[F[_]: MonadThrow, A: Decoder](
      jwtAuth: JwtSymmetricAuth
    )(implicit
      language: Language
    ): Kleisli[F, Request[F], Either[String, A]] =
    Kleisli { request =>
      EitherT
        .fromOptionF(getBearerToken[F].apply(request), BEARER_TOKEN_NOT_FOUND(language))
        .flatMapF { token =>
          validateAndDecodeJwt[F, A](token, jwtAuth)
        }
        .value
    }

  def apply[F[_]: Sync, A: Decoder](
      jwtAuth: JwtSymmetricAuth
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

    Kleisli { (req: Request[F]) =>
      implicit val language: Language = req.lang
      OptionT {
        EitherT(getAndValidateJwtToken[F, A](jwtAuth).apply(req))
          .foldF(
            err => onFailure(AuthedRequest(err, req)).value,
            user => routes(AuthedRequest(user, req)).value,
          )
      }
    }
  }
}
