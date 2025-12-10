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

case class Algebras[F[_]](
    auth: Auth[F, AuthedUser],
    users: UsersAlgebra[F],
    roles: RolesAlgebra[F],
    assets: AssetsAlgebra[F],
    authAlgebra: AuthAlgebra[F],
    emailService: EmailService[F],
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
    )(implicit
      xa: Transactor[F],
      lifter: F ~> ConnectionIO,
    ): Algebras[F] = {
    val users = UsersAlgebra.make[F](repositories.users, repositories.roles)
    val roles = RolesAlgebra.make[F](repositories.roles)
    val emailService = EmailService.make[F](mailer, frontendBaseUrl, activationPath)
    val authAlgebra = AuthAlgebra.make[F](repositories.users, emailService)

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
    )
  }
}
