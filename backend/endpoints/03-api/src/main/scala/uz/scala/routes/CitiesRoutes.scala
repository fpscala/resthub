package uz.scala.routes

import cats.MonadThrow
import cats.implicits._
import io.estatico.newtype.ops.toCoercibleIdOps
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.typelevel.log4cats.Logger

import uz.scala.Language
import uz.scala.ObjectId
import uz.scala.SuccessResult
import uz.scala.algebras.CitiesAlgebra
import uz.scala.domain.AuthedUser
import uz.scala.domain.CityId
import uz.scala.domain.cities.CityInput
import uz.scala.domain.enums.Privilege
import uz.scala.domain.users.Role
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes
import uz.scala.shared.ResponseMessages.CITY_CREATED
import uz.scala.shared.ResponseMessages.CITY_DELETED
import uz.scala.shared.ResponseMessages.CITY_UPDATED
import uz.scala.syntax.all.coercibleEncoder

final case class CitiesRoutes[F[_]: Logger: JsonDecoder: MonadThrow](
    cities: CitiesAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/cities"

  override val public: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      cities.getAll.flatMap { cityList =>
        val cityNames = cityList.map(_.name)
        Ok(cityNames)
      }
  }

  override val `private`: org.http4s.AuthedRoutes[AuthedUser, F] = org.http4s.AuthedRoutes.of {
    case ar @ POST -> Root as user =>
      implicit val language: Language = ar.req.lang
      implicit val role: Role = user.role
      authorize[F](Privilege.CreateCity) {
        ar.req.decodeR[CityInput] { cityInput =>
          cities
            .create(cityInput)
            .flatMap(cityId => Created(ObjectId(cityId, CITY_CREATED(language).some)))
        }
      }

    case ar @ PUT -> Root / UUIDVar(id) as user =>
      implicit val language: Language = ar.req.lang
      implicit val role: Role = user.role
      authorize[F](Privilege.UpdateCity) {
        ar.req.decodeR[CityInput] { cityInput =>
          cities
            .update(id.coerce[CityId], cityInput)
            .flatMap(_ => Ok(SuccessResult(CITY_UPDATED(language))))
        }
      }

    case ar @ DELETE -> Root / UUIDVar(id) as user =>
      implicit val language: Language = ar.req.lang
      implicit val role: Role = user.role
      authorize[F](Privilege.DeleteCity) {
        cities
          .delete(id.coerce[CityId])
          .flatMap(_ => Ok(SuccessResult(CITY_DELETED(language))))
      }
  }
}
