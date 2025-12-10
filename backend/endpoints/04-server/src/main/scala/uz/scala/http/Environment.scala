package uz.scala.http

import cats.effect.Async
import org.http4s.server

import uz.scala.Algebras
import uz.scala.domain.AuthedUser
import uz.scala.http4s.HttpServerConfig

case class Environment[F[_]: Async](
    config: HttpServerConfig,
    middleware: server.AuthMiddleware[F, AuthedUser],
    appMiddleware: server.AuthMiddleware[F, Unit],
    algebras: Algebras[F],
  )
