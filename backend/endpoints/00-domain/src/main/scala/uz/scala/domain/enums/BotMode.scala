package uz.scala.domain.enums

import enumeratum._
import enumeratum.EnumEntry.Lowercase

sealed trait BotMode extends EnumEntry with Lowercase

object BotMode extends Enum[BotMode] with CirceEnum[BotMode] with DoobieEnum[BotMode] {
  case object Buyer extends BotMode
  case object Broker extends BotMode

  val values: IndexedSeq[BotMode] = findValues
}