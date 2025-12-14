package uz.scala.domain.users

import java.util.UUID

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import uz.scala.domain.RoleId
import uz.scala.domain.enums.Privilege
import uz.scala.syntax.circe._

@JsonCodec
case class Role(
    id: RoleId,
    name: NonEmptyString,
    privileges: List[Privilege],
    description: Option[String] = None,
    isSystem: Boolean = false,
    createdAt: java.time.ZonedDateTime,
    updatedAt: Option[java.time.ZonedDateTime] = None,
  )

object Role {
  val USER: RoleId = RoleId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
  val ADMIN: RoleId = RoleId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
}
