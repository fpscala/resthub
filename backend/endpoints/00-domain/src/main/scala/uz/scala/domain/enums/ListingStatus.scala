package uz.scala.domain.enums

import enumeratum._
import enumeratum.EnumEntry.Snakecase

sealed trait ListingStatus extends EnumEntry with Snakecase

object ListingStatus extends Enum[ListingStatus] with CirceEnum[ListingStatus] with DoobieEnum[ListingStatus] {
  case object Pending extends ListingStatus
  case object Approved extends ListingStatus
  case object Rejected extends ListingStatus

  val values: IndexedSeq[ListingStatus] = findValues
}
