package uz.scala.domain.listings

import io.circe.generic.JsonCodec
import uz.scala.syntax.circe._

import uz.scala.domain.ListingId

@JsonCodec
case class GenerateContractInput(
    listingId: ListingId
  )
