package uz.scala.repos

import java.time.ZonedDateTime

import doobie.ConnectionIO

import uz.scala.domain.RefreshTokenId
import uz.scala.domain.UserId
import uz.scala.repos.sql.RefreshTokensSql

trait RefreshTokensRepository[F[_]] {
  def findByHash(tokenHash: String): F[Option[dto.RefreshToken]]
  def findById(id: RefreshTokenId): F[Option[dto.RefreshToken]]
  def create(token: dto.RefreshToken): F[RefreshTokenId]
  def update(id: RefreshTokenId)(update: dto.RefreshToken => dto.RefreshToken): F[Unit]
  def revokeToken(id: RefreshTokenId, reason: String): F[Unit]
  def revokeAllForUser(userId: UserId, reason: String): F[Unit]
  def deleteExpired(before: ZonedDateTime): F[Unit]
  def findActiveByUser(userId: UserId): F[List[dto.RefreshToken]]
}

object RefreshTokensRepository {
  def make: RefreshTokensRepository[ConnectionIO] = new RefreshTokensRepository[ConnectionIO] {
    override def findByHash(tokenHash: String): ConnectionIO[Option[dto.RefreshToken]] =
      RefreshTokensSql.findByHash(tokenHash).option

    override def findById(id: RefreshTokenId): ConnectionIO[Option[dto.RefreshToken]] =
      RefreshTokensSql.findById(id).option

    override def create(token: dto.RefreshToken): ConnectionIO[RefreshTokenId] =
      RefreshTokensSql.insert.run(token).as(token.id)

    override def update(
        id: RefreshTokenId
      )(
        update: dto.RefreshToken => dto.RefreshToken
      ): ConnectionIO[Unit] =
      for {
        tokenOpt <- findById(id)
        _ <- tokenOpt match {
          case Some(token) => RefreshTokensSql.update(update(token)).run.void
          case None => ().pure[ConnectionIO]
        }
      } yield ()

    override def revokeToken(
        id: RefreshTokenId,
        reason: String,
      ): ConnectionIO[Unit] =
      RefreshTokensSql.revokeToken(id, reason).run.void

    override def revokeAllForUser(
        userId: UserId,
        reason: String,
      ): ConnectionIO[Unit] =
      RefreshTokensSql.revokeAllForUser(userId, reason).run.void

    override def deleteExpired(before: ZonedDateTime): ConnectionIO[Unit] =
      RefreshTokensSql.deleteExpired(before).run.void

    override def findActiveByUser(userId: UserId): ConnectionIO[List[dto.RefreshToken]] =
      RefreshTokensSql.findActiveByUser(userId).to[List]
  }
}
