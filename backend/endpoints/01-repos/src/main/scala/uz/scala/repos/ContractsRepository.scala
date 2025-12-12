package uz.scala.repos

import cats.implicits._
import doobie.ConnectionIO
import doobie.implicits._

import uz.scala.Language
import uz.scala.domain.ContractId
import uz.scala.domain.ListingId
import uz.scala.repos.sql.ContractsSql

trait ContractsRepository[F[_]] {
  def create(contract: dto.Contract)(implicit lang: Language): F[Unit]
  def findById(id: ContractId): F[Option[dto.Contract]]
  def findByListingId(listingId: ListingId): F[List[dto.Contract]]
}

object ContractsRepository {
  def make: ContractsRepository[ConnectionIO] = new ContractsRepository[ConnectionIO] {
    override def create(contract: dto.Contract)(implicit lang: Language): ConnectionIO[Unit] =
      ContractsSql.insert.run(contract).void

    override def findById(id: ContractId): ConnectionIO[Option[dto.Contract]] =
      ContractsSql.findById(id).option

    override def findByListingId(listingId: ListingId): ConnectionIO[List[dto.Contract]] =
      ContractsSql.findByListingId(listingId).to[List]
  }
}
