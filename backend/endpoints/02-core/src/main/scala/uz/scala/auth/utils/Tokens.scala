package uz.scala.auth.utils

import java.util.UUID

import cats.Monad
import cats.implicits._
import dev.profunktor.auth.jwt.JwtSecretKey
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.auth.jwt.jwtEncode
import io.circe.Encoder
import pdi.jwt.JwtAlgorithm
import pdi.jwt.JwtClaim

import uz.scala.auth.AuthConfig
import uz.scala.domain.auth.AuthTokens
import uz.scala.effects.GenUUID
import uz.scala.syntax.all.genericSyntaxGenericTypeOps
import uz.scala.syntax.refined.commonSyntaxAutoUnwrapV

trait Tokens[F[_]] {
  def createAccessToken[U: Encoder](data: U): F[JwtToken]
  def createRefreshToken: F[String]
  def createTokenPair[U: Encoder](data: U): F[AuthTokens]
}

object Tokens {
  def make[F[_]: GenUUID: Monad](
      jwtExpire: JwtExpire[F],
      config: AuthConfig,
    ): Tokens[F] =
    new Tokens[F] {
      private def encodeToken: JwtClaim => F[JwtToken] =
        jwtEncode[F](_, JwtSecretKey(config.tokenKey.toCharArray), JwtAlgorithm.HS256)

      override def createAccessToken[U: Encoder](data: U): F[JwtToken] =
        for {
          accessTokenClaim <- jwtExpire.expiresIn(
            JwtClaim(data.toJson),
            config.accessTokenExpiration,
          )
          accessToken <- encodeToken(accessTokenClaim)
        } yield accessToken

      override def createRefreshToken: F[String] =
        GenUUID[F].make.map(_.toString)

      override def createTokenPair[U: Encoder](data: U): F[AuthTokens] =
        for {
          accessToken <- createAccessToken(data)
          refreshToken <- createRefreshToken
        } yield AuthTokens(
          accessToken = accessToken.value,
          refreshToken = refreshToken,
          tokenType = "Bearer",
          expiresIn = config.accessTokenExpiration.toSeconds,
        )
    }
}
