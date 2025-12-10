import Dependencies._

name := "support_kafka"

libraryDependencies ++=
  Seq(
    com.github.fd4s.`fs2-kafka`,
    org.typelevel.cats.core,
    org.typelevel.cats.effect,
    org.typelevel.log4cats,
  ) ++
    Dependencies.io.circe.all ++
    com.github.pureconfig.all
