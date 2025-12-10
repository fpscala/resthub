import Dependencies.*

ThisBuild / version := sys.env.getOrElse("IMAGE_TAG", "latest")

ThisBuild / scalaVersion := "2.13.15"

lazy val root = project
  .in(file("."))
  .settings(
    name := "nest-hub"
  )
  .aggregate(
    endpoints,
    supports,
  )

lazy val common =
  project
    .in(file("common"))
    .settings(
      name := "common"
    )
    .settings(
      libraryDependencies ++=
        Dependencies.io.circe.all ++
          eu.timepit.refined.all ++
          com.github.pureconfig.all ++
          com.beachape.enumeratum.all ++
          tf.tofu.derevo.all ++
          Seq(
            uz.scala.common,
            org.typelevel.cats.core,
            org.typelevel.cats.effect,
            org.typelevel.log4cats,
            ch.qos.logback,
            dev.optics.monocle,
            Dependencies.io.estatico.newtype,
            Dependencies.io.github.jmcardon.`tsec-password`,
            Dependencies.io.scalaland.chimney,
          )
    )
    .dependsOn(LocalProject("support_logback"))

lazy val integrations = project
  .in(file("integrations"))
  .settings(
    name := "integrations"
  )

lazy val supports = project
  .in(file("supports"))
  .settings(
    name := "supports"
  )

lazy val endpoints = project
  .in(file("endpoints"))
  .settings(
    name := "endpoints"
  )

addCommandAlias(
  "styleCheck",
  "all scalafmtSbtCheck; scalafmtCheckAll; Test / compile; scalafixAll --check",
)

addCommandAlias(
  "fmtCheck",
  "all scalafmtSbtCheck; scalafmtCheckAll",
)

addCommandAlias("fmtFix", "scalafmtSbt; scalafmtAll")

Global / lintUnusedKeysOnLoad := false
Global / onChangedBuildSource := ReloadOnSourceChanges

val runServer = inputKey[Unit]("Runs server")

runServer := {
  (LocalProject("endpoints-runner") / Compile / run).evaluated
}
