package uz.scala.routes

import java.util.UUID

import cats.MonadThrow
import cats.implicits._
import org.http4s.AuthedRoutes
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.typelevel.log4cats.Logger

import uz.scala.Language
import uz.scala.SuccessResult
import uz.scala.algebras.AdminListingsAlgebra
import uz.scala.domain.AuthedUser
import uz.scala.domain.ListingId
import uz.scala.domain.enums.ListingStatus
import uz.scala.domain.listings.RejectListingInput
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes
import uz.scala.shared.ResponseMessages._

// Query parameter matcher for status
object StatusQueryParam extends OptionalQueryParamDecoderMatcher[String]("status")

final case class AdminListingsRoutes[F[_]: Logger: JsonDecoder: MonadThrow](
    adminAlgebra: AdminListingsAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/admin/listings"

  override val public: HttpRoutes[F] = HttpRoutes.empty

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    // GET /admin/listings?status=PENDING&page=1&size=20
    case ar @ GET -> Root :? StatusQueryParam(statusStr) +&
         OffsetQueryParam(page) +&
         LimitQueryParam(size) as user =>
      implicit val authedUser: AuthedUser = user
      implicit val language: Language = ar.req.lang

      val status = statusStr.flatMap {
        case "PENDING" => Some(ListingStatus.Pending)
        case "APPROVED" => Some(ListingStatus.Approved)
        case "REJECTED" => Some(ListingStatus.Rejected)
        case _ => None
      }

      val filters = uz
        .scala
        .domain
        .listings
        .ListingFilters(
          city = None,
          minPrice = None,
          maxPrice = None,
          status = status,
          page = page,
          size = size,
        )

      adminAlgebra.getAllListings(filters).flatMap(Ok(_))

    // POST /admin/listings/:id/approve
    case ar @ POST -> Root / UUIDVar(id) / "approve" as user =>
      implicit val authedUser: AuthedUser = user
      implicit val language: Language = ar.req.lang
      adminAlgebra.approve(ListingId(id)) *> Ok(SuccessResult(LISTING_APPROVED(language)))

    // POST /admin/listings/:id/reject
    case ar @ POST -> Root / UUIDVar(id) / "reject" as user =>
      implicit val authedUser: AuthedUser = user
      implicit val language: Language = ar.req.lang
      ar.req.decodeR[RejectListingInput] { input =>
        adminAlgebra.reject(ListingId(id), input.reason.value) *> Ok(
          SuccessResult(LISTING_REJECTED(language))
        )
      }
  }
}
