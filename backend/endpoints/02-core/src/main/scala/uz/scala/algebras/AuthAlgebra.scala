package uz.scala.algebras

import cats.effect.MonadCancelThrow
import cats.implicits._
import doobie.syntax.connectionio._
import org.typelevel.log4cats.Logger
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.SCrypt
import uz.scala.Language
import uz.scala.domain.UserId
import uz.scala.domain.auth.RegisterInput
import uz.scala.domain.auth.AuthTokens
import uz.scala.domain.enums.UserStatus
import uz.scala.domain.users.Role
import uz.scala.effects.Calendar
import uz.scala.effects.GenUUID
import uz.scala.exception.AError
import uz.scala.repos.UsersRepository
import uz.scala.repos.dto
import uz.scala.shared.ResponseMessages._
import uz.scala.utils.ID

trait AuthAlgebra[F[_]] {
  def register(input: RegisterInput)(implicit lang: Language): F[AuthTokens]
}

object AuthAlgebra {
  def make[F[_]: MonadCancelThrow: Calendar: GenUUID: Logger](
      usersRepository: UsersRepository[doobie.ConnectionIO],
    )(implicit
      ev: PasswordHasher[F, SCrypt],
      xa: doobie.Transactor[F],
    ): AuthAlgebra[F] =
    new Impl[F](usersRepository)

  private class Impl[F[_]: MonadCancelThrow: GenUUID: Calendar](
      usersRepository: UsersRepository[doobie.ConnectionIO],
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
          roleId = Role.USER,
          status = UserStatus.Active, // For NestHub MVP, users are active immediately
          lastLoginAt = None,
        )

        _ <- usersRepository.create(user)(lang).transact(xa)
        _ <- logger.info(s"User ${input.email} created successfully")

        // TODO: Auto-login after registration - generate and return tokens
        // For now, return empty tokens - frontend should call /login after registration
        tokens = AuthTokens(accessToken = "", refreshToken = "")

      } yield tokens
  }
}
