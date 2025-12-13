package uz.scala.routes

import java.util.UUID

import cats.MonadThrow
import cats.implicits._
import org.http4s.AuthedRoutes
import org.http4s.HttpRoutes
import org.http4s.QueryParamDecoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.typelevel.log4cats.Logger

import uz.scala.Language
import uz.scala.SuccessResult
import uz.scala.algebras.ListingsAlgebra
import uz.scala.domain.AuthedUser
import uz.scala.domain.ListingId
import uz.scala.domain.enums.ListingStatus
import uz.scala.domain.listings.CreateListingInput
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes
import uz.scala.shared.ResponseMessages._
import uz.scala.syntax.refined._

final case class ListingsRoutes[F[_]: Logger: JsonDecoder: MonadThrow](
    listingsAlgebra: ListingsAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/listings"

  override val public: HttpRoutes[F] =
    HttpRoutes.of[F] {
      // GET /listings?city=...&minPrice=...&maxPrice=...&page=1&size=20
      case GET -> Root :? CityQueryParam(city) +&
           MinPriceQueryParam(minPrice) +&
           MaxPriceQueryParam(maxPrice) +&
           OffsetQueryParam(page) +&
           LimitQueryParam(size) =>
        val filters = uz
          .scala
          .domain
          .listings
          .ListingFilters(
            city = city,
            minPrice = minPrice,
            maxPrice = maxPrice,
            status = Some(ListingStatus.Approved), // Public search only shows APPROVED
            page = page,
            size = size,
          )
        listingsAlgebra.search(filters).flatMap(Ok(_))

      // GET /listings/:id
      case req @ GET -> Root / UUIDVar(id) =>
        implicit val language: Language = req.lang
        listingsAlgebra.findById(ListingId(id)).flatMap(Ok(_))
    }

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    // POST /listings
    case ar @ POST -> Root as user =>
      implicit val authedUser: AuthedUser = user
      implicit val language: Language = ar.req.lang
      ar.req.decodeR[CreateListingInput] { input =>
        listingsAlgebra.create(input).flatMap { listingId =>
          Created(SuccessResult(LISTING_CREATED(language)))
        }
      }

    // GET /listings/my
    case GET -> Root / "my" as user =>
      implicit val authedUser: AuthedUser = user
      listingsAlgebra.myListings.flatMap(Ok(_))

    // DELETE /listings/:id
    case ar @ DELETE -> Root / UUIDVar(id) as user =>
      implicit val authedUser: AuthedUser = user
      implicit val language: Language = ar.req.lang
      listingsAlgebra.delete(ListingId(id)) *> Ok(SuccessResult(LISTING_DELETED(language)))
  }
}
