package uz.scala.domain.listings

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import squants.market.Money

import uz.scala.syntax.circe._

@JsonCodec
case class CreateListingInput(
    title: NonEmptyString,
    description: NonEmptyString,
    price: Money,
    city: NonEmptyString,
    images: List[String],
  )
