package uz.scala

import cats.Parallel
import cats.effect.Async
import cats.effect.ExitCode
import cats.effect.Sync
import cats.implicits.catsSyntaxApplicativeError
import org.typelevel.log4cats.Logger

import uz.scala.jobs._

case class JobsModule[F[_]: Async: Parallel: Logger](
    env: JobsEnvironment[F]
  ) {
  def startJobs(config: JobsRunnerConfig): F[ExitCode] =
    new JobsRunner[F, JobsEnvironment[F]](
      env,
      new SingleJobRunner[F, JobsEnvironment[F]],
      new CronJobRunner[F, JobsEnvironment[F]],
      config,
    ).run.redeem(_ => ExitCode.Error, _ => ExitCode.Success)
}

object JobsModule {
  def make[F[_]: Async: Parallel: Logger](
      env: JobsEnvironment[F]
    ): F[JobsModule[F]] =
    Sync[F].delay(new JobsModule(env))
}
