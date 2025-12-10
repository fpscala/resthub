package uz.scala.sttp

import cats.Applicative
import cats.MonadError
import cats.Traverse
import cats.syntax.all._
import io.circe.Decoder
import sttp.client3._

/** Entity that controls process of decoding HTTP response [[H]] to output type [[A]]
  * @tparam F - effect type
  */
trait SttpResponseDecoder[F[_], H, A] {
  def apply(response: Response[H]): F[A]
}

object SttpResponseDecoder {
  type CirceJson[F[_], A] = CirceJsonM[cats.Id, F, A]
  type CirceJsonM[M[_], F[_], A] = SttpResponseDecoder[F, CirceJsonResponseM[M], M[A]]
  type StringBody[F[_]] = SttpResponseDecoder[F, StringResponse, String]
  implicit def simpleCirceSttpResponseMDecoder[M[_]: Traverse, F[_], A: Decoder](
      implicit
      ev: MonadError[F, Throwable]
    ): SttpResponseDecoder.CirceJsonM[M, F, A] = response =>
    ev.fromEither(
      for {
        body <- response.body.leftMap {
          case HttpError(body, statusCode) => SttpError(statusCode, body)
          case DeserializationException(body, error) =>
            new Exception(
              s"DeserializationException: ${error.getMessage}.\nStatus: ${response.statusText}.\n Body $body"
            )
        }
        result <- body.traverse(_.as[A]).leftMap { parsingError =>
          val message =
            s"""
               |Http JSON Parsing Error: ${parsingError.message}
               |Raw object: ${body.map(_.spaces2).mkString_("\n")}
               |""".stripMargin
          new Throwable(parsingError.copy(message = message))
        }
      } yield result
    )
  implicit def simpleStringSttpResponseDecoder[F[_]](
      implicit
      ev: MonadError[F, Throwable]
    ): SttpResponseDecoder.StringBody[F] = response =>
    ev.fromEither(response.body.leftMap(new Throwable(_)))

  implicit def circeSttpNoResponseDecoder[
      F[_]: Applicative
    ]: SttpResponseDecoder.CirceJson[F, Unit] =
    _ => Applicative[F].unit

  implicit def circeSttpResponseDecoder[F[_], A: Decoder](
      implicit
      ev: MonadError[F, Throwable]
    ): SttpResponseDecoder.CirceJson[F, A] =
    simpleCirceSttpResponseMDecoder[cats.Id, F, A]
}
