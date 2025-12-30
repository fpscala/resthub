package uz.scala.domain.telegram

import io.circe.generic.JsonCodec

import uz.scala.domain.enums.ListingType

@JsonCodec
case class AdminPostingContext(
    forwardedMessage: ForwardedMessage,
    listingType: Option[ListingType] = None,
    price: Option[BigDecimal] = None,
    city: Option[String] = None,
    rooms: Option[Int] = None,
    phone: Option[String] = None,
    district: Option[String] = None,
    floor: Option[Int] = None,
    totalFloors: Option[Int] = None,
    buildingType: Option[String] = None,
    condition: Option[String] = None,
    description: Option[String] = None, // User-provided description (optional)
    selectedChannelId: Option[Long] = None,
) {
  // Helper method to create a copy with updated field (used in broker flow)
  def withListingType(value: ListingType): AdminPostingContext = copy(listingType = Some(value))
  def withPrice(value: BigDecimal): AdminPostingContext = copy(price = Some(value))
  def withCity(value: String): AdminPostingContext = copy(city = Some(value))
  def withRooms(value: Int): AdminPostingContext = copy(rooms = Some(value))
  def withPhone(value: String): AdminPostingContext = copy(phone = Some(value))
  def withDistrict(value: String): AdminPostingContext = copy(district = Some(value))
  def withFloor(value: Int): AdminPostingContext = copy(floor = Some(value))
  def withTotalFloors(value: Int): AdminPostingContext = copy(totalFloors = Some(value))
  def withBuildingType(value: String): AdminPostingContext = copy(buildingType = Some(value))
  def withCondition(value: String): AdminPostingContext = copy(condition = Some(value))
  def withDescription(value: String): AdminPostingContext = copy(description = Some(value))
  def withChannel(value: Long): AdminPostingContext = copy(selectedChannelId = Some(value))

  // Helper method to create empty context
  def empty: AdminPostingContext = AdminPostingContext(
    forwardedMessage = ForwardedMessage(None, List.empty, None)
  )
}
