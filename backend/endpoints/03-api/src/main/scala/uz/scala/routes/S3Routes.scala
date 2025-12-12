package uz.scala.routes

import cats.MonadThrow
import cats.implicits._
import io.circe.generic.JsonCodec
import org.http4s.AuthedRoutes
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.typelevel.log4cats.Logger

import uz.scala.aws.s3.S3Client
import uz.scala.domain.AuthedUser
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.utils.Routes

// Query parameter matcher for S3 key
object KeyQueryParam extends OptionalQueryParamDecoderMatcher[String]("key")

@JsonCodec
case class PresignResponse(
    presignedUrl: String,
    publicUrl: String,
  )

final case class S3Routes[F[_]: Logger: JsonDecoder: MonadThrow](
    s3Client: S3Client[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/s3"

  override val public: HttpRoutes[F] = HttpRoutes.empty

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    // GET /s3/presign?key=uploads/abc123.jpg
    case GET -> Root / "presign" :? KeyQueryParam(keyOpt) as _ =>
      keyOpt match {
        case Some(key) =>
          for {
            presignedUrl <- s3Client.generatePresignedUrl(key, publicRead = true)
            publicUrl <- s3Client.generatePublicUrl(key)
            response = PresignResponse(presignedUrl.toString, publicUrl)
          } yield Ok(response)

        case None =>
          BadRequest("Missing 'key' query parameter")
      }
  }
}
