package uz.scala.repos

import java.util.UUID

import cats.implicits._
import doobie.ConnectionIO
import doobie.implicits._

import uz.scala.repos.sql.BrokerChannelsSql
import uz.scala.repos.sql.ListingChannelsSql

trait BrokerChannelsRepository[F[_]] {
  def findByTelegramUserId(telegramUserId: Long): F[List[dto.BrokerChannel]]
  def findPostableByTelegramUserId(telegramUserId: Long): F[List[dto.BrokerChannel]]
  def findByUserAndChat(telegramUserId: Long, telegramChatId: Long): F[Option[dto.BrokerChannel]]
  def findByChatId(telegramChatId: Long): F[List[dto.BrokerChannel]]
  def upsert(channel: dto.BrokerChannel): F[Unit]
  def updatePermissions(
      telegramUserId: Long,
      telegramChatId: Long,
      botIsAdmin: Boolean,
      userCanPost: Boolean,
    ): F[Unit]
  def deactivate(telegramUserId: Long, telegramChatId: Long): F[Unit]
  def deactivateAllForChat(telegramChatId: Long): F[Unit]
}

object BrokerChannelsRepository {
  def make: BrokerChannelsRepository[ConnectionIO] = new BrokerChannelsRepository[ConnectionIO] {
    override def findByTelegramUserId(telegramUserId: Long): ConnectionIO[List[dto.BrokerChannel]] =
      BrokerChannelsSql.findByTelegramUserId(telegramUserId).to[List]

    override def findPostableByTelegramUserId(telegramUserId: Long): ConnectionIO[List[dto.BrokerChannel]] =
      BrokerChannelsSql.findPostableByTelegramUserId(telegramUserId).to[List]

    override def findByUserAndChat(
        telegramUserId: Long,
        telegramChatId: Long,
      ): ConnectionIO[Option[dto.BrokerChannel]] =
      BrokerChannelsSql.findByUserAndChat(telegramUserId, telegramChatId).option

    override def findByChatId(telegramChatId: Long): ConnectionIO[List[dto.BrokerChannel]] =
      BrokerChannelsSql.findByChatId(telegramChatId).to[List]

    override def upsert(channel: dto.BrokerChannel): ConnectionIO[Unit] =
      BrokerChannelsSql.insert.run(channel).void

    override def updatePermissions(
        telegramUserId: Long,
        telegramChatId: Long,
        botIsAdmin: Boolean,
        userCanPost: Boolean,
      ): ConnectionIO[Unit] =
      BrokerChannelsSql.updatePermissions(telegramUserId, telegramChatId, botIsAdmin, userCanPost).run.void

    override def deactivate(telegramUserId: Long, telegramChatId: Long): ConnectionIO[Unit] =
      BrokerChannelsSql.deactivate(telegramUserId, telegramChatId).run.void

    override def deactivateAllForChat(telegramChatId: Long): ConnectionIO[Unit] =
      BrokerChannelsSql.deactivateAllForChat(telegramChatId).run.void
  }
}

trait ListingChannelsRepository[F[_]] {
  def findByListingId(listingId: UUID): F[List[dto.ListingChannel]]
  def upsert(channel: dto.ListingChannel): F[Unit]
}

object ListingChannelsRepository {
  def make: ListingChannelsRepository[ConnectionIO] = new ListingChannelsRepository[ConnectionIO] {
    override def findByListingId(listingId: UUID): ConnectionIO[List[dto.ListingChannel]] =
      ListingChannelsSql.findByListingId(listingId).to[List]

    override def upsert(channel: dto.ListingChannel): ConnectionIO[Unit] =
      ListingChannelsSql.insert.run(channel).void
  }
}
