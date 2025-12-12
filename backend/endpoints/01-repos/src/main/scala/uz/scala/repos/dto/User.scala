package uz.scala.repos.dto

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.scalaland.chimney.dsl.TransformationOps
import io.scalaland.chimney.dsl._
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt

import uz.scala.domain.AuthedUser
import uz.scala.domain.Phone
import uz.scala.domain.RoleId
import uz.scala.domain.UserId
import uz.scala.domain.enums.UserStatus
import uz.scala.domain.users.Role
import uz.scala.domain.users.{ User => DomainUser }
import uz.scala.shared.EmailAddress
import uz.scala.syntax.refined._

case class User(
    id: UserId,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
    deletedAt: Option[ZonedDateTime] = None,

    // Authentication
    email: EmailAddress,
    password: PasswordHash[SCrypt],

    // Personal info
    firstName: NonEmptyString,
    lastName: NonEmptyString,
    phone: Phone, // Required for Uzbekistan (+998...)

    // System fields
    roleId: RoleId,
    status: UserStatus = UserStatus.PendingVerification,

    // Verification
    emailVerified: Boolean = false,
    phoneVerified: Boolean = false,

    // Timestamps
    lastLoginAt: Option[ZonedDateTime] = None,
  ) {
  def toAuth(role: uz.scala.domain.users.Role): AuthedUser =
    this
      .into[AuthedUser]
      .withFieldConst(_.role, role)
      .withFieldConst(_.name, fullName)
      .transform

  def toDomain(role: uz.scala.domain.users.Role): DomainUser =
    this
      .into[DomainUser]
      .withFieldConst(_.role, role)
      .withFieldConst(_.firstName, firstName)
      .withFieldConst(_.lastName, lastName)
      .withFieldConst(_.phone, phone)
      .withFieldConst(_.email, email)
      .transform

  def fullName: NonEmptyString = s"${firstName.value} ${lastName.value}"
}
