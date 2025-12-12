package uz.scala.repos.dto

import java.time.ZonedDateTime

import io.circe.Json

import uz.scala.domain.enums.BotState

case class TelegramSession(
    telegramId: Long,
    state: BotState,
    context: Option[Json],
    updatedAt: ZonedDateTime,
  )
