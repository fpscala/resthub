package uz.scala.repos.sql

import java.time.ZonedDateTime

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import uz.scala.domain.RefreshTokenId
import uz.scala.domain.UserId
import uz.scala.doobie.Sql
import uz.scala.doobie.syntax.all._
import uz.scala.repos.dto

private[repos] object RefreshTokensSql extends Sql[dto.RefreshToken] {
  def findByHash(tokenHash: String): Query0[dto.RefreshToken] =
    sql"""SELECT $columns FROM $table
          WHERE token_hash = $tokenHash
          LIMIT 1"""
      .query[dto.RefreshToken]

  def findById(id: RefreshTokenId): Query0[dto.RefreshToken] =
    sql"""SELECT $columns FROM $table
          WHERE id = $id
          LIMIT 1"""
      .query[dto.RefreshToken]

  val insert: Update[dto.RefreshToken] = Update[dto.RefreshToken](
    sql"""INSERT INTO $table ($columns) VALUES ($values)""".internals.sql
  )

  def update(token: dto.RefreshToken): Update0 =
    sql"""UPDATE $table
          SET revoked = ${token.revoked},
              revoked_at = ${token.revokedAt},
              revoke_reason = ${token.revokeReason},
              replaced_by_token_id = ${token.replacedByTokenId},
              reuse_window_expires_at = ${token.reuseWindowExpiresAt}
          WHERE id = ${token.id}""".update

  def revokeToken(
      id: RefreshTokenId,
      reason: String,
    ): Update0 =
    sql"""UPDATE $table
          SET revoked = true,
              revoked_at = NOW(),
              revoke_reason = $reason
          WHERE id = $id""".update

  def revokeAllForUser(
      userId: UserId,
      reason: String,
    ): Update0 =
    sql"""UPDATE $table
          SET revoked = true,
              revoked_at = NOW(),
              revoke_reason = $reason
          WHERE user_id = $userId
          AND revoked = false""".update

  def deleteExpired(before: ZonedDateTime): Update0 =
    sql"""DELETE FROM $table
          WHERE expires_at < $before
          OR (revoked = true AND revoked_at < $before)""".update

  def findActiveByUser(userId: UserId): Query0[dto.RefreshToken] =
    sql"""SELECT $columns FROM $table
          WHERE user_id = $userId
          AND revoked = false
          AND expires_at > NOW()
          ORDER BY created_at DESC"""
      .query[dto.RefreshToken]
}
