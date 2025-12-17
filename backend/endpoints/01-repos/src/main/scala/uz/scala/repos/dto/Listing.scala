package uz.scala.repos.dto

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.scalaland.chimney.dsl._
import squants.market.Money

import uz.scala.domain.ListingId
import uz.scala.domain.UserId
import uz.scala.domain.enums.ListingStatus
import uz.scala.domain.enums.ListingType
import uz.scala.domain.listings.ListingOutput

case class Listing(
    id: ListingId,
    ownerId: UserId,
    title: NonEmptyString,
    description: NonEmptyString,
    price: Money,
    city: NonEmptyString,
    images: List[String],
    status: ListingStatus,
    rejectionReason: Option[String],
    listingType: ListingType,
    rooms: Option[Int],
    district: Option[String],
    floor: Option[Int],
    totalFloors: Option[Int],
    buildingType: Option[String],
    condition: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
    approvedAt: Option[ZonedDateTime],
    approvedBy: Option[UserId],
  ) {
  def toDomain(owner: uz.scala.domain.users.User): ListingOutput =
    this
      .into[ListingOutput]
      .withFieldConst(_.owner, owner)
      .transform
}

object Listing {
  def fromDomain(domain: ListingOutput): Listing =
    domain
      .into[Listing]
      .withFieldComputed(_.ownerId, _.owner.id)
      .withFieldComputed(_.price, _.price)
      .withFieldComputed(_.rejectionReason, _ => None)
      .withFieldConst(_.approvedBy, None)
      .withFieldConst(_.approvedAt, None)
      .transform
}
