package uz.scala.algebras

import scala.io.Source

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import eu.timepit.refined.types.string.NonEmptyString
import org.typelevel.log4cats.Logger

import uz.scala.Language
import uz.scala.mailer.Mailer
import uz.scala.mailer.data.Content
import uz.scala.mailer.data.Email
import uz.scala.mailer.data.Html
import uz.scala.shared.EmailAddress
import uz.scala.syntax.refined._

trait EmailService[F[_]] {
  def sendActivationEmail(
      email: EmailAddress,
      userName: String,
      token: String,
    )(implicit
      lang: Language
    ): F[Unit]
}

object EmailService {
  def make[F[_]: Sync: Logger](
      mailer: Mailer[F],
      frontendBaseUrl: String,
      activationPath: String,
    ): EmailService[F] =
    new Impl[F](mailer, frontendBaseUrl, activationPath)

  private class Impl[F[_]: Sync: Logger](
      mailer: Mailer[F],
      frontendBaseUrl: String,
      activationPath: String,
    ) extends EmailService[F] {
    private def loadTemplate(templateName: String): F[String] =
      Sync[F].delay {
        val stream = getClass.getResourceAsStream(s"/email-templates/$templateName")
        if (stream == null)
          throw new RuntimeException(s"Email template not found: $templateName")
        try
          Source.fromInputStream(stream, "UTF-8").mkString
        finally
          Option(stream).foreach(_.close())
      }

    private def replaceVariables(
        template: String,
        variables: Map[String, String],
      ): String =
      variables.foldLeft(template) {
        case (tmpl, (key, value)) =>
          tmpl.replace(s"{{$key}}", value)
      }

    override def sendActivationEmail(
        email: EmailAddress,
        userName: String,
        token: String,
      )(implicit
        lang: Language
      ): F[Unit] = {
      val templateName = lang match {
        case Language.Uz => "user-verification-uz.html"
        case Language.En => "user-verification-en.html"
        case Language.Ru => "user-verification-ru.html"
      }

      val activationLink = s"$frontendBaseUrl$activationPath/$token"

      val subject = lang match {
        case Language.Uz => "Email manzilingizni tasdiqlang"
        case Language.En => "Verify your email address"
        case Language.Ru => "Подтвердите свой email адрес"
      }

      for {
        _ <- Logger[F].info(s"Sending activation email to ${email.value}")

        template <- loadTemplate(templateName)

        htmlContent = replaceVariables(
          template,
          Map(
            "userName" -> userName,
            "email" -> email.value,
            "verificationLink" -> activationLink,
            "subject" -> subject,
          ),
        )

        emailMessage = Email(
          subject = NonEmptyString.unsafeFrom(subject),
          content = Content(
            html = Some(Html(NonEmptyString.unsafeFrom(htmlContent)))
          ),
          to = NonEmptyList.one(email),
        )

        _ <- mailer.send(emailMessage)
        _ <- Logger[F].info(s"Activation email sent to ${email.value}")
      } yield ()
    }
  }
}
