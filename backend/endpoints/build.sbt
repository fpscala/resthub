import Dependencies.dev

name := "endpoints"

lazy val `endpoints-domain` = project
  .in(file("00-domain"))
  .dependsOn(
    LocalProject("common"),
    LocalProject("support_services"),
    LocalProject("support_kafka"),
  )

lazy val `endpoints-repos` =
  project
    .in(file("01-repos"))
    .settings(
      libraryDependencies ++=
        Seq(
          Dependencies.uz.scala.doobie
        )
    )
    .dependsOn(
      `endpoints-domain`
    )

lazy val `endpoints-core` =
  project
    .in(file("02-core"))
    .settings(
      libraryDependencies ++=
          Dependencies.org.apache.pdfbox.all ++
          Seq(
            dev.profunktor.`http4s-jwt-auth`
          )
    )
    .dependsOn(
      `endpoints-repos`,
      LocalProject("support_redis"),
      LocalProject("support_mailer"),
      LocalProject("support_kafka"),
      LocalProject("integration_aws-s3"),
    )

lazy val `endpoints-api` =
  project
    .in(file("03-api"))
    .dependsOn(
      `endpoints-core`
    )

lazy val `endpoints-jobs` =
  project
    .in(file("03-jobs"))
    .dependsOn(
      `endpoints-core`,
      LocalProject("support_jobs"),
    )

lazy val `endpoints-server` =
  project
    .in(file("04-server"))
    .dependsOn(`endpoints-api`)

lazy val `endpoints-runner` =
  project
    .in(file("05-runner"))
    .settings(
      libraryDependencies ++= Seq(
        Dependencies.uz.scala.flyway
      )
    )
    .dependsOn(
      `endpoints-server`,
      `endpoints-jobs`,
    )
    .settings(DockerImagePlugin.serviceSetting("endpoints"))
    .enablePlugins(DockerImagePlugin, JavaAppPackaging, DockerPlugin)

aggregateProjects(
  `endpoints-domain`,
  `endpoints-repos`,
  `endpoints-core`,
  `endpoints-api`,
  `endpoints-server`,
  `endpoints-runner`,
)
