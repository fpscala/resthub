package uz.scala.auth.impl

import cats.effect.Sync
import dev.profunktor.auth.jwt.JwtAuth
import org.http4s.server
import pdi.jwt.JwtAlgorithm

import uz.scala.auth.AuthConfig
import uz.scala.auth.utils.AuthMiddleware
import uz.scala.domain.AuthedUser

object LiveMiddleware {
  // Stateless JWT middleware - no Redis lookup
  def make[F[_]: Sync](
      jwtConfig: AuthConfig
    ): server.AuthMiddleware[F, AuthedUser] = {
    val userJwtAuth = JwtAuth.hmac(jwtConfig.tokenKey.value.toCharArray, JwtAlgorithm.HS256)
    AuthMiddleware[F, AuthedUser](userJwtAuth)
  }

  def makeForApp[F[_]: Sync](
      jwtConfig: AuthConfig
    ): server.AuthMiddleware[F, Unit] = {
    val appJwtAuth = JwtAuth.hmac(jwtConfig.appTokenKey.value.toCharArray, JwtAlgorithm.HS256)
    AuthMiddleware[F, Unit](appJwtAuth)
  }
}
