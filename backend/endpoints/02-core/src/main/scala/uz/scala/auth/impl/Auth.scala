package uz.scala.auth.impl

import java.security.MessageDigest
import java.time.ZonedDateTime

import cats.effect.Sync
import cats.implicits._
import cats.~>
import doobie.ConnectionIO
import doobie.syntax.connectionio._
import io.circe.syntax.EncoderOps
import org.typelevel.log4cats.Logger
import tsec.passwordhashers.jca.SCrypt

import uz.scala.Language
import uz.scala.algebras.UsersAlgebra
import uz.scala.auth.AuthConfig
import uz.scala.auth.utils.JwtExpire
import uz.scala.auth.utils.Tokens
import uz.scala.domain.AuthedUser
import uz.scala.domain.RefreshTokenId
import uz.scala.domain.auth._
import uz.scala.effects.Calendar
import uz.scala.exception.AError
import uz.scala.exception.AError.AuthError._
import uz.scala.repos.RefreshTokensRepository
import uz.scala.repos.RolesRepository
import uz.scala.shared.ResponseMessages._
import uz.scala.syntax.option._
import uz.scala.syntax.refined.commonSyntaxAutoUnwrapV
import uz.scala.utils.ID

trait Auth[F[_], A] {
  def loginByPassword(
      credentials: UserCredentials,
      deviceInfo: Option[DeviceInfo] = None,
    )(implicit
      language: Language
    ): F[AuthTokens]

  def refresh(
      refreshToken: String,
      deviceInfo: Option[DeviceInfo] = None,
    )(implicit
      language: Language
    ): F[AuthTokens]

  def logout(refreshToken: String): F[Unit]
}

