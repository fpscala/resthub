package uz.scala.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import io.estatico.newtype.ops.toCoercibleIdOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import org.typelevel.log4cats.Logger

import uz.scala.Language
import uz.scala.SuccessResult
import uz.scala.algebras.RolesAlgebra
import uz.scala.algebras.UsersAlgebra
import uz.scala.domain.AuthedUser
import uz.scala.domain.UserId
import uz.scala.domain.enums.Privilege
import uz.scala.domain.users.Role
import uz.scala.domain.users.UpdatePasswordInput
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes
import uz.scala.shared.ResponseMessages.PASSWORD_UPDATED
import uz.scala.shared.ResponseMessages.USER_DELETED

final case class UsersRoutes[F[_]: Logger: JsonDecoder: MonadThrow](
    users: UsersAlgebra[F],
    roles: RolesAlgebra[F],
  ) extends Routes[F, AuthedUser] {
  override val path = "/users"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ DELETE -> Root / UUIDVar(id) as user =>
      implicit val language: Language = ar.req.lang
      implicit val role: Role = user.role
      authorize[F](Privilege.DeleteUser) {
        users
          .delete(id.coerce[UserId])
          .flatMap(_ => Ok(SuccessResult(USER_DELETED(language))))
      }

    case ar @ PUT -> Root / UUIDVar(id) / "password" as user =>
      implicit val language: Language = ar.req.lang
      implicit val role: Role = user.role
      authorize[F](Privilege.UpdateUser) {
        ar.req.decodeR[UpdatePasswordInput] { input =>
          users
            .updatePassword(id.coerce[UserId], input)
            .flatMap(_ => Ok(SuccessResult(PASSWORD_UPDATED(language))))
        }
      }
  }
}
