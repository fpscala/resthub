package uz.scala.mailer.exception

case class InvalidAddress(cause: String) extends Throwable {
  override def getMessage: String = cause
}
