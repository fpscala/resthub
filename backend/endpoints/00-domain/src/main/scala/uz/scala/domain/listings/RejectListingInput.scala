package uz.scala.domain.listings

import io.circe.generic.JsonCodec
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined._

@JsonCodec
case class RejectListingInput(
    reason: NonEmptyString
  )
