package uz.scala.domain

import io.circe.generic.JsonCodec

@JsonCodec
case class FileUpload(
    filename: String,
    contentType: String,
    content: Array[Byte],
  ) {
  // Override equals and hashCode to handle Array[Byte] properly
  override def equals(obj: Any): Boolean = obj match {
    case that: FileUpload =>
      this.filename == that.filename &&
      this.contentType == that.contentType &&
      java.util.Arrays.equals(this.content, that.content)
    case _ => false
  }

  override def hashCode(): Int = {
    val prime = 31
    var result = 1
    result = prime * result + filename.hashCode
    result = prime * result + contentType.hashCode
    result = prime * result + java.util.Arrays.hashCode(content)
    result
  }
}
