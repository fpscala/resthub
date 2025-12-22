package uz.scala.repos

import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits.catsSyntaxApplicativeErrorId
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.catsSyntaxOptionId
import cats.implicits.toFunctorOps
import doobie.ConnectionIO

import uz.scala.Language
import uz.scala.domain.RoleId
import uz.scala.domain.enums.Privilege
import uz.scala.domain.users.Role
import uz.scala.exception.AError
import uz.scala.repos.sql.RolesSql
import uz.scala.shared.ResponseMessages.ROLE_NOT_FOUND

trait RolesRepository[F[_]] {
  def create(role: dto.Role): F[Unit]
  def getAll: F[List[Role]]
  def update(
      id: RoleId
    )(
      update: Role => Role
    )(implicit
      language: Language
    ): ConnectionIO[Unit]
  def getRole(roleId: RoleId)(implicit language: Language): F[Role]
  def getRoleByName(name: String)(implicit language: Language): F[Role]
  def getRoles(roleIds: List[RoleId]): F[Map[RoleId, Role]]
  def insertPrivileges(privileges: List[Privilege]): F[Unit]
  def addPrivileges(roleId: RoleId, privileges: List[Privilege]): F[Unit]
  def delete(roleId: RoleId): F[Unit]
}

object RolesRepository {
  def make: RolesRepository[ConnectionIO] = new RolesRepository[ConnectionIO] {
    override def create(role: dto.Role): ConnectionIO[Unit] =
      RolesSql.insert.run(role).void

    override def getAll: ConnectionIO[List[Role]] =
      RolesSql.selectAll.to[List]

    override def update(
        id: RoleId
      )(
        update: Role => Role
      )(implicit
        language: Language
      ): ConnectionIO[Unit] =
      OptionT(RolesSql.findById(id).option).cataF(
        AError.Internal(ROLE_NOT_FOUND(language)).raiseError[ConnectionIO, Unit],
        { role =>
          val updatedRole = update(role)
          // Get the current DTO to preserve metadata fields
          val updatedDto = dto.Role.fromDomain(updatedRole)
          for {
            _ <- RolesSql.update(updatedDto).run.void
            _ <- removePrivileges(id)
            _ <- RolesSql
              .addRolePrivileges
              .updateMany(updatedRole.privileges.map(p => id -> p.entryName))
              .void
          } yield {}

        },
      )

    override def getRole(roleId: RoleId)(implicit language: Language): ConnectionIO[Role] =
      OptionT(RolesSql.findById(roleId).option)
        .getOrRaise(AError.Internal(ROLE_NOT_FOUND(language)))

    override def getRoleByName(name: String)(implicit language: Language): ConnectionIO[Role] =
      OptionT(RolesSql.findByName(name).option)
        .getOrRaise(AError.Internal(ROLE_NOT_FOUND(language)))

    override def getRoles(roleIds: List[RoleId]): ConnectionIO[Map[RoleId, Role]] =
      NonEmptyList
        .fromList(roleIds)
        .fold(
          Map.empty[RoleId, Role].pure[ConnectionIO]
        ) { ids =>
          RolesSql.get(ids).to[List].map(_.map(r => r.id -> r).toMap)
        }

    override def insertPrivileges(privileges: List[Privilege]): ConnectionIO[Unit] =
      RolesSql
        .insertPrivileges
        .updateMany(privileges.map(p => (p.entryName, p.group, p.description.some)))
        .void

    override def addPrivileges(roleId: RoleId, privileges: List[Privilege]): ConnectionIO[Unit] =
      RolesSql.addRolePrivileges.updateMany(privileges.map(p => roleId -> p.entryName)).void

    private def removePrivileges(roleId: RoleId): ConnectionIO[Unit] =
      RolesSql.removePrivileges(roleId).run.void

    override def delete(roleId: RoleId): ConnectionIO[Unit] =
      RolesSql.delete(roleId).run.void
  }
}
