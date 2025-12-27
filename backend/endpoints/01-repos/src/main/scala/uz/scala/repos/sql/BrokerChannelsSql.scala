package uz.scala.repos.sql

import java.util.UUID

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import uz.scala.doobie.Sql
import uz.scala.doobie.syntax.all._
import uz.scala.domain.enums.ChatType
import uz.scala.repos.dto

private[repos] object BrokerChannelsSql extends Sql[dto.BrokerChannel] {
  def findByTelegramUserId(telegramUserId: Long): Query0[dto.BrokerChannel] =
    sql"""SELECT $columns FROM $table WHERE telegram_user_id = $telegramUserId AND is_active = TRUE"""
      .query[dto.BrokerChannel]

  def findPostableByTelegramUserId(telegramUserId: Long): Query0[dto.BrokerChannel] =
    sql"""SELECT $columns FROM $table
          WHERE telegram_user_id = $telegramUserId
          AND is_active = TRUE
          AND user_can_post = TRUE
          AND bot_is_admin = TRUE"""
      .query[dto.BrokerChannel]

  def findByUserAndChat(telegramUserId: Long, telegramChatId: Long): Query0[dto.BrokerChannel] =
    sql"""SELECT $columns FROM $table
          WHERE telegram_user_id = $telegramUserId
          AND telegram_chat_id = $telegramChatId
          LIMIT 1"""
      .query[dto.BrokerChannel]

  def findByChatId(telegramChatId: Long): Query0[dto.BrokerChannel] =
    sql"""SELECT $columns FROM $table WHERE telegram_chat_id = $telegramChatId"""
      .query[dto.BrokerChannel]

  val insert: Update[dto.BrokerChannel] = Update[dto.BrokerChannel](
    sql"""INSERT INTO $table ($columns) VALUES ($values)
          ON CONFLICT (telegram_user_id, telegram_chat_id) DO UPDATE SET
            chat_title = EXCLUDED.chat_title,
            chat_type = EXCLUDED.chat_type,
            chat_username = EXCLUDED.chat_username,
            bot_is_admin = EXCLUDED.bot_is_admin,
            user_can_post = EXCLUDED.user_can_post,
            is_active = EXCLUDED.is_active,
            last_verified_at = EXCLUDED.last_verified_at,
            updated_at = NOW()""".internals.sql
  )

  def updatePermissions(
      telegramUserId: Long,
      telegramChatId: Long,
      botIsAdmin: Boolean,
      userCanPost: Boolean,
    ): Update0 =
    sql"""UPDATE $table SET
            bot_is_admin = $botIsAdmin,
            user_can_post = $userCanPost,
            last_verified_at = NOW(),
            updated_at = NOW()
          WHERE telegram_user_id = $telegramUserId
          AND telegram_chat_id = $telegramChatId""".update

  def deactivate(telegramUserId: Long, telegramChatId: Long): Update0 =
    sql"""UPDATE $table SET is_active = FALSE, updated_at = NOW()
          WHERE telegram_user_id = $telegramUserId
          AND telegram_chat_id = $telegramChatId""".update

  def deactivateAllForChat(telegramChatId: Long): Update0 =
    sql"""UPDATE $table SET is_active = FALSE, updated_at = NOW()
          WHERE telegram_chat_id = $telegramChatId""".update
}

private[repos] object ListingChannelsSql extends Sql[dto.ListingChannel] {
  def findByListingId(listingId: UUID): Query0[dto.ListingChannel] =
    sql"""SELECT $columns FROM $table WHERE listing_id = $listingId"""
      .query[dto.ListingChannel]

  val insert: Update[dto.ListingChannel] = Update[dto.ListingChannel](
    sql"""INSERT INTO $table ($columns) VALUES ($values)
          ON CONFLICT (listing_id, telegram_chat_id) DO UPDATE SET
            telegram_message_id = EXCLUDED.telegram_message_id,
            posted_at = EXCLUDED.posted_at""".internals.sql
  )
}
