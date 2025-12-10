package uz.scala.repos.dto

import eu.timepit.refined.types.string.NonEmptyString

import uz.scala.domain.UserId

case class UserInfo(
    id: UserId,
    name: NonEmptyString,
  )
