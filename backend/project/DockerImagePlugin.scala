import com.typesafe.sbt.packager.Keys.*
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker.DockerChmodType
import com.typesafe.sbt.packager.docker.DockerPlugin
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.dockerChmodType
import sbt.*
import sbt.Keys.*

object DockerImagePlugin extends AutoPlugin {
  object autoImport {
    lazy val generateServiceImage: TaskKey[Unit] =
      taskKey[Unit]("Generates an image with the native binary")
  }
  val AppEnv: String = sys.env.getOrElse("APP_ENV", "staging")

  override def projectSettings: Seq[Def.Setting[?]] =
    Seq(
      dockerBaseImage := "base-image:1.0",
      dockerExposedPorts ++= Seq(8000),
      dockerExposedUdpPorts += 4444,
      dockerUpdateLatest := true,
      dockerChmodType    := DockerChmodType.UserGroupWriteExecute,
    )

  def serviceSetting(serviceName: String): Seq[Def.Setting[?]] =
    Seq(
      Docker / packageName         := s"fpscala/shop-$serviceName-$AppEnv",
      packageDoc / publishArtifact := false,
      packageSrc / publishArtifact := true,
      publish / skip               := false,
    )

  override def requires: sbt.Plugins =
    JavaAppPackaging && DockerPlugin
}
