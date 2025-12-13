package uz.scala.domain.auth

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import uz.scala.domain.Phone
import uz.scala.shared.EmailAddress

@JsonCodec
case class RegisterInput(
    email: EmailAddress,
    password: NonEmptyString,
    firstName: NonEmptyString,
    lastName: NonEmptyString,
    phone: Phone,
  )
