package uz.scala.mailer.data

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

import eu.timepit.refined.types.string.NonEmptyString
import types.Subtype.PLAIN

case class Text(
    value: String,
    charset: Charset = StandardCharsets.UTF_8,
    subtype: NonEmptyString = PLAIN,
    headers: List[Header] = Nil,
  )
