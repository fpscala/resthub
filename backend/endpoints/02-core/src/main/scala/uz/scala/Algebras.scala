package uz.scala

import _root_.doobie.ConnectionIO
import _root_.doobie.Transactor
import cats.effect.Sync
import cats.effect.std.Random
import cats.~>
import org.typelevel.log4cats.Logger

import uz.scala.algebras._
import uz.scala.auth.AuthConfig
import uz.scala.auth.impl.Auth
import uz.scala.aws.s3.S3Client
import uz.scala.domain.AuthedUser
import uz.scala.mailer.Mailer
import uz.scala.redis.RedisClient
import uz.scala.services.PdfService

case class Algebras[F[_]](
    auth: Auth[F, AuthedUser],
    users: UsersAlgebra[F],
    roles: RolesAlgebra[F],
    assets: AssetsAlgebra[F],
    authAlgebra: AuthAlgebra[F],
    emailService: EmailService[F],
    listings: ListingsAlgebra[F],
    adminListings: AdminListingsAlgebra[F],
    contracts: ContractsAlgebra[F],
    telegramBot: TelegramBotAlgebra[F],
    cities: CitiesAlgebra[F],
  )

object Algebras {
  def make[F[_]: Sync: Logger: Random](
      s3Client: S3Client[F],
      config: AuthConfig,
      repositories: Repositories[ConnectionIO],
      redis: RedisClient[F], // Still used for support module, not auth
      mailer: Mailer[F],
      frontendBaseUrl: String,
      activationPath: String,
      botApi: telegramium.bots.high.Api[F],
      botToken: String,
      webhookBaseUrl: String,
    )(implicit
      xa: Transactor[F],
      lifter: F ~> ConnectionIO,
    ): Algebras[F] = {
    val users = UsersAlgebra.make[F](repositories.users, repositories.roles)
    val roles = RolesAlgebra.make[F](repositories.roles)
    val emailService = EmailService.make[F](mailer, frontendBaseUrl, activationPath)
    val authAlgebra = AuthAlgebra.make[F](repositories.users, repositories.roles)
    val listings =
      ListingsAlgebra.make[F](repositories.listings, repositories.users, repositories.roles)
    val adminListings =
      AdminListingsAlgebra.make[F](repositories.listings, repositories.users, repositories.roles)
    val pdfService = PdfService.make[F]
    val contracts =
      ContractsAlgebra.make[F](
        repositories.contracts,
        repositories.listings,
        repositories.users,
        s3Client,
        pdfService,
      )
    val cities = CitiesAlgebra.make[F](repositories.cities)

    val telegramBot = TelegramBotAlgebra.make[F](
      botApi,
      repositories.telegramUsers,
      repositories.telegramSessions,
      repositories.listings,
      listings,
      cities,
      authAlgebra,
      botToken,
      webhookBaseUrl,
    )

    Algebras[F](
      auth = Auth.make[F](
        config,
        users,
        repositories.users,
        repositories.roles,
        repositories.refreshTokens,
      ),
      users = users,
      roles = roles,
      assets = AssetsAlgebra.make[F](
        s3Client
      ),
      authAlgebra = authAlgebra,
      emailService = emailService,
      listings = listings,
      adminListings = adminListings,
      contracts = contracts,
      telegramBot = telegramBot,
      cities = cities,
    )
  }
}
