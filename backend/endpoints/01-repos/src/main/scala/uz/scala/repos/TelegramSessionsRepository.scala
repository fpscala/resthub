package uz.scala.repos

import cats.implicits._
import doobie.ConnectionIO
import doobie.implicits._
import io.circe.Json

import uz.scala.Language
import uz.scala.domain.enums.BotState
import uz.scala.repos.sql.TelegramSessionsSql

trait TelegramSessionsRepository[F[_]] {
  def findByTelegramId(telegramId: Long): F[Option[dto.TelegramSession]]
  def upsert(session: dto.TelegramSession)(implicit lang: Language): F[Unit]
  def updateState(telegramId: Long, state: BotState, context: Option[Json]): F[Unit]
}

object TelegramSessionsRepository {
  def make: TelegramSessionsRepository[ConnectionIO] = new TelegramSessionsRepository[ConnectionIO] {
    override def findByTelegramId(telegramId: Long): ConnectionIO[Option[dto.TelegramSession]] =
      TelegramSessionsSql.findByTelegramId(telegramId).option

    override def upsert(session: dto.TelegramSession)(implicit lang: Language): ConnectionIO[Unit] =
      TelegramSessionsSql.insert.run(session).void

    override def updateState(telegramId: Long, state: BotState, context: Option[Json]): ConnectionIO[Unit] =
      TelegramSessionsSql.updateState(telegramId, state, context).run.void
  }
}
