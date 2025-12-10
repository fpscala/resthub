package uz.scala.domain.users

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import uz.scala.domain.enums.Privilege

@JsonCodec
case class RoleUpdateInput(
    name: Option[NonEmptyString],
    privileges: Option[List[Privilege]],
  )
