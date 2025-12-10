package uz.scala.mailer.data

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

import Props._
import eu.timepit.refined.types.net.SystemPortNumber
import eu.timepit.refined.types.string.NonEmptyString
import types.Protocol.Smtp

final case class Props(values: Map[String, String]) {
  def withSmtpAddress(host: NonEmptyString, port: SystemPortNumber): Props =
    copy(values = values ++ Map(SmtpHostKey -> host.value, SmtpPortKey -> port.value.toString))

  def setConnectionTimeout(timeout: FiniteDuration): Props =
    copy(values = values ++ Map(SmtpConnectionTimeoutKey -> timeout.toMillis.toString))

  def setSmtpTimeout(timeout: FiniteDuration): Props =
    copy(values = values ++ Map(SmtpTimeoutKey -> timeout.toMillis.toString))

  def withTls(enable: Boolean = true, required: Boolean = false): Props =
    copy(values =
      values ++ Map(
        SmtpStartTlsEnableKey -> enable.toString,
        SmtpStartTlsRequiredKey -> required.toString,
      )
    )

  def setProtocol(protocol: NonEmptyString): Props =
    copy(values = values ++ Map(TransportProtocolKey -> protocol.value))

  def withDebug(debug: Boolean = false): Props =
    copy(values = values ++ Map(DebugKey -> debug.toString))

  def withAuth(enable: Boolean = true): Props =
    copy(values = values ++ Map(SmtpAuthKey -> enable.toString))

  def set(key: String, value: String): Props =
    copy(values = values ++ Map(key -> value))
}

object Props {
  val DebugKey = "mail.debug"
  val SmtpConnectionTimeoutKey = "mail.smtp.connectiontimeout"
  val SmtpHostKey = "mail.smtp.host"
  val SmtpPortKey = "mail.smtp.port"
  val SmtpStartTlsEnableKey = "mail.smtp.starttls.enable"
  val SmtpSslProtocolKey = "mail.smtp.ssl.protocols"
  val SmtpStartTlsRequiredKey = "mail.smtp.starttls.required"
  val SmtpTimeoutKey = "mail.smtp.timeout"
  val TransportProtocolKey = "mail.transport.protocol"
  val SmtpAuthKey = "mail.smtp.auth"
  val defaultProps =
    Map(
      SmtpHostKey -> "localhost",
      SmtpPortKey -> "25",
      DebugKey -> "false",
      SmtpConnectionTimeoutKey -> 3.seconds.toMillis.toString,
      SmtpTimeoutKey -> 30.seconds.toMillis.toString,
      SmtpStartTlsEnableKey -> "true",
      SmtpSslProtocolKey -> "TLSv1.2",
      SmtpStartTlsRequiredKey -> "true",
      TransportProtocolKey -> Smtp.value,
      SmtpAuthKey -> "true",
    )

  def default: Props = Props(defaultProps)
}
