package uz.scala.domain.listings

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import uz.scala.domain.ListingId
import uz.scala.domain.enums.ListingStatus
import uz.scala.domain.users.User
import uz.scala.syntax.circe._

@JsonCodec
case class ListingOutput(
    id: ListingId,
    owner: User,
    title: NonEmptyString,
    description: NonEmptyString,
    price: BigDecimal,
    city: NonEmptyString,
    images: List[String],
    status: ListingStatus,
    createdAt: ZonedDateTime,
  )
