package uz.scala.domain.telegram

import java.time.ZonedDateTime

import io.circe.Json
import io.circe.generic.JsonCodec

import uz.scala.domain.enums.BotState
import uz.scala.syntax.circe._

@JsonCodec
case class BotSession(
    telegramId: Long,
    state: BotState,
    context: Option[Json],
    updatedAt: ZonedDateTime,
  )
