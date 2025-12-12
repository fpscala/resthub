package uz.scala.domain.telegram

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

@derive(encoder, decoder)
case class SearchContext(
    city: Option[String] = None,
    minPrice: Option[BigDecimal] = None,
    maxPrice: Option[BigDecimal] = None,
  )
