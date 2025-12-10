package uz.scala.mailer.data

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

import eu.timepit.refined.types.string.NonEmptyString
import types.Subtype.HTML

case class Html(
    value: NonEmptyString,
    charset: Charset = StandardCharsets.UTF_8,
    subtype: NonEmptyString = HTML,
    headers: List[Header] = Nil,
  )
