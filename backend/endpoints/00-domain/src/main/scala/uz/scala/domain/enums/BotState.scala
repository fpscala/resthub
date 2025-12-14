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

  val values: IndexedSeq[BotState] = findValues
}
