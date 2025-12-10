package uz.scala.setup

import eu.timepit.refined.types.string.NonEmptyString

case class FrontendConfig(
    baseUrl: NonEmptyString,
    activationPath: NonEmptyString,
  )
