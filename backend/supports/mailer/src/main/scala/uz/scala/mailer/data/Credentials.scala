package uz.scala.mailer.data

import eu.timepit.refined.types.string.NonEmptyString

case class Credentials(user: NonEmptyString, password: NonEmptyString)
