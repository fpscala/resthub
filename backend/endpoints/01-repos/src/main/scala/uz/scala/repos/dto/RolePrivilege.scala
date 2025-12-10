package uz.scala.repos.dto

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString

import uz.scala.domain.RoleId

case class RolePrivilege(
    roleId: RoleId,
    privilege: NonEmptyString,
    createdAt: ZonedDateTime,
  )
