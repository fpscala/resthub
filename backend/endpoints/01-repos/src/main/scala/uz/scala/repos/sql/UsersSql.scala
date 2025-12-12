package uz.scala.repos.sql

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._

import uz.scala.domain.UserId
import uz.scala.doobie.Sql
import uz.scala.doobie.syntax.all._
import uz.scala.repos.dto
import uz.scala.repos.dto.User
import uz.scala.shared.EmailAddress

private[repos] object UsersSql extends Sql[dto.User] {
  def findByEmail(email: EmailAddress): Query0[dto.User] =
    sql"""SELECT $columns FROM $table WHERE email = $email AND deleted_at IS NULL LIMIT 1"""
      .query[dto.User]

  def findById(id: UserId): Query0[dto.User] =
    sql"""SELECT $columns FROM $table WHERE id = $id AND deleted_at IS NULL LIMIT 1"""
      .query[dto.User]

  val insert: Update[User] = Update[dto.User](
    sql"""INSERT INTO $table ($columns) VALUES ($values)""".internals.sql
  )

  def delete(id: UserId): Update0 =
    sql"""UPDATE $table SET deleted_at = now() WHERE id = $id""".update

  def update(user: dto.User): Update0 =
    sql"""UPDATE $table
        SET status = ${user.status},
            phone = ${user.phone},
            password = ${user.password},
            role_id = ${user.roleId},
            email_verified = ${user.emailVerified},
            phone_verified = ${user.phoneVerified},
            updated_at = ${user.updatedAt}
        WHERE id = ${user.id}""".update

  def updateLastLogin(id: UserId): Update0 =
    sql"""UPDATE $table
          SET last_login_at = NOW(),
              updated_at = NOW()
          WHERE id = $id""".update
}
