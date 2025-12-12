package uz.scala.bot

import cats.effect.Async
import cats.implicits._
import org.typelevel.log4cats.Logger
import telegramium.bots._
import telegramium.bots.high.Api

import uz.scala.domain.listings.Listing

/**
 * NotificationService sends Telegram notifications when new listings are approved.
 *
 * USAGE:
 * 1. Integrate with Kafka consumer to listen to "listing.approved" topic
 * 2. Query telegram_subscriptions table for matching criteria
 * 3. Send notifications to matched users
 *
 * NOTE: This is a skeleton - full Kafka integration requires:
 * - fs2-kafka consumer setup
 * - Redis caching to avoid duplicate notifications
 * - Error handling and retry logic
 */
trait NotificationService[F[_]] {
  def notifyNewListing(listing: Listing): F[Unit]
}

object NotificationService {
  def make[F[_]: Async: Logger](
      api: Api[F]
      // Add: subscriptionsRepo, redis, etc.
    ): NotificationService[F] =
    new Impl[F](api)

  private class Impl[F[_]: Async: Logger](
      api: Api[F]
    ) extends NotificationService[F] {

    override def notifyNewListing(listing: Listing): F[Unit] = {
      for {
        _ <- Logger[F].info(s"New listing approved: ${listing.id}, notifying subscribers...")

        // TODO: Query telegram_subscriptions for matching criteria
        // val subscribers = subscriptionsRepo.findMatching(listing.city, listing.price)

        // TODO: Filter already notified using Redis
        // val cacheKey = s"notified:${listing.id}"
        // val alreadyNotified = redis.get[Set[Long]](cacheKey)

        // TODO: Send notifications
        // subscribers.traverse { sub =>
        //   sendNotification(sub.telegramId, listing)
        // }

        // For now, just log
        _ <- Logger[F].info(s"Notification service called for listing: ${listing.id}")

      } yield ()
    }

    private def sendNotification(telegramId: Long, listing: Listing): F[Unit] = {
      val message = s"""🏠 Yangi e'lon!

📍 Shahar: ${listing.city.value}
💰 Narx: ${listing.price} UZS/oy
📝 ${listing.title.value}

${listing.description.value.take(150)}...

Batafsil: /view_${listing.id.value}
"""

      api.execute(SendMessage(
        ChatIntId(telegramId),
        message
      )).void.handleErrorWith { err =>
        Logger[F].error(err)(s"Failed to send notification to $telegramId")
      }
    }
  }
}
