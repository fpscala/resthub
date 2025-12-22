package uz.scala.algebras

import cats.effect.MonadCancelThrow
import doobie.ConnectionIO
import doobie.syntax.connectionio._

import uz.scala.Language
import uz.scala.domain.RoleId
import uz.scala.domain.users.Role
import uz.scala.domain.users.RoleInput
import uz.scala.domain.users.RoleUpdateInput
import uz.scala.effects.Calendar
import uz.scala.effects.GenUUID
import uz.scala.repos.RolesRepository
import uz.scala.repos.dto
import uz.scala.utils.ID

trait RolesAlgebra[F[_]] {
  def createRole(input: RoleInput)(implicit language: Language): F[RoleId]
  def updateRole(roleId: RoleId, input: RoleUpdateInput)(implicit language: Language): F[Unit]
  def getAll: F[List[Role]]
  def getRole(roleId: RoleId)(implicit language: Language): F[Role]
  def getRoleByName(name: String)(implicit language: Language): F[Role]
  def deleteRole(roleId: RoleId): F[Unit]
}

object RolesAlgebra {
  def make[F[_]: MonadCancelThrow: Calendar: GenUUID](
      rolesRepository: RolesRepository[doobie.ConnectionIO]
    )(implicit
      xa: doobie.Transactor[F]
    ): RolesAlgebra[F] =
    new RolesAlgebra[F] {
      override def createRole(input: RoleInput)(implicit language: Language): F[RoleId] =
        (for {
          now <- Calendar[ConnectionIO].currentZonedDateTime
          id <- ID.make[ConnectionIO, RoleId]
          role = dto.Role(
            id = id,
            name = input.name,
            description = input.description,
            isSystem = false,
            createdAt = now,
          )
          _ <- rolesRepository.create(role)
          _ <- rolesRepository.addPrivileges(id, input.privileges)
        } yield id).transact(xa)

      override def updateRole(
          roleId: RoleId,
          input: RoleUpdateInput,
        )(implicit
          language: Language
        ): F[Unit] =
        rolesRepository
          .update(roleId) { role =>
            role.copy(
              name = input.name.getOrElse(role.name),
              privileges = input.privileges.getOrElse(role.privileges),
            )
          }
          .transact(xa)

      override def getAll: F[List[Role]] =
        rolesRepository.getAll.transact(xa)

      override def getRole(roleId: RoleId)(implicit language: Language): F[Role] =
        rolesRepository.getRole(roleId).transact(xa)

      override def getRoleByName(name: String)(implicit language: Language): F[Role] =
        rolesRepository.getRoleByName(name).transact(xa)

      override def deleteRole(roleId: RoleId): F[Unit] =
        rolesRepository.delete(roleId).transact(xa)
    }
}
