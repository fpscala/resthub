package uz.scala.domain.cities

import io.circe.generic.JsonCodec

@JsonCodec
case class CityInput(
    name: String,
    isActive: Boolean = true,
  )
