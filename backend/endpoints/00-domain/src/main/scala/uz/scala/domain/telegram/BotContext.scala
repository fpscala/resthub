package uz.scala.domain.telegram

import io.circe.generic.JsonCodec
import uz.scala.domain.enums.{BotMode, ListingType}
import uz.scala.domain.telegram.SearchContext

@JsonCodec
case class BotContext(
    mode: BotMode,
    draftListing: Option[AdminPostingContext] = None,
    searchContext: Option[SearchContext] = None
)