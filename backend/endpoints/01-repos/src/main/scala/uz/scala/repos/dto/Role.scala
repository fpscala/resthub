package uz.scala.repos.dto

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.scalaland.chimney.dsl._

import uz.scala.domain.RoleId
import uz.scala.domain.enums.Privilege

case class Role(
    id: RoleId,
    name: NonEmptyString,
    description: Option[String],
    isSystem: Boolean = false,
    createdAt: ZonedDateTime,
    updatedAt: Option[ZonedDateTime] = None,
  ) {
  def toDomain(privileges: List[Privilege]): uz.scala.domain.users.Role =
    this
      .into[uz.scala.domain.users.Role]
      .withFieldConst(_.privileges, privileges)
      .transform
}

object Role {
  def fromDomain(
      domain: uz.scala.domain.users.Role
    ): Role =
    domain
      .into[Role]
      .withFieldComputed(_.description, _.description)
      .withFieldComputed(_.isSystem, _.isSystem)
      .withFieldComputed(_.createdAt, _.createdAt)
      .withFieldComputed(_.updatedAt, _.updatedAt)
      .transform
}
