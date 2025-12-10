name := "mailer"

libraryDependencies ++= Seq(
  Dependencies.javax.mailer,
  Dependencies.com.github.cb372.retry,
)

dependsOn(LocalProject("common"))
