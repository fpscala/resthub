package uz.scala

import _root_.doobie.ConnectionIO
import _root_.doobie.Transactor

case class JobsEnvironment[F[_]](
    repos: Repositories[ConnectionIO],
    xa: Transactor[F],
  )
