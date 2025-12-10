package uz.scala.domain

import io.circe.generic.JsonCodec

@JsonCodec
case class ResponseData[A](
    data: List[A],
    total: Long,
  )

object ResponseData {
  def empty[A]: ResponseData[A] = ResponseData[A](Nil, 0L)
}
