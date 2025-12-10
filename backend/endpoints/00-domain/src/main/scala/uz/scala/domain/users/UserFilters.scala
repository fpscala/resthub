package uz.scala.domain.users

import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import uz.scala.domain.Phone
import uz.scala.domain.RoleId
import uz.scala.syntax.circe._

@JsonCodec
case class UserFilters(
    firstname: Option[NonEmptyString] = None,
    lastname: Option[NonEmptyString] = None,
    phone: Option[Phone] = None,
    roleId: Option[RoleId] = None,
    limit: Option[NonNegInt] = None,
    page: Option[NonNegInt] = None,
  )
