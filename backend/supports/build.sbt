name := "supports"

lazy val support_services = project.in(file("services"))
lazy val support_logback = project.in(file("logback"))
lazy val support_redis = project.in(file("redis"))
lazy val support_sttp = project.in(file("sttp"))
lazy val support_jobs = project.in(file("jobs"))
lazy val support_mailer = project.in(file("mailer"))
lazy val support_kafka = project.in(file("kafka"))

aggregateProjects(
  support_services,
  support_logback,
  support_redis,
  support_sttp,
  support_jobs,
  support_mailer,
  support_kafka,
)
