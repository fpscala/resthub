package uz.scala.setup

import cats.effect.Async
import cats.effect.Resource
import cats.effect.std.Console
import cats.effect.std.Random
import cats.implicits.catsSyntaxApplicativeByName
import cats.~>
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.NoOp.instance
import doobie.ConnectionIO
import doobie.WeakAsync
import doobie.syntax.connectionio.toConnectionIOOps
import eu.timepit.refined.pureconfig._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.server
import org.typelevel.log4cats.Logger
import pureconfig.generic.auto.exportReader
import pureconfig.module.cron4s._
import telegramium.bots.high.Api
import telegramium.bots.high.BotApi
import uz.scala.Algebras
import uz.scala.JobsEnvironment
import uz.scala.Repositories
import uz.scala.auth.impl.LiveMiddleware
import uz.scala.aws.s3.S3Client
import uz.scala.domain.AuthedUser
import uz.scala.domain.enums.Privilege
import uz.scala.doobie.DoobieTransaction
import uz.scala.flyway.Migrations
import uz.scala.http.{Environment => ServerEnvironment}
import uz.scala.mailer.Mailer
import uz.scala.redis.RedisClient
import uz.scala.utils.ConfigLoader

case class Environment[F[_]: Async: Logger: Random](
    config: Config,
    repositories: Repositories[ConnectionIO],
    s3Client: S3Client[F],
    redis: RedisClient[F],
    httpClient: Client[F],
    middleware: server.AuthMiddleware[F, AuthedUser],
    appMiddleware: server.AuthMiddleware[F, Unit],
  )(implicit
    xa: doobie.Transactor[F],
    lifter: F ~> ConnectionIO,
  ) {
  private val mailer: Mailer[F] = Mailer.make[F](config.mailer)

  private val botApi: Api[F] = BotApi(
    httpClient,
    baseUrl = s"https://api.telegram.org/bot${config.telegram.token}",
  )

  private val algebras: Algebras[F] =
    Algebras.make[F](
      s3Client,
      config.auth,
      repositories,
      redis,
      mailer,
      config.frontend.baseUrl.value,
      config.frontend.activationPath.value,
      botApi,
    )
  lazy val toServer: ServerEnvironment[F] =
    ServerEnvironment(
      middleware = middleware,
      appMiddleware = appMiddleware,
      config = config.http,
      botConfig = config.telegram,
      algebras = algebras,
      s3Client = s3Client,
    )

  lazy val toJobs: JobsEnvironment[F] = JobsEnvironment(
    repos = repositories,
    xa = xa,
  )
}
object Environment {
  def make[F[_]: Async: Console: Logger]: Resource[F, Environment[F]] =
    for {
      config <- Resource.eval(ConfigLoader.load[F, Config])
      _ <- Resource.eval(Migrations.run[F](config.migrations))
      implicit0(xa: doobie.Transactor[F]) = DoobieTransaction.make[F](config.database)
      repositories = Repositories.make
      redis <- Redis[F].utf8(config.redis.uri.toString).map(RedisClient[F](_, config.redis.prefix))
      _ <- Resource.eval(repositories.roles.insertPrivileges(Privilege.values.toList).transact(xa))
      middleware = LiveMiddleware.make[F](config.auth) // Stateless JWT - no Redis
      appMiddleware = LiveMiddleware.makeForApp[F](config.auth)
      implicit0(random: Random[F]) <- Resource.eval(Random.scalaUtilRandom[F])
      implicit0(lifter: (F ~> ConnectionIO)) <- WeakAsync.liftK[F, ConnectionIO]

      s3Client <- S3Client.resource(config.awsConfig)
      httpClient <- BlazeClientBuilder[F].resource
      env = Environment[F](
        config = config,
        repositories = repositories,
        s3Client = s3Client,
        redis = redis,
        httpClient = httpClient,
        middleware = middleware,
        appMiddleware = appMiddleware,
      )
      _ <- Resource.eval(env.algebras.assets.initializeBucket())
      _ <- Resource.eval(env.algebras.telegramBot.setupWebhook(config.telegram.webhookUrl)).whenA(config.telegram.useWebhook)

    } yield env
}
