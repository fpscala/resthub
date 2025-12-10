package uz.scala.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait SortOrder extends Snakecase {
  def value: String
}

object SortOrder extends Enum[SortOrder] with CirceEnum[SortOrder] with DoobieEnum[SortOrder] {
  case object Descending extends SortOrder {
    override def value: String = "DESC"
  }
  case object Ascending extends SortOrder {
    override def value: String = "ASC"
  }

  def values: IndexedSeq[SortOrder] = findValues
}
