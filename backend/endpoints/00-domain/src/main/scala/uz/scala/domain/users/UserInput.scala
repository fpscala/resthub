package uz.scala.domain.users

import eu.timepit.refined.types.string.NonEmptyString

import uz.scala.Language
import uz.scala.domain.Phone
import uz.scala.domain.RoleId
import uz.scala.domain.enums.Privilege
import uz.scala.shared.ResponseMessages.CREATE_SUPER_USER
import uz.scala.shared.ResponseMessages.PRIVILEGE_CREATE_SUPER_USER
import uz.scala.shared.ResponseMessages.PRIVILEGE_CREATE_USER
import uz.scala.validation.Rules
import uz.scala.validation.createRule

case class UserInput(
    firstname: NonEmptyString,
    lastname: NonEmptyString,
    phone: Phone,
    roleId: RoleId,
  )

object UserInput {
  implicit def validate(implicit userRole: Role, language: Language): Rules[Role] =
    List(
      createRule[Role](PRIVILEGE_CREATE_USER(language)) { role =>
        userRole.privileges.contains(Privilege.CreateUser)
      },
      createRule[Role](CREATE_SUPER_USER(language)) { role =>
        !role.privileges.contains(Privilege.CreateSuperUser)
      },
      createRule[Role](PRIVILEGE_CREATE_SUPER_USER(language)) { role =>
        val cond = role
          .privileges
          .exists(
            List(Privilege.ViewUsers, Privilege.CreateUser, Privilege.UpdateAnyUser).contains
          )
        !cond || (
          cond &&
          userRole.privileges.contains(Privilege.CreateSuperUser)
        )
      },
    )
}
