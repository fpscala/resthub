package uz.scala.aws.s3

import java.net.URL

import cats.effect.Async
import cats.effect.Resource
import cats.effect.Sync
import cats.implicits._
import fs2._
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.async.{AsyncRequestBody, AsyncResponseTransformer}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model._

/** S3 client for file upload and download operations */
trait S3Client[F[_]] {
  /** Upload file with private ACL */
  def putObject(key: String): Pipe[F, Byte, Unit]

  /** Upload file with public read ACL */
  def putObjectPublic(key: String, fileSize: Long): Pipe[F, Byte, Unit]

  /** Download file */
  def downloadObject(key: String): Stream[F, Byte]

  /** Delete file */
  def deleteObject(key: String): Stream[F, Unit]

  /** Generate public URL for a file */
  def generateUrl(key: String): F[URL]

  /** Generate public URL as string */
  def generatePublicUrl(key: String): F[String]

  /** Generate presigned URL (for temporary access) */
  def generatePresignedUrl(key: String, publicRead: Boolean = false): F[URL]

  /** Set bucket policy */
  def setBucketPolicy(policy: String): F[Unit]
}

object S3Client {
  def resource[F[_]: Async](awsConfig: AWSConfig): Resource[F, S3Client[F]] =
    Resource
      .make(
        Sync[F].delay {
          S3AsyncClient
            .builder()
            .region(Region.of(awsConfig.signingRegion.value))
            .credentialsProvider(
              StaticCredentialsProvider.create(
                AwsBasicCredentials.create(awsConfig.accessKey.value, awsConfig.secretKey.value)
              )
            )
            .endpointOverride(java.net.URI.create(awsConfig.serviceEndpoint.value))
            .forcePathStyle(true)
            .build()
        }
      )(client => Sync[F].delay(client.close()))
      .map(new S3ClientImpl[F](awsConfig, _))

  private class S3ClientImpl[F[_]: Async](
      awsConfig: AWSConfig,
      s3: S3AsyncClient,
  ) extends S3Client[F] {

    override def putObject(key: String): Pipe[F, Byte, Unit] =
      uploadWithAcl(key, ObjectCannedACL.PRIVATE)

    override def putObjectPublic(key: String, fileSize: Long): Pipe[F, Byte, Unit] =
      uploadWithAcl(key, ObjectCannedACL.PUBLIC_READ)

    override def downloadObject(key: String): Stream[F, Byte] =
      Stream
        .eval(
          Async[F].fromCompletableFuture(
            Sync[F].delay(
              s3.getObject(
                GetObjectRequest
                  .builder()
                  .bucket(awsConfig.bucketName.value)
                  .key(key)
                  .build(),
                AsyncResponseTransformer.toBytes[GetObjectResponse](),
              )
            )
          )
        )
        .flatMap(response => Stream.emits(response.asByteArray()))

    override def deleteObject(key: String): Stream[F, Unit] =
      Stream.eval(
        Async[F].fromCompletableFuture(
          Sync[F].delay(
            s3.deleteObject(
              DeleteObjectRequest
                .builder()
                .bucket(awsConfig.bucketName.value)
                .key(key)
                .build()
            )
          )
        )
      ).void

    override def generateUrl(key: String): F[URL] =
      Sync[F].delay {
        new java.net.URL(s"${awsConfig.serviceEndpoint.value}/${awsConfig.bucketName.value}/$key")
      }

    override def generatePublicUrl(key: String): F[String] =
      Sync[F].delay {
        val baseUrl = awsConfig.serviceEndpoint.value.stripSuffix("/")
        s"$baseUrl/${awsConfig.bucketName.value}/$key"
      }

    override def generatePresignedUrl(key: String, publicRead: Boolean = false): F[URL] =
      // For MinIO/local development, we use direct URL
      // In production with AWS S3, you would use presigner
      generateUrl(key)

    override def setBucketPolicy(policy: String): F[Unit] =
      Async[F]
        .fromCompletableFuture(
          Sync[F].delay(
            s3.putBucketPolicy(
              PutBucketPolicyRequest
                .builder()
                .bucket(awsConfig.bucketName.value)
                .policy(policy)
                .build()
            )
          )
        )
        .void

    // Helper method to upload with specified ACL
    private def uploadWithAcl(key: String, acl: ObjectCannedACL): Pipe[F, Byte, Unit] =
      (stream: Stream[F, Byte]) =>
        Stream.eval(
          stream.compile.to(Array).flatMap { bytes =>
            Async[F].fromCompletableFuture(
              Sync[F].delay(
                s3.putObject(
                  PutObjectRequest
                    .builder()
                    .bucket(awsConfig.bucketName.value)
                    .key(key)
                    .acl(acl)
                    .contentLength(bytes.length.toLong)
                    .build(),
                  AsyncRequestBody.fromBytes(bytes),
                )
              )
            )
          }
        ).void
  }
}
