package uz.scala.domain.telegram

import java.time.ZonedDateTime

import io.circe.generic.JsonCodec

import uz.scala.domain.UserId
import uz.scala.syntax.circe._

@JsonCodec
case class BotUser(
    telegramId: Long,
    userId: Option[UserId],
    username: Option[String],
    firstName: String,
    languageCode: String,
    isRegistered: Boolean,
    createdAt: ZonedDateTime,
    lastInteractionAt: ZonedDateTime,
  )
