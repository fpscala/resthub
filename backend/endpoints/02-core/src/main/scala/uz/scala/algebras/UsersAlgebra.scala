package uz.scala.algebras

import cats.effect.MonadCancelThrow
import cats.effect.std.Random
import cats.implicits._
import doobie.syntax.connectionio._
import org.typelevel.log4cats.Logger
import tsec.common.VerificationFailed
import tsec.common.Verified
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.SCrypt

import uz.scala.Language
import uz.scala.domain.UserId
import uz.scala.domain.users._
import uz.scala.effects.Calendar
import uz.scala.effects.GenUUID
import uz.scala.exception.AError
import uz.scala.repos.RolesRepository
import uz.scala.repos.UsersRepository
import uz.scala.repos.dto
import uz.scala.shared.EmailAddress
import uz.scala.shared.ResponseMessages.USER_NOT_FOUND
import uz.scala.shared.ResponseMessages.WRONG_PASSWORD
import uz.scala.syntax.all.optionSyntaxFunctorOptionOps

trait UsersAlgebra[F[_]] {
  def find(email: EmailAddress): F[Option[dto.User]]
  def delete(id: UserId): F[Unit]
  def updatePassword(
      id: UserId,
      input: UpdatePasswordInput,
    )(implicit
      language: Language
    ): F[Unit]
}

object UsersAlgebra {
  def make[F[_]: MonadCancelThrow: Calendar: GenUUID: Logger: Random](
      usersRepository: UsersRepository[doobie.ConnectionIO],
      rolesRepository: RolesRepository[doobie.ConnectionIO],
    )(implicit
      ev: PasswordHasher[F, SCrypt],
      xa: doobie.Transactor[F],
    ): UsersAlgebra[F] =
    new Impl[F](usersRepository, rolesRepository)

  private class Impl[F[_]: MonadCancelThrow: GenUUID: Calendar: Random](
      usersRepository: UsersRepository[doobie.ConnectionIO],
      rolesRepository: RolesRepository[doobie.ConnectionIO],
    )(implicit
      logger: Logger[F],
      ev: PasswordHasher[F, SCrypt],
      xa: doobie.Transactor[F],
    ) extends UsersAlgebra[F] {
    override def find(email: EmailAddress): F[Option[dto.User]] =
      usersRepository.find(email).transact(xa)

    override def delete(id: UserId): F[Unit] =
      usersRepository.delete(id).transact(xa)

    override def updatePassword(
        id: UserId,
        input: UpdatePasswordInput,
      )(implicit
        language: Language
      ): F[Unit] =
      for {
        _ <- logger.info(s"Updating user password.. $input")
        user <- usersRepository
          .findById(id)
          .transact(xa)
          .getOrRaise(AError.BadRequest(USER_NOT_FOUND(language)))
        passwordHash <- SCrypt.hashpw[F](input.newPassword.value)

        _ <- SCrypt
          .checkpw[F](input.currentPassword.value, user.password)
          .flatMap {
            case VerificationFailed =>
              AError
                .NotAllowed(WRONG_PASSWORD(language))
                .raiseError[F, Unit]
            case Verified =>
              usersRepository
                .update(id)(_.copy(password = passwordHash))
                .transact(xa)
          }
      } yield {}
  }
}
