import Dependencies.*

name := "jobs"

libraryDependencies ++= Seq(
  com.github.cron4s,
  eu.timepit.cron4s,
)

dependsOn(LocalProject("common"))
