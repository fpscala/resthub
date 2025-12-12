package uz.scala.repos.dto

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString

import uz.scala.domain.ListingId
import uz.scala.domain.UserId
import uz.scala.domain.enums.ListingStatus

case class Listing(
    id: ListingId,
    ownerId: UserId,
    title: NonEmptyString,
    description: NonEmptyString,
    price: BigDecimal,
    city: NonEmptyString,
    images: List[String],
    status: ListingStatus,
    rejectionReason: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
    approvedAt: Option[ZonedDateTime],
    approvedBy: Option[UserId],
  )
