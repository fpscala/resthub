package uz.scala.repos.dto

import java.time.ZonedDateTime

import uz.scala.domain.UserId

case class TelegramUser(
    telegramId: Long,
    userId: Option[UserId],
    username: Option[String],
    firstName: String,
    languageCode: String,
    isRegistered: Boolean,
    createdAt: ZonedDateTime,
    lastInteractionAt: ZonedDateTime,
  )
