package uz.scala

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.ExitCode
import cats.effect.kernel.Resource
import cats.implicits.toFunctorOps
import cats.implicits.toSemigroupKOps
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

import uz.scala.domain.AuthedUser
import uz.scala.http.Environment
import uz.scala.http4s.HttpServer
import uz.scala.http4s.utils.Routes
import uz.scala.routes._

object HttpModule {
  private def allRoutes[F[_]: Async: JsonDecoder: Logger](
      env: Environment[F]
    ): NonEmptyList[HttpRoutes[F]] =
    NonEmptyList
      .of[Routes[F, AuthedUser]](
        new AuthRoutes[F](env.algebras.auth, env.algebras.authAlgebra),
        new UsersRoutes[F](env.algebras.users, env.algebras.roles),
        new RolesRoutes[F](env.algebras.roles),
        new RootRoutes[F](env.algebras.assets),
        new ListingsRoutes[F](env.algebras.listings),
        new AdminListingsRoutes[F](env.algebras.adminListings),
        new ContractsRoutes[F](env.algebras.contracts),
        new S3Routes[F](env.s3Client),
      )
      .map { r =>
        Router(
          r.path -> (r.public <+> env.middleware(r.`private`))
        )
      }

  def make[F[_]: Async](
      env: Environment[F]
    )(implicit
      logger: Logger[F]
    ): Resource[F, F[ExitCode]] =
    HttpServer
      .make[F](env.config, _ => allRoutes[F](env))
      .map { _ =>
        logger.info(s"HTTP server is started").as(ExitCode.Success)
      }
}
