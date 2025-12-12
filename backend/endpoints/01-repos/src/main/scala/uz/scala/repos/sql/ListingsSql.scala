package uz.scala.repos.sql

import doobie._
import doobie.implicits._
import doobie.refined.implicits._

import uz.scala.domain.ListingId
import uz.scala.domain.UserId
import uz.scala.domain.enums.ListingStatus
import uz.scala.domain.listings.ListingFilters
import uz.scala.doobie.Sql
import uz.scala.doobie.syntax.all._
import uz.scala.repos.dto

private[repos] object ListingsSql extends Sql[dto.Listing] {
  def findById(id: ListingId): Query0[dto.Listing] =
    sql"""SELECT $columns FROM $table WHERE id = $id LIMIT 1"""
      .query[dto.Listing]

  def findByFilters(filters: ListingFilters): Query0[dto.Listing] = {
    val cityFilter = filters.city.map(city => fr"city = $city")
    val minPriceFilter = filters.minPrice.map(minPrice => fr"price >= $minPrice")
    val maxPriceFilter = filters.maxPrice.map(maxPrice => fr"price <= $maxPrice")
    val statusFilter = filters.status.map(status => fr"status = $status")

    val whereClause = Fragments.whereAndOpt(cityFilter, minPriceFilter, maxPriceFilter, statusFilter)

    val limitClause = filters.size.getOrElse(20)
    val offsetClause = filters.page.map(page => (page - 1) * limitClause).getOrElse(0)

    (sql"""SELECT $columns FROM $table""" ++ whereClause ++ fr"ORDER BY created_at DESC LIMIT $limitClause OFFSET $offsetClause")
      .query[dto.Listing]
  }

  def count(filters: ListingFilters): Query0[Long] = {
    val cityFilter = filters.city.map(city => fr"city = $city")
    val minPriceFilter = filters.minPrice.map(minPrice => fr"price >= $minPrice")
    val maxPriceFilter = filters.maxPrice.map(maxPrice => fr"price <= $maxPrice")
    val statusFilter = filters.status.map(status => fr"status = $status")

    val whereClause = Fragments.whereAndOpt(cityFilter, minPriceFilter, maxPriceFilter, statusFilter)

    (sql"""SELECT COUNT(*) FROM $table""" ++ whereClause).query[Long]
  }

  val insert: Update[dto.Listing] = Update[dto.Listing](
    sql"""INSERT INTO $table ($columns) VALUES ($values)""".internals.sql
  )

  def updateStatus(id: ListingId, status: ListingStatus, approvedBy: Option[UserId]): Update0 = {
    val approvedAt = if (status == ListingStatus.Approved) Some(fr"NOW()") else None
    val approvedByFrag = approvedBy.map(userId => fr", approved_by = $userId").getOrElse(Fragment.empty)
    val approvedAtFrag = approvedAt.map(_ => fr", approved_at = NOW()").getOrElse(Fragment.empty)

    (sql"""UPDATE $table
           SET status = $status""" ++ approvedByFrag ++ approvedAtFrag ++ fr""", updated_at = NOW()
           WHERE id = $id""").update
  }

  def delete(id: ListingId): Update0 =
    sql"""DELETE FROM $table WHERE id = $id""".update
}
