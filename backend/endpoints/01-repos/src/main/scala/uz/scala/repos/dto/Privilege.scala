package uz.scala.repos.dto

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString

case class Privilege(
    name: NonEmptyString,
    groupName: NonEmptyString,
    description: Option[String],
    createdAt: ZonedDateTime,
  )
