package uz.scala.algebras

import cats.effect.MonadCancelThrow
import cats.implicits._
import doobie.syntax.connectionio._
import org.typelevel.log4cats.Logger
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.SCrypt

import uz.scala.Language
import uz.scala.domain.UserId
import uz.scala.domain.auth.AuthTokens
import uz.scala.domain.auth.RegisterInput
import uz.scala.domain.enums.UserStatus
import uz.scala.effects.Calendar
import uz.scala.effects.GenUUID
import uz.scala.exception.AError
import uz.scala.repos.UsersRepository
import uz.scala.repos.dto
import uz.scala.shared.ResponseMessages._
import uz.scala.syntax.refined._
import uz.scala.utils.ID

trait AuthAlgebra[F[_]] {
  def register(input: RegisterInput)(implicit lang: Language): F[AuthTokens]
  def createUserFromTelegram(telegramUser: dto.TelegramUser)(implicit lang: Language): F[UserId]
}

object AuthAlgebra {
  def make[F[_]: MonadCancelThrow: Calendar: GenUUID: Logger](
      usersRepository: UsersRepository[doobie.ConnectionIO],
      rolesRepository: uz.scala.repos.RolesRepository[doobie.ConnectionIO],
    )(implicit
      ev: PasswordHasher[F, SCrypt],
      xa: doobie.Transactor[F],
    ): AuthAlgebra[F] =
    new Impl[F](usersRepository, rolesRepository)

  private class Impl[F[_]: MonadCancelThrow: GenUUID: Calendar](
      usersRepository: UsersRepository[doobie.ConnectionIO],
      rolesRepository: uz.scala.repos.RolesRepository[doobie.ConnectionIO],
    )(implicit
      logger: Logger[F],
      ev: PasswordHasher[F, SCrypt],
      xa: doobie.Transactor[F],
    ) extends AuthAlgebra[F] {
    override def register(input: RegisterInput)(implicit lang: Language): F[AuthTokens] =
      for {
        _ <- logger.info(s"Registering user: ${input.email}")

        // Check email NOT exists
        _ <- usersRepository
          .find(input.email)
          .transact(xa)
          .flatMap {
            case Some(_) => AError.BadRequest(EMAIL_ALREADY_EXISTS(lang)).raiseError[F, Unit]
            case None => ().pure[F]
          }

        // Hash password
        hashedPassword <- SCrypt.hashpw[F](input.password.value)

        // Generate user ID
        userId <- ID.make[F, UserId]
        now <- Calendar[F].currentZonedDateTime

        // Get USER role ID
        userRole <- rolesRepository.getRoleByName("USER").transact(xa)

        // Create user
        user = dto.User(
          id = userId,
          createdAt = now,
          updatedAt = now,
          deletedAt = None,
          email = input.email,
          password = hashedPassword,
          firstName = input.firstName,
          lastName = input.lastName,
          phone = input.phone,
          roleId = userRole.id,
          status = UserStatus.Active, // For NestHub MVP, users are active immediately
          lastLoginAt = None,
        )

        _ <- usersRepository.create(user)(lang).transact(xa)
        _ <- logger.info(s"User ${input.email} created successfully")

        // TODO: Auto-login after registration - generate and return tokens
        // For now, return empty tokens - frontend should call /login after registration
        tokens = AuthTokens(accessToken = "", refreshToken = "")

      } yield tokens

    override def createUserFromTelegram(
        telegramUser: dto.TelegramUser
      )(implicit
        lang: Language
      ): F[UserId] =
      for {
        _ <- logger.info(s"Creating user from Telegram: ${telegramUser.telegramId}")

        // Generate user ID
        userId <- ID.make[F, UserId]
        now <- Calendar[F].currentZonedDateTime

        // Generate secure password hash
        generatedPassword = s"tg_${telegramUser.telegramId}_${System.currentTimeMillis()}"
        hashedPassword <- SCrypt.hashpw[F](generatedPassword)

        // Create valid phone number (+998 + 9 digits)
        phoneDigits = s"${telegramUser.telegramId}".take(9).padTo(9, '0')

        // Get USER role ID
        userRole <- rolesRepository.getRoleByName("USER").transact(xa)

        // Create user
        user = dto.User(
          id = userId,
          createdAt = now,
          updatedAt = now,
          deletedAt = None,
          email = s"tg_${telegramUser.telegramId}@nesthub.local",
          password = hashedPassword,
          firstName = telegramUser.firstName,
          lastName = "Telegram",
          phone = s"+998$phoneDigits",
          roleId = userRole.id,
          status = UserStatus.Active,
          lastLoginAt = Some(now),
        )

        _ <- usersRepository.create(user)(lang).transact(xa)
        _ <- logger.info(
          s"Telegram user created successfully: ${telegramUser.telegramId} -> $userId"
        )

      } yield userId
  }
}
