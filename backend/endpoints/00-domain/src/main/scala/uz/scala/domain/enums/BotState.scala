package uz.scala.domain.enums

import enumeratum._
import enumeratum.EnumEntry.UpperSnakecase

sealed trait BotState extends EnumEntry with UpperSnakecase

object BotState extends Enum[BotState] with CirceEnum[BotState] with DoobieEnum[BotState] {
  case object Idle extends BotState
  case object AwaitingCity extends BotState
  case object AwaitingPriceRange extends BotState
  case object AwaitingCustomCity extends BotState
  case object AwaitingCustomPriceMin extends BotState
  case object AwaitingCustomPriceMax extends BotState
  case object ViewingResults extends BotState
  case object Registering extends BotState
  // Admin posting states
  case object AwaitingListingType extends BotState
  case object AwaitingPrice extends BotState
  case object AwaitingCityForPosting extends BotState
  case object AwaitingRooms extends BotState
  case object AwaitingPhone extends BotState
  case object AwaitingDistrict extends BotState
  case object AwaitingFloor extends BotState
  case object AwaitingTotalFloors extends BotState
  case object AwaitingBuildingType extends BotState
  case object AwaitingCondition extends BotState
  case object AwaitingConfirmation extends BotState
  case object AwaitingChannelSelection extends BotState

  val values: IndexedSeq[BotState] = findValues
}
