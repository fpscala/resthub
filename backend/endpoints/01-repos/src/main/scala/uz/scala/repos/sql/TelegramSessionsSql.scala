package uz.scala.repos.sql

import doobie._
import doobie.implicits._
import doobie.postgres.circe.jsonb.implicits._
import io.circe.Json

import uz.scala.domain.enums.BotState
import uz.scala.doobie.Sql
import uz.scala.doobie.syntax.all._
import uz.scala.repos.dto

private[repos] object TelegramSessionsSql extends Sql[dto.TelegramSession] {
  def findByTelegramId(telegramId: Long): Query0[dto.TelegramSession] =
    sql"""SELECT $columns FROM $table WHERE telegram_id = $telegramId LIMIT 1"""
      .query[dto.TelegramSession]

  val insert: Update[dto.TelegramSession] = Update[dto.TelegramSession](
    sql"""INSERT INTO $table ($columns) VALUES ($values)
          ON CONFLICT (telegram_id) DO UPDATE
          SET state = EXCLUDED.state,
              context = EXCLUDED.context,
              updated_at = EXCLUDED.updated_at""".internals.sql
  )

  def updateState(
      telegramId: Long,
      state: BotState,
      context: Option[Json],
    ): Update0 =
    sql"""UPDATE $table
          SET state = $state, context = $context, updated_at = NOW()
          WHERE telegram_id = $telegramId""".update

  def update(session: dto.TelegramSession): Update0 =
    sql"""UPDATE $table
          SET state = ${session.state},
           context = ${session.context},
           updated_at = ${session.updatedAt}
          WHERE telegram_id = ${session.telegramId}""".update
}
