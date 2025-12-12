package uz.scala.domain.listings

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

import uz.scala.domain.ListingId

@derive(encoder, decoder)
case class GenerateContractInput(
    listingId: ListingId
  )
