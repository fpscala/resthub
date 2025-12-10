package uz.scala

import cats.Parallel
import cats.effect.Async
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.effect.std.Console
import cats.implicits.toFunctorOps
import cats.implicits.toTraverseOps
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import uz.scala.jobs.listeners.DraftApprovedListener
import uz.scala.setup.Environment

object Main extends IOApp {
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  private def runnable[F[_]: Async: Logger: Console: Parallel]: Resource[F, List[F[ExitCode]]] =
    for {
      env <- Environment.make[F]
      httpModule <- HttpModule.make[F](env.toServer)
      jobsModule <- Resource.eval(
        JobsModule.make[F](env.toJobs).map(_.startJobs(env.config.jobs))
      )

      // Start Kafka listeners
      implicit0(jobsEnv: JobsEnvironment[F]) = env.toJobs
      draftApprovedListener = DraftApprovedListener.make[F](env.topics.draftApproved)
      listenerStream = draftApprovedListener.start().compile.drain.as(ExitCode.Success)

    } yield List(httpModule, jobsModule, listenerStream)

  override def run(
      args: List[String]
    ): IO[ExitCode] =
    runnable[IO]
      .use { runners =>
        for {
          fibers <- runners.traverse(_.start)
          _ <- fibers.traverse(_.join)
          _ <- IO.never[Unit]
        } yield ExitCode.Success
      }
}
