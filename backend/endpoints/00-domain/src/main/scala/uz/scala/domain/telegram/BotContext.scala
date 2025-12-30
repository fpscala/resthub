package uz.scala.domain.telegram

import io.circe.generic.JsonCodec

import uz.scala.domain.enums.BotMode

@JsonCodec
case class BotContext(
    mode: BotMode,
    // ============================================================
    // BUYER SEARCH CONTEXT (ONLY USED IN BUYER MODE)
    // ============================================================
    search: Option[SearchContext] = None,
    // ============================================================
    // BROKER POSTING CONTEXT (ONLY USED IN BROKER MODE)
    // ============================================================
    broker: Option[BrokerFlowContext] = None,
  )

object BotContext {
  // ============================================================
  // LEGACY ACCESSORS (FOR BACKWARD COMPATIBILITY - DEPRECATE)
  // ============================================================
  @deprecated("Use .broker.flatMap(_.draft) instead", "0.1.0")
  def draftListing(context: BotContext): Option[AdminPostingContext] = context.broker.map(_.draft)

  @deprecated("Use .search instead", "0.1.0")
  def searchContext(context: BotContext): Option[SearchContext] = context.search

  def apply(mode: BotMode): BotContext = BotContext(mode = mode, search = None, broker = None)
}

@JsonCodec
case class BrokerFlowContext(
    draft: AdminPostingContext,
    currentStep: BrokerStep = BrokerStep.Start,
  )

sealed trait BrokerStep
object BrokerStep {
  case object Start extends BrokerStep
  case object ListingType extends BrokerStep
  case object Price extends BrokerStep
  case object City extends BrokerStep
  case object Rooms extends BrokerStep
  case object Phone extends BrokerStep
  case object District extends BrokerStep
  case object Floor extends BrokerStep
  case object TotalFloors extends BrokerStep
  case object BuildingType extends BrokerStep
  case object Condition extends BrokerStep
  case object Description extends BrokerStep // NEW: Optional description after Condition
  case object Confirmation extends BrokerStep
  case object ChannelSelection extends BrokerStep
  case object Complete extends BrokerStep

  implicit val brokerStepEncoder: io.circe.Encoder[BrokerStep] =
    io.circe.Encoder.encodeString.contramap(_.toString)
  implicit val brokerStepDecoder: io.circe.Decoder[BrokerStep] =
    io.circe.Decoder.decodeString.emap {
      case "Start" => Right(Start)
      case "ListingType" => Right(ListingType)
      case "Price" => Right(Price)
      case "City" => Right(City)
      case "Rooms" => Right(Rooms)
      case "Phone" => Right(Phone)
      case "District" => Right(District)
      case "Floor" => Right(Floor)
      case "TotalFloors" => Right(TotalFloors)
      case "BuildingType" => Right(BuildingType)
      case "Condition" => Right(Condition)
      case "Description" => Right(Description)
      case "Confirmation" => Right(Confirmation)
      case "ChannelSelection" => Right(ChannelSelection)
      case "Complete" => Right(Complete)
      case other => Left(s"Unknown BrokerStep: $other")
    }
}
