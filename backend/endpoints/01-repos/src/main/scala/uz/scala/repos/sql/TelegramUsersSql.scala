package uz.scala.repos.sql

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import uz.scala.Language
import uz.scala.doobie.Sql
import uz.scala.doobie.syntax.all._
import uz.scala.repos.dto

private[repos] object TelegramUsersSql extends Sql[dto.TelegramUser] {
  def findByTelegramId(telegramId: Long): Query0[dto.TelegramUser] =
    sql"""SELECT $columns FROM $table WHERE telegram_id = $telegramId LIMIT 1"""
      .query[dto.TelegramUser]

  val insert: Update[dto.TelegramUser] = Update[dto.TelegramUser](
    sql"""INSERT INTO $table ($columns) VALUES ($values)""".internals.sql
  )

  def updateLastInteraction(telegramId: Long): Update0 =
    sql"""UPDATE $table SET last_interaction_at = NOW() WHERE telegram_id = $telegramId""".update

  def updateUserId(telegramId: Long, userId: uz.scala.domain.UserId): Update0 =
    sql"""UPDATE $table SET user_id = $userId WHERE telegram_id = $telegramId""".update

  def updatePhoneNumber(telegramId: Long, phoneNumber: String): Update0 =
    sql"""UPDATE $table SET phone_number = $phoneNumber WHERE telegram_id = $telegramId""".update

  def updateLanguage(telegramId: Long, language: Language): Update0 = {
    val langCode = language.entryName.toLowerCase
    sql"""UPDATE $table SET language_code = $langCode::language WHERE telegram_id = $telegramId""".update
  }
}
