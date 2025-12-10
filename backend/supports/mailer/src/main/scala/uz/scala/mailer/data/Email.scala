package uz.scala.mailer.data

import cats.data.NonEmptyList
import eu.timepit.refined.types.string.NonEmptyString

import uz.scala.shared.EmailAddress

/** Represents the e-mail message itself.
  *
  * @param subject
  *   e-mail subject text
  * @param content
  *   e-mail content,
  * @param to
  *   set of e-mail receiver addresses
  * @param from
  *   optional custom sender address (if None, uses config default)
  * @param cc
  *   set of e-mail ''carbon copy'' receiver addresses
  * @param bcc
  *   set of e-mail ''blind carbon copy'' receiver addresses
  * @param replyTo
  *   addresses used to reply this message
  */
case class Email(
    subject: NonEmptyString,
    content: Content,
    to: NonEmptyList[EmailAddress],
    from: Option[EmailAddress] = None,
    cc: List[EmailAddress] = Nil,
    bcc: List[EmailAddress] = Nil,
    replyTo: List[EmailAddress] = Nil,
    headers: List[Header] = Nil,
    attachments: List[Attachment] = Nil,
  )
