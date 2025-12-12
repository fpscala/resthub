package uz.scala.routes

import cats.MonadThrow
import cats.implicits._
import io.circe.generic.JsonCodec
import org.http4s.AuthedRoutes
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.typelevel.log4cats.Logger

import uz.scala.Language
import uz.scala.algebras.ContractsAlgebra
import uz.scala.domain.AuthedUser
import uz.scala.domain.listings.GenerateContractInput
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

@JsonCodec
case class GenerateContractResponse(
    pdfUrl: String
  )

final case class ContractsRoutes[F[_]: Logger: JsonDecoder: MonadThrow](
    contractsAlgebra: ContractsAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/contracts"

  override val public: HttpRoutes[F] = HttpRoutes.empty

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    // POST /contracts/generate
    case ar @ POST -> Root / "generate" as user =>
      implicit val authedUser: AuthedUser = user
      implicit val language: Language = ar.req.lang
      ar.req.decodeR[GenerateContractInput] { input =>
        contractsAlgebra.generate(input.listingId).flatMap { pdfUrl =>
          Ok(GenerateContractResponse(pdfUrl))
        }
      }
  }
}
