package uz.scala.domain.enums

import enumeratum._
import enumeratum.EnumEntry.UpperSnakecase

sealed trait ListingType extends EnumEntry with UpperSnakecase {
  def valueUz: String
}

object ListingType extends Enum[ListingType] with CirceEnum[ListingType] with DoobieEnum[ListingType] {
  case object ForSale extends ListingType {
    val valueUz: String = "Sotiladi"
  }
  case object ForRent extends ListingType {
    val valueUz: String = "Ijaraga"
  }

  val values: IndexedSeq[ListingType] = findValues
}