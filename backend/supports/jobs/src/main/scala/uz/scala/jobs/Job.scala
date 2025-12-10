package uz.scala.jobs

import scala.concurrent.duration.FiniteDuration
import scala.reflect.runtime.universe
import scala.util.Try

import cats.effect.Sync
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import shapeless.Typeable
import shapeless.syntax.typeable._
/** Definition of job, which consists of:
  *  - name of the job
  *  - logger, which will be used to log all the internal details of the job
  *  - run method, the code that will be executed on each job run
  */
sealed trait Job[F[_], Env] {
  def name: String
  def logger: Logger[F]

  def run(implicit env: Env): F[Unit]
}

object Job {

  /** Loads Job object from class path.
    * NOT typesafe. If it fails to find object on class path will return None, but in cases if
    * job exists, but requires different Env to be run, it will throw exception in runtime during job execution.
    */
  def fromClassPath[F[_], Env](obj: String): Option[Job[F, Env]] =
    Try {
      val mirror = universe.runtimeMirror(getClass.getClassLoader)
      val module = mirror.staticModule(obj)
      val instance = mirror.reflectModule(module).instance
      instance.asInstanceOf[Job[F, Env]]
    }.toOption

  abstract class AutoName[F[_]: Sync] { this: Job[F, _] =>
    lazy val name: String = this.getClass.getSimpleName.filterNot(_ == '$')
    implicit lazy val logger: Logger[F] = Slf4jLogger
      .getLoggerFromClass[F](this.getClass)
      .withModifiedString(_.prependedAll(s"[$name] "))
  }
}

trait PeriodicJob[F[_], Env] extends Job[F, Env] {
  def interval: FiniteDuration
}

object PeriodicJob {
  def fromClassPath[F[_], Env](obj: String): Option[PeriodicJob[F, Env]] =
    Job.fromClassPath[F, Env](obj).flatMap(_.narrowTo[PeriodicJob[F, Env]])

  implicit def periodicJobTypeable[F[_], Env]: Typeable[PeriodicJob[F, Env]] =
    Typeable.simpleTypeable(classOf[PeriodicJob[F, Env]])
}

/** Job interval should be provided through config */
trait CronJob[F[_], Env] extends Job[F, Env]

object CronJob {
  def fromClassPath[F[_], Env](obj: String): Option[CronJob[F, Env]] =
    Job.fromClassPath[F, Env](obj).flatMap(_.narrowTo[CronJob[F, Env]])

  implicit def CronJobTypeable[F[_], Env]: Typeable[CronJob[F, Env]] =
    Typeable.simpleTypeable(classOf[CronJob[F, Env]])
}
