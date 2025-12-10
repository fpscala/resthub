package uz.scala.domain.users

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import uz.scala.domain.UserId
import uz.scala.syntax.circe._

@JsonCodec
case class UserInfo(
    id: UserId,
    name: NonEmptyString,
  )
