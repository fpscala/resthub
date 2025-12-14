package uz.scala.domain.telegram

import io.circe.generic.JsonCodec

@JsonCodec
case class SearchContext(
    city: Option[String] = None,
    minPrice: Option[BigDecimal] = None,
    maxPrice: Option[BigDecimal] = None,
    rooms: Option[Int] = None,
    page: Int = 1,
    totalResults: Long = 0,
  )
