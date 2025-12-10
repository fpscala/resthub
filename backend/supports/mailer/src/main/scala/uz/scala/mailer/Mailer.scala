package uz.scala.mailer

import java.util.Properties
import javax.activation.DataHandler
import javax.mail.Authenticator
import javax.mail.Message.RecipientType._
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.DurationLong
import scala.jdk.CollectionConverters.MapHasAsJava

import cats.effect.Async
import cats.effect.Sync
import cats.implicits._
import eu.timepit.refined.cats.refTypeShow
import org.typelevel.log4cats.Logger
import retry.RetryPolicies.exponentialBackoff
import retry.RetryPolicies.limitRetries
import retry.RetryPolicy

import uz.scala.mailer.data.Attachment
import uz.scala.mailer.data.Credentials
import uz.scala.mailer.data.Email
import uz.scala.mailer.data.Html
import uz.scala.mailer.data.Props
import uz.scala.mailer.data.Props.SmtpConnectionTimeoutKey
import uz.scala.mailer.data.Text
import uz.scala.mailer.retries.Retry

trait Mailer[F[_]] {
  def send(email: Email): F[Unit]
}
object Mailer {
  def make[F[_]: Async: Logger](config: MailerConfig): Mailer[F] =
    if (config.enabled)
      new MailerImpl[F](
        config,
        Props.default.withSmtpAddress(config.host, config.port),
        Credentials(config.username, config.password),
      )
    else new NoOpMailerImpl[F](config)

  def default[F[_]: Async: Logger](config: MailerConfig, credentials: Credentials): Mailer[F] =
    if (config.enabled)
      new MailerImpl[F](config, Props.default, credentials)
    else new NoOpMailerImpl[F](config)

  class NoOpMailerImpl[F[_]: Logger](config: MailerConfig) extends Mailer[F] {
    override def send(email: Email): F[Unit] = {
      val fromAddress = email.from.getOrElse(config.fromAddress)
      Logger[F].info(
        s"""Email sent from [ $fromAddress ] to ${email.to.mkString_("[ ", ", ", " ]")}
          ${email.content.text.fold("") { text =>
            s"email text [ \n${text.value}\n ]"
          }}
          ${email.content.html.fold("") { html =>
            s"email html [ \n${html.value}\n ]"
          }}"""
      )
    }
  }

  private class MailerImpl[F[_]: Async](
      config: MailerConfig,
      props: Props,
      credentials: Credentials,
    )(implicit
      logger: Logger[F],
      F: Sync[F],
    ) extends Mailer[F] {
    private val retryPolicy: RetryPolicy[F] = {
      val delay = props.values.get(SmtpConnectionTimeoutKey).fold(1.second)(_.toLong.millis)
      limitRetries[F](2) |+| exponentialBackoff[F](delay)
    }

    private val properties: Properties = {
      val properties = new Properties()
      properties.putAll(props.values.asJava)
      properties
    }

    private val authenticator: F[Authenticator] =
      F.delay(
        new Authenticator {
          override def getPasswordAuthentication: PasswordAuthentication =
            new PasswordAuthentication(credentials.user.value, credentials.password.value)
        }
      )

    private def session(properties: Properties, auth: Authenticator): F[Session] =
      F.delay(Session.getInstance(properties, auth))

    private def prepTextPart(text: Text): MimeBodyPart = {
      val part = new MimeBodyPart()
      part.setText(text.value, text.charset.toString, text.subtype.value)
      text.headers.foreach(header => part.setHeader(header.name, header.value))
      part
    }

    private def prepHtmlPart(html: Html): MimeBodyPart = {
      val part = new MimeBodyPart()
      part.setText(html.value.value, html.charset.toString, html.subtype.value)
      html.headers.foreach(header => part.setHeader(header.name, header.value))
      part
    }

    private def attachBytes(
        attachment: Attachment
      ): MimeBodyPart = {
      val part = new MimeBodyPart()
      attachment.contentId.foreach(part.setContentID)
      part.setDataHandler(
        new DataHandler(new ByteArrayDataSource(attachment.bytes, attachment.mimeType))
      )
      attachment.name.foreach(part.setFileName)
      attachment.headers.foreach(header => part.setHeader(header.name, header.value))
      part
    }

    private def prepareMessage(session: Session, email: Email): MimeMessage = {
      val message = new MimeMessage(session)
      val fromAddress = email.from.getOrElse(config.fromAddress)
      // Set from address with personal name if provided
      val internetAddress = config.fromName match {
        case Some(name) => new InternetAddress(fromAddress.value, name.value, "UTF-8")
        case None => new InternetAddress(fromAddress.value)
      }
      message.setFrom(internetAddress)
      email.to.map(ads => message.addRecipient(TO, new InternetAddress(ads.value)))
      email.cc.foreach(ads => message.addRecipient(CC, new InternetAddress(ads.value)))
      email.bcc.foreach(ads => message.addRecipient(BCC, new InternetAddress(ads.value)))
      message.setSubject(email.subject.value)
      val bodyParts = List(
        email.content.text.map(prepTextPart).toList,
        email.content.html.map(prepHtmlPart).toList,
        email.attachments.map(attachBytes),
      ).flatten
      message.setContent(new MimeMultipart {
        bodyParts.foreach(addBodyPart)
      })
      email.headers.foreach(header => message.setHeader(header.name, header.value))
      message
    }

    override def send(email: Email): F[Unit] = {
      val fromAddress = email.from.getOrElse(config.fromAddress)
      for {
        _ <- Logger[F].info(
          s"Starting sending email: from [$fromAddress] subject [${email.subject}]"
        )
        auth <- authenticator
        session <- session(properties, auth)
        message = prepareMessage(session, email)
        task = F.delay(Transport.send(message))
        _ <- Retry[F].retry(retryPolicy)(task)
        _ <- Logger[F].info(
          s"Finished sending email: from [$fromAddress] subject [${email.subject}]"
        )
      } yield {}
    }
  }
}
