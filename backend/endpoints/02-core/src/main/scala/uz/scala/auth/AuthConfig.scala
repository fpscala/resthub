package uz.scala.auth

import scala.concurrent.duration.FiniteDuration

import eu.timepit.refined.types.string.NonEmptyString

case class AuthConfig(
    tokenKey: NonEmptyString,
    appTokenKey: NonEmptyString,
    appToken: NonEmptyString,
    accessTokenExpiration: FiniteDuration,
    refreshTokenExpiration: FiniteDuration,
  )
