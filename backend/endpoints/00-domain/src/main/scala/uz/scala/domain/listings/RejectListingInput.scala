package uz.scala.domain.listings

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined._

@derive(encoder, decoder)
case class RejectListingInput(
    reason: NonEmptyString
  )
