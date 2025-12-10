package uz.scala.domain.auth

import io.circe.generic.JsonCodec
import io.circe.refined._

import uz.scala.syntax.circe._

@JsonCodec
case class AuthTokens(
    accessToken: String, // JWT, short-lived (15 min)
    refreshToken: String, // Random UUID, long-lived (7 days)
    tokenType: String = "Bearer",
    expiresIn: Long = 900, // 15 minutes in seconds
  )

@JsonCodec
case class DeviceInfo(
    platform: String, // "android", "ios", "web"
    deviceId: Option[String] = None,
    deviceModel: Option[String] = None,
    osVersion: Option[String] = None,
    appVersion: Option[String] = None,
  )
