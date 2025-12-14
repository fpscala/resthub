package uz.scala.domain.cities

import io.circe.generic.JsonCodec

import uz.scala.domain.CityId
import uz.scala.syntax.circe._

@JsonCodec
case class City(
    id: CityId,
    name: String,
    isActive: Boolean = true,
  )
