package uz.scala.mailer.data

import eu.timepit.refined.types.string.NonEmptyString

import uz.scala.syntax.refined._

object types {
  object Subtype {
    val HTML: NonEmptyString = "html"
    val PLAIN: NonEmptyString = "plain"
  }
  object Protocol {
    val Smtp: NonEmptyString = "smtp"
    val Imap: NonEmptyString = "imap"
  }
}
