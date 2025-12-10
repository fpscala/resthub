package uz.scala.jobs

import scala.collection.immutable

import enumeratum.EnumEntry.Lowercase
import enumeratum._
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveEnumerationReader
sealed trait JobRunMode extends EnumEntry with Lowercase

object JobRunMode extends CirceEnum[JobRunMode] with Enum[JobRunMode] {
  final case object Once extends JobRunMode
  final case object Forever extends JobRunMode
  override def values: immutable.IndexedSeq[JobRunMode] = findValues
  implicit val configReader: ConfigReader[JobRunMode] = deriveEnumerationReader[JobRunMode]
}
