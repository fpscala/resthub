package uz.scala.repos.dto

import io.scalaland.chimney.dsl.TransformerOps

import uz.scala.domain.CityId

case class City(
    id: CityId,
    name: String,
    isActive: Boolean,
  ) {
  def toDomain: uz.scala.domain.cities.City =
    this.transformInto[uz.scala.domain.cities.City]
}

object City {
  def fromDomain(city: uz.scala.domain.cities.City): City =
    city.transformInto[City]
}
