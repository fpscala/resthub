package uz.scala.mailer

import eu.timepit.refined.types.net.SystemPortNumber
import eu.timepit.refined.types.string.NonEmptyString

import uz.scala.shared.EmailAddress

case class MailerConfig(
    enabled: Boolean,
    host: NonEmptyString,
    port: SystemPortNumber,
    username: NonEmptyString,
    password: NonEmptyString,
    fromAddress: EmailAddress,
    fromName: Option[NonEmptyString],
    recipients: List[EmailAddress],
  )
