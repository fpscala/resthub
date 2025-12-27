package uz.scala

import _root_.doobie.ConnectionIO
import cats.effect.kernel.MonadCancelThrow

import uz.scala.repos._

case class Repositories[F[_]](
    users: UsersRepository[F],
    roles: RolesRepository[F],
    refreshTokens: RefreshTokensRepository[F],
    listings: ListingsRepository[F],
    contracts: ContractsRepository[F],
    telegramUsers: TelegramUsersRepository[F],
    telegramSessions: TelegramSessionsRepository[F],
    brokerChannels: BrokerChannelsRepository[F],
    listingChannels: ListingChannelsRepository[F],
    cities: CitiesRepository[F],
  )

object Repositories {
  def make[F[_]: MonadCancelThrow]: Repositories[ConnectionIO] =
    Repositories(
      users = UsersRepository.make,
      roles = RolesRepository.make,
      refreshTokens = RefreshTokensRepository.make,
      listings = ListingsRepository.make,
      contracts = ContractsRepository.make,
      telegramUsers = TelegramUsersRepository.make,
      telegramSessions = TelegramSessionsRepository.make,
      brokerChannels = BrokerChannelsRepository.make,
      listingChannels = ListingChannelsRepository.make,
      cities = CitiesRepository.make,
    )
}
