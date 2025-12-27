package uz.scala.domain.enums

import enumeratum._
import enumeratum.EnumEntry.UpperSnakecase

sealed trait BotState extends EnumEntry with UpperSnakecase

object BotState extends Enum[BotState] with CirceEnum[BotState] with DoobieEnum[BotState] {
  // ============================================================
  // SHARED STATE
  // ============================================================
  case object Idle extends BotState

  // ============================================================
  // BUYER SEARCH STATES (ONLY USED IN BUYER MODE)
  // ============================================================
  case object AwaitingCity extends BotState
  case object AwaitingPriceRange extends BotState
  case object AwaitingCustomCity extends BotState
  case object AwaitingCustomPriceMin extends BotState
  case object AwaitingCustomPriceMax extends BotState
  case object ViewingResults extends BotState

  // ============================================================
  // BROKER POSTING STATES (ONLY USED IN BROKER MODE)
  // Flow: ListingType → Price → City → Rooms → Phone → Images →
  //       District → Floor → TotalFloors → BuildingType → Condition →
  //       Confirmation → ChannelSelection
  // ============================================================
  case object BrokerAwaitingListingType extends BotState
  case object BrokerAwaitingPrice extends BotState
  case object BrokerAwaitingCity extends BotState
  case object BrokerAwaitingRooms extends BotState
  case object BrokerAwaitingPhone extends BotState
  case object BrokerAwaitingImages extends BotState  // NEW: 1-10 photos
  case object BrokerAwaitingDistrict extends BotState
  case object BrokerAwaitingFloor extends BotState
  case object BrokerAwaitingTotalFloors extends BotState
  case object BrokerAwaitingBuildingType extends BotState
  case object BrokerAwaitingCondition extends BotState
  case object BrokerAwaitingConfirmation extends BotState
  case object BrokerAwaitingChannelSelection extends BotState

  val values: IndexedSeq[BotState] = findValues
}
