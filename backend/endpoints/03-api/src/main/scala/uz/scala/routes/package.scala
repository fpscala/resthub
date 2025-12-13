package uz.scala

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s.QueryParamDecoder
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
  implicit val bigDecimalQueryParamDecoder: QueryParamDecoder[BigDecimal] =
    QueryParamDecoder[String].map(BigDecimal.apply)

  // Query parameter matchers
  object CityQueryParam extends OptionalQueryParamDecoderMatcher[String]("city")
  object MinPriceQueryParam extends OptionalQueryParamDecoderMatcher[BigDecimal]("minPrice")
  object MaxPriceQueryParam extends OptionalQueryParamDecoderMatcher[BigDecimal]("maxPrice")
}
