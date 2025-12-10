package uz.scala.repos

import cats.data.OptionT
import cats.implicits.catsSyntaxApplicativeErrorId
import cats.implicits.toFunctorOps
import doobie.ConnectionIO
import doobie.implicits.toDoobieApplicativeErrorOps
import doobie.postgres.sqlstate

import uz.scala.Language
import uz.scala.domain.UserId
import uz.scala.exception.AError
import uz.scala.repos.sql.UsersSql
import uz.scala.shared.EmailAddress
import uz.scala.shared.ResponseMessages.PHONE_Y_EXISTS
import uz.scala.shared.ResponseMessages.USER_NOT_FOUND

trait UsersRepository[F[_]] {
  def find(email: EmailAddress): F[Option[dto.User]]
  def findById(id: UserId): F[Option[dto.User]]
  def create(user: dto.User)(implicit language: Language): ConnectionIO[Unit]
  def update(id: UserId)(update: dto.User => dto.User)(implicit language: Language): F[Unit]
  def delete(id: UserId): F[Unit]
  def updateLastLogin(id: UserId): F[Unit]
}

object UsersRepository {
  def make: UsersRepository[ConnectionIO] = new UsersRepository[ConnectionIO] {
    override def find(email: EmailAddress): ConnectionIO[Option[dto.User]] =
      UsersSql.findByEmail(email).option

    override def create(user: dto.User)(implicit language: Language): ConnectionIO[Unit] =
      UsersSql
        .insert
        .run(user)
        .void
        .exceptSomeSqlState {
          case sqlstate.class23.UNIQUE_VIOLATION =>
            AError.BadRequest(PHONE_Y_EXISTS(language)).raiseError[ConnectionIO, Unit]
        }

    override def findById(id: UserId): ConnectionIO[Option[dto.User]] =
      UsersSql.findById(id).option

    override def update(
        id: UserId
      )(
        update: dto.User => dto.User
      )(implicit
        language: Language
      ): ConnectionIO[Unit] =
      OptionT(findById(id)).cataF(
        AError.BadRequest(USER_NOT_FOUND(language)).raiseError[ConnectionIO, Unit],
        user => UsersSql.update(update(user)).run.void,
      )

    override def delete(id: UserId): ConnectionIO[Unit] =
      UsersSql.delete(id).run.void

    override def updateLastLogin(id: UserId): ConnectionIO[Unit] =
      UsersSql.updateLastLogin(id).run.void
  }
}
