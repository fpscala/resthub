package uz.scala.repos

import cats.data.OptionT
import cats.implicits._
import doobie.ConnectionIO
import doobie.implicits._
import uz.scala.Language
import uz.scala.domain.CityId
import uz.scala.exception.AError
import uz.scala.repos.dto.City
import uz.scala.repos.sql.CitiesSql
import uz.scala.shared.ResponseMessages.CITY_NOT_FOUND

trait CitiesRepository[F[_]] {
  def getAll: F[List[City]]
  def create(city: City): F[Unit]
  def findById(id: CityId): F[Option[City]]
  def update(
      id: CityId
    )(
      update: dto.City => dto.City
    )(implicit
      language: Language
    ): F[Unit]
  def delete(id: CityId): F[Unit]
}

object CitiesRepository {
  def make: CitiesRepository[ConnectionIO] = new CitiesRepository[ConnectionIO] {
    override def getAll: ConnectionIO[List[City]] =
      CitiesSql.findAll.to[List]

    override def create(city: City): ConnectionIO[Unit] =
      CitiesSql.insert.run(city).void

    override def update(
        id: CityId
      )(
        update: dto.City => dto.City
      )(implicit
        language: Language
      ): ConnectionIO[Unit] =
      OptionT(findById(id)).cataF(
        AError.BadRequest(CITY_NOT_FOUND(language)).raiseError[ConnectionIO, Unit],
        city => CitiesSql.update(update(city)).run.void,
      )

    override def findById(id: CityId): ConnectionIO[Option[City]] =
      CitiesSql.findById(id).option

    override def delete(id: CityId): ConnectionIO[Unit] =
      CitiesSql.delete(id).run.void
  }
}
