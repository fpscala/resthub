package uz.scala.repos

import cats.implicits._
import doobie.ConnectionIO
import doobie.implicits._

import uz.scala.Language
import uz.scala.repos.sql.TelegramUsersSql

trait TelegramUsersRepository[F[_]] {
  def findByTelegramId(telegramId: Long): F[Option[dto.TelegramUser]]
  def create(telegramUser: dto.TelegramUser)(implicit lang: Language): F[Unit]
  def updateLastInteraction(telegramId: Long): F[Unit]
}

object TelegramUsersRepository {
  def make: TelegramUsersRepository[ConnectionIO] = new TelegramUsersRepository[ConnectionIO] {
    override def findByTelegramId(telegramId: Long): ConnectionIO[Option[dto.TelegramUser]] =
      TelegramUsersSql.findByTelegramId(telegramId).option

    override def create(telegramUser: dto.TelegramUser)(implicit lang: Language): ConnectionIO[Unit] =
      TelegramUsersSql.insert.run(telegramUser).void

    override def updateLastInteraction(telegramId: Long): ConnectionIO[Unit] =
      TelegramUsersSql.updateLastInteraction(telegramId).run.void
  }
}
