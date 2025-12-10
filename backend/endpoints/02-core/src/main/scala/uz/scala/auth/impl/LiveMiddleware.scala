package uz.scala.auth.impl

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt.JwtAuth
import org.http4s.server
import pdi.jwt.JwtAlgorithm

import uz.scala.auth.AuthConfig
import uz.scala.auth.utils.AuthMiddleware
import uz.scala.auth.utils.JwtAuthMiddleware
import uz.scala.domain.AuthedUser
import uz.scala.syntax.refined.commonSyntaxAutoUnwrapV

object LiveMiddleware {
  // Stateless JWT middleware - no Redis lookup
  def make[F[_]: Sync](
      jwtConfig: AuthConfig
    ): server.AuthMiddleware[F, AuthedUser] = {
    val userJwtAuth = JwtAuth.hmac(jwtConfig.tokenKey.toCharArray, JwtAlgorithm.HS256)
    AuthMiddleware[F, AuthedUser](userJwtAuth)
  }

  def makeForApp[F[_]: Sync](
      jwtConfig: AuthConfig
    ): server.AuthMiddleware[F, Unit] =
    JwtAuthMiddleware[F, Unit](
      JwtAuth.hmac(jwtConfig.appTokenKey.toCharArray, JwtAlgorithm.HS256),
      token => (token.value == jwtConfig.appToken.value).guard[Option].pure[F],
    )
}
