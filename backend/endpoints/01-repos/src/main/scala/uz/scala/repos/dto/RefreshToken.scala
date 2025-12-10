package uz.scala.repos.dto

import java.time.ZonedDateTime

import uz.scala.domain.RefreshTokenId
import uz.scala.domain.UserId

case class RefreshToken(
    id: RefreshTokenId,
    userId: UserId,
    tokenHash: String, // SHA-256 hash of the actual token

    // Device Information (for analytics and multi-device management)
    deviceInfo: Option[io.circe.Json] = None, // {platform: "android", device_id: "...", model: "...", os_version: "...", app_version: "1.0.0"}
    ipAddress: Option[String] = None,
    userAgent: Option[String] = None,

    // Token Rotation with Grace Period
    replacedByTokenId: Option[RefreshTokenId] = None, // Chain to new token
    reuseWindowExpiresAt: Option[ZonedDateTime] = None, // Grace period (30 sec)

    // Lifecycle
    expiresAt: ZonedDateTime,
    revoked: Boolean = false,
    revokedAt: Option[ZonedDateTime] = None,
    revokeReason: Option[String] = None, // "logout", "security", "token_rotation", "reuse_detected"

    createdAt: ZonedDateTime,
  )
