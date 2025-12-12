package uz.scala.domain.auth

import io.circe.generic.JsonCodec
import io.circe.refined._
import eu.timepit.refined.types.string.NonEmptyString

import uz.scala.domain.Phone

@JsonCodec
case class RegisterInput(
    email: NonEmptyString,
    password: NonEmptyString,
    firstName: NonEmptyString,
    lastName: NonEmptyString,
    phone: Option[Phone] = None,
  )