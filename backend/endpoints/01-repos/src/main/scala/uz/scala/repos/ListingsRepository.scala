package uz.scala.repos

import cats.implicits._
import doobie.ConnectionIO
import doobie.implicits._

import uz.scala.Language
import uz.scala.domain.ListingId
import uz.scala.domain.UserId
import uz.scala.domain.enums.ListingStatus
import uz.scala.domain.listings.ListingFilters
import uz.scala.repos.sql.ListingsSql

trait ListingsRepository[F[_]] {
  def create(listing: dto.Listing)(implicit lang: Language): F[Unit]
  def findById(id: ListingId): F[Option[dto.Listing]]
  def findByFilters(filters: ListingFilters): F[List[dto.Listing]]
  def count(filters: ListingFilters): F[Long]
  def updateStatus(
      id: ListingId,
      status: ListingStatus,
      approvedBy: Option[UserId],
    )(implicit
      lang: Language
    ): F[Unit]
  def delete(id: ListingId): F[Unit]
}

object ListingsRepository {
  def make: ListingsRepository[ConnectionIO] = new ListingsRepository[ConnectionIO] {
    override def create(listing: dto.Listing)(implicit lang: Language): ConnectionIO[Unit] =
      ListingsSql.insert.run(listing).void

    override def findById(id: ListingId): ConnectionIO[Option[dto.Listing]] =
      ListingsSql.findById(id).option

    override def findByFilters(filters: ListingFilters): ConnectionIO[List[dto.Listing]] =
      ListingsSql.findByFilters(filters).to[List]

    override def count(filters: ListingFilters): ConnectionIO[Long] =
      ListingsSql.count(filters).unique

    override def updateStatus(
        id: ListingId,
        status: ListingStatus,
        approvedBy: Option[UserId],
      )(implicit
        lang: Language
      ): ConnectionIO[Unit] =
      ListingsSql.updateStatus(id, status, approvedBy).run.void

    override def delete(id: ListingId): ConnectionIO[Unit] =
      ListingsSql.delete(id).run.void
  }
}
