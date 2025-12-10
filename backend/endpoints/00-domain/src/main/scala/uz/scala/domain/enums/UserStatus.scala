package uz.scala.domain.enums

import enumeratum._
import enumeratum.EnumEntry.Snakecase

sealed trait UserStatus extends EnumEntry with Snakecase

object UserStatus extends Enum[UserStatus] with CirceEnum[UserStatus] with DoobieEnum[UserStatus] {
  case object Active extends UserStatus
  case object Inactive extends UserStatus
  case object Blocked extends UserStatus
  case object PendingVerification extends UserStatus

  val values: IndexedSeq[UserStatus] = findValues
}
