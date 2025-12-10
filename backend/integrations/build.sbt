name := "integrations"
lazy val integration_aws = project.in(file("aws"))

aggregateProjects(
  integration_aws,
)
