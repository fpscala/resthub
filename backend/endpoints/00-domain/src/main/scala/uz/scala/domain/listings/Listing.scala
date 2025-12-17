package uz.scala.domain.listings

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import uz.scala.domain.ListingId
import uz.scala.domain.UserId
import uz.scala.domain.enums.ListingStatus
import uz.scala.domain.enums.ListingType
import uz.scala.syntax.circe._

@JsonCodec
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
  )
