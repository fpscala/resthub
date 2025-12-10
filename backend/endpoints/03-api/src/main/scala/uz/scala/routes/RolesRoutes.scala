package uz.scala.routes

import cats.MonadThrow
import cats.implicits.catsSyntaxOptionId
import cats.implicits.toFlatMapOps
import io.estatico.newtype.ops.toCoercibleIdOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import org.typelevel.log4cats.Logger

import uz.scala.Language
import uz.scala.ObjectId
import uz.scala.SuccessResult
import uz.scala.algebras.RolesAlgebra
import uz.scala.domain.AuthedUser
import uz.scala.domain.RoleId
import uz.scala.domain.enums.Privilege
import uz.scala.domain.users.Role
import uz.scala.domain.users.RoleInput
import uz.scala.domain.users.RoleUpdateInput
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes
import uz.scala.shared.ResponseMessages.ROLE_CREATED
import uz.scala.shared.ResponseMessages.ROLE_DELETED
import uz.scala.shared.ResponseMessages.ROLE_UPDATED
import uz.scala.syntax.all.coercibleEncoder

final case class RolesRoutes[F[_]: Logger: JsonDecoder: MonadThrow](
    roles: RolesAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/roles"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ GET -> Root as user =>
      implicit val language: Language = ar.req.lang
      implicit val role: Role = user.role
      authorize[F](Privilege.ViewRoles) {
        roles.getAll.flatMap(Ok(_))
      }

    case ar @ POST -> Root as user =>
      implicit val language: Language = ar.req.lang
      implicit val role: Role = user.role
      authorize[F](Privilege.CreateRole) {
        ar.req.decodeR[RoleInput] { input =>
          roles
            .createRole(input)
            .flatMap(id => Created(ObjectId(id, ROLE_CREATED(language).some)))
        }
      }

    case ar @ PUT -> Root / UUIDVar(roleId) as user =>
      implicit val language: Language = ar.req.lang
      implicit val role: Role = user.role
      authorize[F](Privilege.UpdateRole) {
        ar.req.decodeR[RoleUpdateInput] { input =>
          roles
            .updateRole(roleId.coerce[RoleId], input)
            .flatMap(_ => Ok(SuccessResult(ROLE_UPDATED(language))))
        }
      }

    case ar @ DELETE -> Root / UUIDVar(roleId) as user =>
      implicit val language: Language = ar.req.lang
      implicit val role: Role = user.role
      authorize[F](Privilege.DeleteRole) {
        roles
          .deleteRole(roleId.coerce[RoleId])
          .flatMap(_ => Ok(SuccessResult(ROLE_DELETED(language))))
      }

    case ar @ GET -> Root / "privileges" as user =>
      implicit val language: Language = ar.req.lang
      implicit val role: Role = user.role
      authorize[F](Privilege.ViewRoles) {
        Ok(Privilege.groupedValues)
      }
  }
}
