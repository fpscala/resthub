package uz.scala.algebras

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps

import uz.scala.aws.s3.S3Client
import uz.scala.domain.AssetUploadResponse
import uz.scala.domain.FileMeta
import uz.scala.effects.GenUUID

trait AssetsAlgebra[F[_]] {
  def create(meta: FileMeta[F]): F[AssetUploadResponse]
  def initializeBucket(): F[Unit]
}
object AssetsAlgebra {
  def make[F[_]: MonadThrow: GenUUID: Lambda[M[_] => fs2.Compiler[M, M]]](
      s3Client: S3Client[F]
    ): AssetsAlgebra[F] =
    new AssetsAlgebra[F] {
      override def create(meta: FileMeta[F]): F[AssetUploadResponse] =
        for {
          id <- GenUUID[F].make
          key <- genFileKey(meta.fileName, meta.folder)
          _ <- meta.bytes.through(s3Client.putObjectPublic(key, meta.fileSize)).compile.drain
          publicUrl <- s3Client.generatePublicUrl(key)
        } yield AssetUploadResponse(id, publicUrl)

      override def initializeBucket(): F[Unit] = {
        val bucketPolicy = """{
          "Version": "2012-10-17",
          "Statement": [
            {
              "Sid": "PublicReadGetObject",
              "Effect": "Allow",
              "Principal": "*",
              "Action": "s3:GetObject",
              "Resource": "arn:aws:s3:::*/*"
            }
          ]
        }"""
        s3Client.setBucketPolicy(bucketPolicy)
      }

      private def getFileType(filename: String): String = {
        val extension = filename.substring(filename.lastIndexOf('.') + 1)
        extension.toLowerCase
      }

      private def genFileKey(orgFilename: String, folder: Option[String]): F[String] =
        GenUUID[F].make.map { uuid =>
          s"${folder.getOrElse("")}/$uuid.${getFileType(orgFilename)}"
        }
    }
}
