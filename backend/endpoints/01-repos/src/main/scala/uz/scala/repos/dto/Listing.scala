package uz.scala.repos.dto

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import squants.market.Money
import io.scalaland.chimney.dsl._

import uz.scala.domain.ListingId
import uz.scala.domain.UserId
import uz.scala.domain.enums.ListingStatus
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
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
    approvedAt: Option[ZonedDateTime],
    approvedBy: Option[UserId],
  ) {
  def toDomain(owner: uz.scala.domain.users.User): ListingOutput =
    this
      .into[ListingOutput]
      .withFieldConst(_.owner, owner)
      .withFieldComputed(_.price, _.price)
      .transform
}

object Listing {
  def fromDomain(domain: ListingOutput): Listing =
    domain
      .into[Listing]
      .withFieldComputed(_.ownerId, _.owner.id)
      .withFieldComputed(_.price, _.price)
      .withFieldComputed(_.rejectionReason, _ => None)
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.approvedAt, _ => None)
      .withFieldComputed(_.approvedBy, _ => None)
      .transform
}
