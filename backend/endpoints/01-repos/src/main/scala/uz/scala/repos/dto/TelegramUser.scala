package uz.scala.repos.dto

import java.time.ZonedDateTime

import uz.scala.Language
import uz.scala.domain.UserId

case class TelegramUser(
    telegramId: Long,
    userId: Option[UserId],
    username: Option[String],
    firstName: String,
    languageCode: Language,
    phoneNumber: Option[String],
    isRegistered: Boolean,
    createdAt: ZonedDateTime,
    lastInteractionAt: ZonedDateTime,
  )
