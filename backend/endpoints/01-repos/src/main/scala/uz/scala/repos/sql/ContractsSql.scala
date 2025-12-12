package uz.scala.repos.sql

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import uz.scala.domain.ContractId
import uz.scala.domain.ListingId
import uz.scala.doobie.Sql
import uz.scala.doobie.syntax.all._
import uz.scala.repos.dto

private[repos] object ContractsSql extends Sql[dto.Contract] {
  def findById(id: ContractId): Query0[dto.Contract] =
    sql"""SELECT $columns FROM $table WHERE id = $id LIMIT 1"""
      .query[dto.Contract]

  def findByListingId(listingId: ListingId): Query0[dto.Contract] =
    sql"""SELECT $columns FROM $table WHERE listing_id = $listingId"""
      .query[dto.Contract]

  val insert: Update[dto.Contract] = Update[dto.Contract](
    sql"""INSERT INTO $table ($columns) VALUES ($values)""".internals.sql
  )
}
