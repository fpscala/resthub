package uz.scala.domain.auth

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined._

import uz.scala.domain.custom.refinements.Tel

@derive(encoder, decoder)
case class RegisterInput(
    email: NonEmptyString,
    password: NonEmptyString,
    firstName: NonEmptyString,
    lastName: NonEmptyString,
    phone: Option[Tel] = None,
  )