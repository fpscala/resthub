package uz.scala.routes

import cats.effect.Async
import cats.implicits.catsSyntaxOptionId
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.multipart.Multipart

import uz.scala.Language
import uz.scala.Mode
import uz.scala.Mode.Production
import uz.scala.algebras.AssetsAlgebra
import uz.scala.domain.AuthedUser
import uz.scala.domain.FileMeta
import uz.scala.domain.enums.Privilege
import uz.scala.domain.users.Role
import uz.scala.effects.GenUUID
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxPartOps
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes
import uz.scala.shared.ResponseMessages.FILE_NOT_FOUND

final class RootRoutes[F[_]: JsonDecoder: Async: GenUUID](assets: AssetsAlgebra[F])
    extends Routes[F, AuthedUser] {
  override val path: String = "/"
  private val AllowedMediaTypes: List[MediaType] = List(
    MediaType.image.png,
    MediaType.image.jpeg,
  )
  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    // POST /upload - Single file upload (frontend compatibility)
    case ar @ POST -> Root / "upload" as user =>
      implicit val language: Language = ar.req.lang
      implicit val role: Role = user.role
      authorize[F](Privilege.CreateAsset) {
        ar.req.decode[Multipart[F]] { multipart =>
          for {
            textParts <- multipart.parts.textPartMap
            fileParts = multipart.parts.fileParts(AllowedMediaTypes: _*)
            fileMeta = fileParts.headOption.map { fp =>
              FileMeta(
                bytes = fp.body,
                contentType =
                  fp.contentType.map(_.mediaType).map(m => s"${m.mainType}/${m.subType}"),
                fileName = fp.filename.getOrElse(""),
                fileSize = fp.contentLength.getOrElse(0L),
                folder = s"${user.email.value}${textParts.get("type").fold("")("/" + _)}".some,
              )
            }
            res <- fileMeta
              .fold(BadRequest(FILE_NOT_FOUND(language)))(
                assets.create(_).flatMap(Created(_))
              )
          } yield res
        }
      }
  }

  override val public: HttpRoutes[F] = HttpRoutes.of {
    case req @ GET -> Root / "swagger" if Mode.current != Production =>
      StaticFile.fromResource("/swagger/swagger.html", req.some).getOrElseF(NotFound())
    case req @ GET -> Root / "endpoints.yml" if Mode.current != Production =>
      StaticFile.fromResource("/swagger/endpoints.yml", req.some).getOrElseF(NotFound())
  }
}
