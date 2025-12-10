package uz.scala

import java.util.UUID

import derevo.cats.eqv
import derevo.cats.show
import derevo.derive
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval.Closed
import eu.timepit.refined.string.MatchesRegex
import io.circe.Decoder
import io.circe.Encoder
import io.estatico.newtype.macros.newtype
import squants.Money
import squants.market.Currency

import uz.scala.utils.uuid

package object domain {
  // refined types
  type RatingType = Int Refined Closed[1, 5]
  type Phone = String Refined MatchesRegex[W.`"""[+][\\d]{12}+"""`.T]

  type WebsiteUrl = String Refined MatchesRegex[
    W.`"""https?://[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?"""`.T
  ]
  type Percentage = BigDecimal Refined Closed[1, 100]
  object Percentage {
    def fromBigDecimal(value: BigDecimal): Percentage = {
      val scaled = value.setScale(2, BigDecimal.RoundingMode.HALF_UP) * 100
      eu.timepit.refined.refineV[Closed[1, 100]](scaled) match {
        case Right(refined) => refined
        case Left(_) =>
          eu.timepit.refined.refineV[Closed[1, 100]](BigDecimal(50)) match {
            case Right(default) => default
            case Left(_) => throw new RuntimeException("Failed to create default Percentage")
          }
      }
    }
  }

  object UZS extends Currency("UZS", "Uzbek sum", "SUM", 2)

  // newtypes
  @derive(eqv, show, uuid)
  @newtype case class UserId(value: UUID)
  @derive(eqv, show, uuid)
  @newtype case class AssetId(value: UUID)
  @derive(eqv, show, uuid)
  @newtype case class RoleId(value: UUID)
  @derive(eqv, show, uuid)
  @newtype case class RefreshTokenId(value: UUID)
}
