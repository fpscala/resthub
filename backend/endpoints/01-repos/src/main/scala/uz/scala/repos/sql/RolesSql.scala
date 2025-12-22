package uz.scala.repos.sql

import cats.data.NonEmptyList
import doobie._
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.fragments._

import uz.scala.domain.RoleId
import uz.scala.domain.users.Role
import uz.scala.doobie.Sql
import uz.scala.doobie.syntax.all._
import uz.scala.repos.dto

private[repos] object RolesSql extends Sql[dto.Role] {
  val insert: Update[dto.Role] = Update[dto.Role](
    sql"""INSERT INTO $table ($columns) VALUES ($values)""".internals.sql
  )
  def findById(id: RoleId): Query0[Role] =
    sql"""
        SELECT * FROM role_privileges_view WHERE id = $id
    """.query[Role]

  def findDtoById(id: RoleId): Query0[dto.Role] =
    sql"""
        SELECT id, name, description, is_system, created_at, updated_at
        FROM $table WHERE id = $id
    """.query[dto.Role]

  def findByName(name: String): Query0[Role] =
    sql"""
        SELECT * FROM role_privileges_view WHERE name = $name
    """.query[Role]

  def update(role: dto.Role): Update0 =
    sql"""
        UPDATE $table SET 
          name = ${role.name}, 
          description = ${role.description}, 
          is_system = ${role.isSystem}, 
          updated_at = ${role.updatedAt}
        WHERE id = ${role.id}
    """.update

  def get(roleIds: NonEmptyList[RoleId]): Query0[Role] =
    sql"""
        SELECT * FROM role_privileges_view ${whereAnd(in(fr"id", roleIds))}
    """.query[Role]

  val insertPrivileges: Update[(String, String, Option[String])] =
    Update[(String, String, Option[String])](
      "INSERT INTO privileges (name, group_name, description) VALUES (?, ?, ?) ON CONFLICT (name) DO NOTHING"
    )

  val selectAll: Query0[Role] =
    sql"""SELECT * FROM role_privileges_view WHERE name NOT IN ('SUPER_ADMIN', 'TECH_ADMIN')"""
      .query[Role]

  val addRolePrivileges: Update[(RoleId, String)] =
    Update[(RoleId, String)](
      """INSERT INTO role_privileges (role_id, privilege) VALUES (?, ?) ON CONFLICT (role_id, privilege) DO NOTHING"""
    )

  def removePrivileges(roleId: RoleId): Update0 =
    sql"""DELETE FROM role_privileges WHERE role_id = $roleId""".update

  def delete(roleId: RoleId): Update0 =
    sql"""DELETE FROM $table WHERE id = $roleId""".update
}
