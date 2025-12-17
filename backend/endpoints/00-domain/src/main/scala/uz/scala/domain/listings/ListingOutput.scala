package uz.scala.domain.listings

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import squants.Money

import uz.scala.domain.ListingId
import uz.scala.domain.enums.ListingStatus
import uz.scala.domain.enums.ListingType
import uz.scala.domain.users.User
import uz.scala.syntax.circe._

@JsonCodec
case class ListingOutput(
    id: ListingId,
    owner: User,
    title: NonEmptyString,
    description: NonEmptyString,
    price: Money,
    city: NonEmptyString,
    listingType: ListingType,
    rooms: Option[Int],
    district: Option[String],
    floor: Option[Int],
    totalFloors: Option[Int],
    buildingType: Option[String],
    condition: Option[String],
    images: List[String],
    status: ListingStatus,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  )
