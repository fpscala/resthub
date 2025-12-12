package uz.scala.repos.dto

import java.time.ZonedDateTime

import uz.scala.domain.ContractId
import uz.scala.domain.ListingId
import uz.scala.domain.UserId

case class Contract(
    id: ContractId,
    listingId: ListingId,
    pdfUrl: String,
    generatedBy: UserId,
    createdAt: ZonedDateTime,
  )
