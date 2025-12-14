package uz.scala.algebras

import cats.effect.MonadCancelThrow
import doobie.ConnectionIO
import doobie.syntax.connectionio._

import uz.scala.Language
import uz.scala.domain.CityId
import uz.scala.domain.cities._
import uz.scala.effects.Calendar
import uz.scala.effects.GenUUID
import uz.scala.repos.CitiesRepository
import uz.scala.repos.dto
import uz.scala.utils.ID

trait CitiesAlgebra[F[_]] {
  def getAll: F[List[City]]
  def create(city: CityInput)(implicit language: Language): F[CityId]
  def update(id: CityId, city: CityInput)(implicit language: Language): F[Unit]
  def delete(id: CityId)(implicit language: Language): F[Unit]
}

object CitiesAlgebra {
  def make[F[_]: MonadCancelThrow: Calendar: GenUUID](
      citiesRepository: CitiesRepository[ConnectionIO]
    )(implicit
      xa: doobie.Transactor[F]
    ): CitiesAlgebra[F] =
    new CitiesAlgebra[F] {
      override def getAll: F[List[City]] =
        citiesRepository
          .getAll
          .map(_.map(_.toDomain))
          .transact(xa)

      override def create(city: CityInput)(implicit language: Language): F[CityId] =
        (for {
          cityId <- ID.make[ConnectionIO, CityId]
          newCity = dto.City(cityId, city.name, city.isActive)
          _ <- citiesRepository.create(newCity)
        } yield cityId).transact(xa)

      override def update(id: CityId, city: CityInput)(implicit language: Language): F[Unit] =
        citiesRepository
          .update(id)(c => c.copy(name = city.name, isActive = city.isActive))
          .transact(xa)

      override def delete(id: CityId)(implicit language: Language): F[Unit] =
        citiesRepository.delete(id).transact(xa)
    }
}
