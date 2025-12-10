package uz.scala

import _root_.doobie.ConnectionIO
import cats.effect.kernel.MonadCancelThrow

import uz.scala.repos._

case class Repositories[F[_]](
    users: UsersRepository[F],
    roles: RolesRepository[F],
    refreshTokens: RefreshTokensRepository[F],
  )

object Repositories {
  def make[F[_]: MonadCancelThrow]: Repositories[ConnectionIO] =
    Repositories(
      users = UsersRepository.make,
      roles = RolesRepository.make,
      refreshTokens = RefreshTokensRepository.make,
    )
}
