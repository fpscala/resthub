package uz.scala.services

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO

import cats.effect.Async
import cats.effect.Sync
import cats.implicits._
import fs2.Stream

import uz.scala.aws.s3.S3Client
import uz.scala.domain.FileUpload
import uz.scala.effects.GenUUID

trait FileStorageService[F[_]] {
  def uploadFile(fileUpload: FileUpload): F[String]
  def uploadPublicFile(fileUpload: FileUpload): F[String]
  def generateThumbnail(
      fileUpload: FileUpload,
      width: Int,
      height: Int,
    ): F[FileUpload]
  def deleteFile(key: String): F[Unit]
  def getFileUrl(key: String): F[URL]
  def getPublicFileUrl(key: String): F[String]
}

object FileStorageService {
  // CRITICAL: Detect content type from file bytes and filename to ensure proper Content-Type headers
  private def detectContentType(bytes: Array[Byte], filename: String): String = {
    // First try file extension
    val extension = filename.toLowerCase.split('.').lastOption.getOrElse("")

    extension match {
      case "jpg" | "jpeg" => "image/jpeg"
      case "png" => "image/png"
      case "gif" => "image/gif"
      case "webp" => "image/webp"
      case "bmp" => "image/bmp"
      case "svg" => "image/svg+xml"
      case "pdf" => "application/pdf"
      case _ =>
        // Fallback to magic byte detection
        if (bytes.length >= 4) {
          val header = bytes.take(4)
          if (java.util.Arrays.equals(header, Array[Byte](0xFF.toByte, 0xD8.toByte, 0xFF.toByte, 0xE0.toByte)) ||
              java.util.Arrays.equals(header.take(2), Array[Byte](0xFF.toByte, 0xD8.toByte))) {
            "image/jpeg"
          } else if (java.util.Arrays.equals(header, Array[Byte](0x89.toByte, 0x50.toByte, 0x4E.toByte, 0x47.toByte))) {
            "image/png"
          } else if (java.util.Arrays.equals(header.take(2), Array[Byte](0x42.toByte, 0x4D.toByte))) {
            "image/bmp"
          } else if (java.util.Arrays.equals(header.take(4), Array[Byte](0x47.toByte, 0x49.toByte, 0x46.toByte, 0x38.toByte))) {
            "image/gif"
          } else if (java.util.Arrays.equals(header.take(4), Array[Byte](0x52.toByte, 0x49.toByte, 0x46.toByte, 0x46.toByte))) {
            "image/webp"
          } else {
            "application/octet-stream"
          }
        } else {
          "application/octet-stream"
        }
    }
  }

  def make[F[_]: Async: GenUUID](s3Client: S3Client[F]): FileStorageService[F] =
    new FileStorageService[F] {
      override def uploadFile(fileUpload: FileUpload): F[String] =
        for {
          key <- generateFileKey(fileUpload.filename)
          _ <- Stream
            .emits(fileUpload.content)
            .through(s3Client.putObject(key))
            .compile
            .drain
          url <- s3Client.generateUrl(key)
        } yield url.toString

      override def uploadPublicFile(fileUpload: FileUpload): F[String] =
        for {
          key <- generateFileKey(fileUpload.filename)
          // CRITICAL: Detect content type for proper Content-Type headers
          contentType = detectContentType(fileUpload.content, fileUpload.filename)
          _ <- Stream
            .emits(fileUpload.content)
            .through(s3Client.putObjectPublicWithContentType(key, fileUpload.content.length.toLong, contentType))
            .compile
            .drain
          url <- s3Client.generatePublicUrl(key)
        } yield url

      override def generateThumbnail(
          fileUpload: FileUpload,
          width: Int,
          height: Int,
        ): F[FileUpload] =
        Sync[F].delay {
          val inputStream = new ByteArrayInputStream(fileUpload.content)
          val originalImage = ImageIO.read(inputStream)

          // Calculate dimensions while maintaining aspect ratio
          val (targetWidth, targetHeight) =
            calculateDimensions(originalImage.getWidth, originalImage.getHeight, width, height)

          // Create thumbnail
          val thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
          val graphics = thumbnail.createGraphics()

          // Set rendering hints for better quality
          graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR,
          )
          graphics.setRenderingHint(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY,
          )
          graphics.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON,
          )

          // Draw the scaled image
          graphics.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null)
          graphics.dispose()

          // Convert to byte array
          val outputStream = new ByteArrayOutputStream()
          val formatName = getImageFormat(fileUpload.filename)
          ImageIO.write(thumbnail, formatName, outputStream)

          // Create new FileUpload with thumbnail
          val thumbnailFilename = addThumbnailSuffix(fileUpload.filename, width, height)
          FileUpload(
            thumbnailFilename,
            fileUpload.contentType,
            outputStream.toByteArray,
          )
        }

      override def deleteFile(key: String): F[Unit] =
        s3Client.deleteObject(key).compile.drain

      override def getFileUrl(key: String): F[URL] =
        s3Client.generateUrl(key)

      override def getPublicFileUrl(key: String): F[String] =
        s3Client.generatePublicUrl(key)

      // Helper methods
      private def generateFileKey(filename: String): F[String] =
        for {
          uuid <- GenUUID[F].make
          timestamp = System.currentTimeMillis()
          extension = filename.substring(filename.lastIndexOf(".") + 1)
        } yield s"products/${uuid.toString}/$timestamp.$extension"

      private def calculateDimensions(
          originalWidth: Int,
          originalHeight: Int,
          targetWidth: Int,
          targetHeight: Int,
        ): (Int, Int) = {
        val widthRatio = targetWidth.toDouble / originalWidth
        val heightRatio = targetHeight.toDouble / originalHeight
        val ratio = Math.min(widthRatio, heightRatio)

        val newWidth = (originalWidth * ratio).toInt
        val newHeight = (originalHeight * ratio).toInt

        (newWidth, newHeight)
      }

      private def getImageFormat(filename: String): String = {
        val extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase
        extension match {
          case "jpg" | "jpeg" => "jpeg"
          case "png" => "png"
          case "gif" => "gif"
          case "webp" => "webp"
          case _ => "jpeg" // Default to JPEG
        }
      }

      private def addThumbnailSuffix(
          filename: String,
          width: Int,
          height: Int,
        ): String = {
        val dotIndex = filename.lastIndexOf(".")
        if (dotIndex > 0) {
          val name = filename.substring(0, dotIndex)
          val extension = filename.substring(dotIndex)
          s"${name}_thumb_${width}x$height$extension"
        }
        else
          s"${filename}_thumb_${width}x$height"
      }
    }
}
