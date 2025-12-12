package uz.scala.domain.listings

import eu.timepit.refined.types.string.NonEmptyString

import uz.scala.domain.enums.ListingStatus

case class ListingFilters(
    city: Option[NonEmptyString] = None,
    minPrice: Option[BigDecimal] = None,
    maxPrice: Option[BigDecimal] = None,
    status: Option[ListingStatus] = None,
    page: Option[Int] = None,
    size: Option[Int] = None,
  )
