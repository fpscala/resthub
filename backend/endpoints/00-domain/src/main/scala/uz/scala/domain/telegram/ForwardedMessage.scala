package uz.scala.domain.telegram

import io.circe.generic.JsonCodec

@JsonCodec
case class ForwardedMessage(
    text: Option[String],
    images: List[String],
    originalAuthor: Option[String],
  )
