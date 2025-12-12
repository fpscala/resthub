package uz.scala.domain.listings

import java.time.ZonedDateTime

import io.circe.generic.JsonCodec

import uz.scala.domain.ContractId
import uz.scala.domain.ListingId
import uz.scala.domain.UserId
import uz.scala.syntax.circe._

@JsonCodec
case class Contract(
    id: ContractId,
    listingId: ListingId,
    pdfUrl: String,
    generatedBy: UserId,
    createdAt: ZonedDateTime,
  )
