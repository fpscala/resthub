package uz.scala.setup

import uz.scala.auth.AuthConfig
import uz.scala.aws.s3.AWSConfig
import uz.scala.doobie.DataBaseConfig
import uz.scala.flyway.MigrationsConfig
import uz.scala.http4s.HttpServerConfig
import uz.scala.jobs.JobsRunnerConfig
import uz.scala.kafka.KafkaConfig
import uz.scala.mailer.MailerConfig
import uz.scala.redis.RedisConfig

case class Config(
    http: HttpServerConfig,
    auth: AuthConfig,
    redis: RedisConfig,
    awsConfig: AWSConfig,
    database: DataBaseConfig,
    jobs: JobsRunnerConfig,
    mailer: MailerConfig,
    frontend: FrontendConfig,
    kafka: KafkaConfig,
  ) {
  lazy val migrations: MigrationsConfig = MigrationsConfig(
    hostname = database.host.value,
    port = database.port.value,
    database = database.database.value,
    username = database.user.value,
    password = database.password.value,
    schema = database.schema.fold("public")(_.value),
    location = "db/migration",
  )
}
