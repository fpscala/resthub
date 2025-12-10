package uz.scala.jobs

import java.time.LocalDateTime

import cats.Monad
import cats.syntax.all._
import cron4s.CronExpr
import cron4s.lib.javatime._
import org.typelevel.log4cats.Logger

import uz.scala.effects.Calendar

trait SupervisedCronJob[F[_], Env] {
  def job: CronJob[F, Env]
  def interval: CronExpr
  def getNextRun: F[Option[LocalDateTime]]
}

object SupervisedCronJob {
  def make[F[_]: Monad, Env](
      cronJob: CronJob[F, Env],
      cronInterval: CronExpr,
    )(implicit
      calendar: Calendar[F]
    ): SupervisedCronJob[F, Env] =
    new SupervisedCronJob[F, Env] {
      val job: CronJob[F, Env] = new CronJob[F, Env] {
        def name: String = cronJob.name
        def logger: Logger[F] = cronJob.logger

        def run(implicit env: Env): F[Unit] =
          for {
            _ <- cronJob.run(env)
            nextRun <- getNextRun
            _ <- logger.info(s"$name Next run $nextRun")
          } yield ()
      }
      val interval: CronExpr = cronInterval

      def getNextRun: F[Option[LocalDateTime]] = for {
        now <- Calendar[F].currentDateTime
        nextRun = cronInterval.next(now)
      } yield nextRun
    }
}
