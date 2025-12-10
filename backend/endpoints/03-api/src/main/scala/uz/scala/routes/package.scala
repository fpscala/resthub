package uz.scala

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s.Response
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher

import uz.scala.domain.enums.Privilege
import uz.scala.domain.users.Role
import uz.scala.shared.ResponseMessages.INSUFFICIENT_PRIVILEGES
import uz.scala.syntax.all.validationOps
import uz.scala.validation.Rule
import uz.scala.validation.createRule

package object routes {
  def authorize[F[_]: MonadThrow](
      privilege: Privilege,
      privileges: Privilege*
    )(
      handle: => F[Response[F]]
    )(implicit
      role: Role,
      lang: Language,
    ): F[Response[F]] = {
    implicit val rules: Rule[Role] =
      createRule[Role](INSUFFICIENT_PRIVILEGES(lang))(
        _.privileges.exists(::(privilege, privileges.toList).contains)
      )
    role.checkAuth[F].flatMap(_ => handle)
  }
  object LimitQueryParam extends OptionalQueryParamDecoderMatcher[Int]("limit")
  object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
}
