package uz.scala.repos.dto

import java.time.ZonedDateTime
import java.util.UUID

import uz.scala.domain.enums.ChatType

case class BrokerChannel(
    id: UUID,
    telegramUserId: Long,
    telegramChatId: Long,
    chatTitle: String,
    chatType: ChatType,
    chatUsername: Option[String],
    botIsAdmin: Boolean,
    userCanPost: Boolean,
    isActive: Boolean,
    discoveredAt: ZonedDateTime,
    lastVerifiedAt: ZonedDateTime,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  )

case class ListingChannel(
    id: UUID,
    listingId: UUID,
    telegramChatId: Long,
    telegramMessageId: Option[Long],
    postedAt: ZonedDateTime,
  )
