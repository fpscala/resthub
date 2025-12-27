package uz.scala.domain.enums

import enumeratum._
import enumeratum.EnumEntry.Uppercase

sealed trait ChatType extends EnumEntry with Uppercase

object ChatType extends Enum[ChatType] with CirceEnum[ChatType] with DoobieEnum[ChatType] {
  case object Channel extends ChatType
  case object Group extends ChatType
  case object Supergroup extends ChatType

  val values: IndexedSeq[ChatType] = findValues
}
