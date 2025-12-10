package uz.scala.mailer.data

/** Appends the given array of bytes as the e-mail message attachment. Useful especially when the
  * original file object is not available, only its array of bytes.
  *
  * @param bytes     array of bytes representing the attachment
  * @param mimeType  ''MIME type'' of the attachment
  * @param name      name of the attachment (optional)
  * @param contentId the "Content-ID" header field of this body part (optional)
  * @param headers   content part headers (''RFC 822'')
  *         content part
  */
case class Attachment(
    bytes: Array[Byte],
    mimeType: String,
    name: Option[String] = None,
    contentId: Option[String] = None,
    headers: Seq[Header] = Seq.empty[Header],
  )
