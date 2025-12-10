package uz.scala

import scala.math.BigDecimal.RoundingMode

import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import squants.Money

import uz.scala.domain.UZS

package object shared {
  def smartRound(value: Money): Money =
    smartRound(value.amount)

  def smartRound(value: BigDecimal): Money = {
    // Define the rounding bases
    val base500 = BigDecimal.valueOf(500)
    val base1000 = BigDecimal.valueOf(1000)

    // Determine the appropriate base based on the value
    val roundingBase =
      if (value.compareTo(BigDecimal.valueOf(1000)) < 0) base500 // Less than 1000: round to 500
      else base500 // Greater than or equal to 1000: round to 500

    val roundedValue = (value / roundingBase).setScale(0, RoundingMode.CEILING) * roundingBase

    // Return the result wrapped in the Money case class
    UZS(roundedValue)
  }

  type EmailAddress =
    String Refined MatchesRegex[W.`"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+[.][a-zA-Z]{2,}"`.T]
}
