package uz.scala.domain.enums

import enumeratum._
import enumeratum.EnumEntry.Snakecase

sealed trait BotState extends EnumEntry with Snakecase

object BotState extends Enum[BotState] with CirceEnum[BotState] with DoobieEnum[BotState] {
  case object Idle extends BotState
  case object AwaitingCity extends BotState
  case object AwaitingPriceRange extends BotState
  case object Registering extends BotState

  val values: IndexedSeq[BotState] = findValues
}