object Auth {
  def make[F[_]: Sync](
      config: AuthConfig,
      users: UsersAlgebra[F],
      usersRepository: uz.scala.repos.UsersRepository[doobie.ConnectionIO],
      rolesRepository: RolesRepository[doobie.ConnectionIO],
      refreshTokensRepository: RefreshTokensRepository[doobie.ConnectionIO],
    )(implicit
      logger: Logger[F],
      xa: doobie.Transactor[F],
      lifter: F ~> ConnectionIO,
    ): Auth[F, AuthedUser] =
    new Auth[F, AuthedUser] {
      val tokens: Tokens[F] =
        Tokens.make[F](JwtExpire[F], config)

      // SHA-256 hash for refresh token
      private def hashToken(token: String): String = {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.digest(token.getBytes("UTF-8")).map("%02x".format(_)).mkString
      }

      override def loginByPassword(
          credentials: UserCredentials,
          deviceInfo: Option[DeviceInfo] = None,
        )(implicit
          language: Language
        ): F[AuthTokens] =
        users.find(credentials.email).flatMap {
          case None =>
            NoSuchUser(USER_NOT_FOUND(language)).raiseError[F, AuthTokens]
          case Some(user) if !SCrypt.checkpwUnsafe(credentials.password, user.password) =>
            PasswordDoesNotMatch(PASSWORD_DOES_NOT_MATCH(language)).raiseError[F, AuthTokens]
          case Some(user) =>
            for {
              _ <- usersRepository.updateLastLogin(user.id).transact(xa)
              role <- rolesRepository.getRole(user.roleId).transact(xa)
              authTokens <- createTokenPair(user.toAuth(role), deviceInfo)
            } yield authTokens
        }

      override def refresh(
          refreshTokenStr: String,
          deviceInfo: Option[DeviceInfo] = None,
        )(implicit
          language: Language
        ): F[AuthTokens] = {
        val now = ZonedDateTime.now()
        val tokenHash = hashToken(refreshTokenStr)

        for {
          // Find refresh token in DB
          oldToken <- refreshTokensRepository
            .findByHash(tokenHash)
            .transact(xa)
            .getOrRaise(InvalidToken(INVALID_REFRESH_TOKEN(language)))

          result <-
            if (oldToken.revoked && oldToken.reuseWindowExpiresAt.exists(_.isAfter(now)))
              // GRACE PERIOD ACTIVE - Return the new tokens (idempotent)
              for {
                _ <- logger.info(
                  s"Refresh token reuse detected within grace period for user ${oldToken.userId}"
                )
                tokens <- handleGracePeriodReuse(oldToken)
              } yield tokens
            else if (oldToken.revoked)
              for {
                _ <- logger.warn(
                  s"Refresh token reuse detected AFTER grace period for user ${oldToken.userId} - revoking all tokens"
                )
                _ <- refreshTokensRepository
                  .revokeAllForUser(oldToken.userId, "reuse_detected")
                  .transact(xa)
                error <- InvalidToken(TOKEN_REUSE_DETECTED_ALL_REVOKED(language))
                  .raiseError[F, AuthTokens]
              } yield error
            else if (oldToken.expiresAt.isBefore(now))
              // Token expired
              InvalidToken(REFRESH_TOKEN_EXPIRED(language)).raiseError[F, AuthTokens]
            else
              // NORMAL CASE: Generate new tokens with rotation
              rotateRefreshToken(oldToken, deviceInfo)
        } yield result
      }

      override def logout(refreshTokenStr: String): F[Unit] = {
        val tokenHash = hashToken(refreshTokenStr)
        refreshTokensRepository
          .findByHash(tokenHash)
          .flatMap {
            case Some(token) =>
              refreshTokensRepository.revokeToken(token.id, "logout")
            case None =>
              ().pure[doobie.ConnectionIO]
          }
          .transact(xa)
      }

      // GRACE PERIOD REUSE HANDLER
      private def handleGracePeriodReuse(
          oldToken: uz.scala.repos.dto.RefreshToken
        )(implicit
          language: Language
        ): F[AuthTokens] =
        oldToken.replacedByTokenId match {
          case Some(newTokenId) =>
            // Return the already-generated new tokens
            refreshTokensRepository
              .findById(newTokenId)
              .flatMap {
                case Some(newToken) =>
                  // Get user and role to regenerate access token
                  for {
                    user <- usersRepository
                      .findById(oldToken.userId)
                      .getOrRaise(AError.BadRequest(USER_NOT_FOUND(language)))
                    role <- rolesRepository.getRole(user.roleId)
                    accessToken <- lifter(
                      tokens
                        .createAccessToken(user.toAuth(role))
                        .map(_.value)
                    )
                  } yield AuthTokens(
                    accessToken = accessToken,
                    refreshToken = newToken.id.value.toString, // Return the NEW refresh token
                    expiresIn = config.accessTokenExpiration.toSeconds,
                  )
                case None =>
                  InvalidToken(TOKEN_CHAIN_BROKEN(language))
                    .raiseError[doobie.ConnectionIO, AuthTokens]
              }
              .transact(xa)
          case None =>
            InvalidToken(TOKEN_REUSE_DETECTED_ALL_REVOKED(language)).raiseError[F, AuthTokens]
        }

      // TOKEN ROTATION WITH GRACE PERIOD
      private def rotateRefreshToken(
          oldToken: uz.scala.repos.dto.RefreshToken,
          deviceInfo: Option[DeviceInfo],
        )(implicit
          language: Language
        ): F[AuthTokens] =
        (for {
          // Get user and role
          now <- Calendar[ConnectionIO].currentZonedDateTime
          gracePeriodSeconds = 30L // 30 seconds grace period
          newRefreshTokenId <- ID.make[ConnectionIO, RefreshTokenId]
          newRefreshTokenStr = newRefreshTokenId.value.toString
          newRefreshTokenHash = hashToken(newRefreshTokenId.value.toString)

          newRefreshToken = uz
            .scala
            .repos
            .dto
            .RefreshToken(
              id = newRefreshTokenId,
              userId = oldToken.userId,
              tokenHash = newRefreshTokenHash,
              deviceInfo = deviceInfo.map(_.asJson),
              ipAddress = None,
              userAgent = None,
              expiresAt = now.plusDays(7),
              createdAt = now,
            )
          user <- usersRepository
            .findById(oldToken.userId)
            .getOrRaise(AError.BadRequest(USER_NOT_FOUND(language)))
          role <- rolesRepository.getRole(user.roleId)

          // Save new refresh token
          _ <- refreshTokensRepository.create(newRefreshToken)

          // Mark old token as replaced (with grace period)
          _ <- refreshTokensRepository.update(oldToken.id)(old =>
            old.copy(
              revoked = true,
              revokedAt = Some(now),
              revokeReason = Some("token_rotation"),
              replacedByTokenId = Some(newRefreshTokenId),
              reuseWindowExpiresAt = Some(now.plusSeconds(gracePeriodSeconds)),
            )
          )
          accessToken <- lifter(
            tokens
              .createAccessToken(user.toAuth(role))
              .map(_.value)
          )
        } yield AuthTokens(
          accessToken = accessToken,
          refreshToken = newRefreshTokenStr,
          expiresIn = config.accessTokenExpiration.toSeconds,
        )).transact(xa)

      // CREATE TOKEN PAIR (Login)
      private def createTokenPair(
          user: AuthedUser,
          deviceInfo: Option[DeviceInfo],
        ): F[AuthTokens] = (for {
        now <- Calendar[ConnectionIO].currentZonedDateTime
        newRefreshTokenId <- ID.make[ConnectionIO, RefreshTokenId]
        newRefreshTokenStr = newRefreshTokenId.value.toString
        newRefreshTokenHash = hashToken(newRefreshTokenId.value.toString)

        newRefreshToken = uz
          .scala
          .repos
          .dto
          .RefreshToken(
            id = newRefreshTokenId,
            userId = user.id,
            tokenHash = newRefreshTokenHash,
            deviceInfo = deviceInfo.map(_.asJson),
            ipAddress = None,
            userAgent = None,
            expiresAt = now.plusDays(7),
            createdAt = now,
          )

        _ <- refreshTokensRepository.create(newRefreshToken)
        accessToken <- lifter(tokens.createAccessToken(user).map(_.value))
      } yield AuthTokens(
        accessToken = accessToken,
        refreshToken = newRefreshTokenStr,
        expiresIn = config.accessTokenExpiration.toSeconds,
      )).transact(xa)
    }
}
