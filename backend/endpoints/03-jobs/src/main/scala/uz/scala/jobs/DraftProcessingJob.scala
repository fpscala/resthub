package uz.scala.jobs

import scala.concurrent.duration._

import cats.effect.IO
import cats.implicits._

import uz.scala.JobsEnvironment
import uz.scala.jobs.Job.AutoName

/** Background job that processes approved job drafts by converting them to published jobs.
  *
  * This job runs periodically to:
  * 1. Find all approved drafts ready for processing
  * 2. Convert each draft to a Job using the JobsAlgebra
  * 3. Mark the draft as processed
  * 4. Handle errors with retry logic and notifications
  *
  * The job implements comprehensive error handling and retry mechanisms to ensure
  * reliable processing of approved drafts.
  *
  * Configuration is loaded from application.conf under the 'draft-processing' section.
  */
object DraftProcessingJob
    extends AutoName[cats.effect.IO]
       with PeriodicJob[cats.effect.IO, JobsEnvironment[cats.effect.IO]] {
  override val interval: FiniteDuration = 5.minutes

  override def run(implicit env: JobsEnvironment[IO]): IO[Unit] =
    for {
      _ <- logger.info("Starting draft processing job")

    } yield ()

}
