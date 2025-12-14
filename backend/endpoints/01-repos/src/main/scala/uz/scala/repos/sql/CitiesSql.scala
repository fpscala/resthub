package uz.scala.repos.sql

import doobie._
import doobie.implicits._

import uz.scala.domain.CityId
import uz.scala.doobie.Sql
import uz.scala.doobie.syntax.all._
import uz.scala.repos.dto.City

object CitiesSql extends Sql[City] {
  def findAll: Query0[City] =
    sql"""SELECT $columns FROM $table WHERE is_active = true ORDER BY name ASC""".query[City]

  val insert: Update[City] = Update[City](
    sql"""INSERT INTO $table ($columns) VALUES ($values)""".internals.sql
  )

  def findById(id: CityId): Query0[City] =
    sql"""SELECT $columns FROM $table WHERE id = $id LIMIT 1"""
      .query[City]

  def update(city: City): Update0 =
    sql"""
      UPDATE $table
      SET name = ${city.name}, is_active = ${city.isActive}
      WHERE id = ${city.id}
    """.update

  def delete(id: CityId): Update0 =
    sql"""
      UPDATE $table
      SET is_active = false
      WHERE id = $id
    """.update
}
