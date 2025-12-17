package uz.scala.domain.telegram

import io.circe.generic.JsonCodec

import uz.scala.domain.enums.ListingType

@JsonCodec
case class AdminPostingContext(
    forwardedMessage: ForwardedMessage,
    listingType: Option[ListingType],
    price: Option[BigDecimal],
    city: Option[String],
    rooms: Option[Int],
    phone: Option[String],
    district: Option[String],
    floor: Option[Int],
    totalFloors: Option[Int],
    buildingType: Option[String],
    condition: Option[String],
    selectedChannelId: Option[Long],
    step: Int = 0,
  )
