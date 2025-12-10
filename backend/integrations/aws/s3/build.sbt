name := "s3"

libraryDependencies ++=
  Dependencies.com.amazonaws.all ++
    Dependencies.co.fs2.all

dependsOn(LocalProject("common"))
