package uz.scala.domain.users

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import uz.scala.domain.Phone
import uz.scala.domain.UserId
import uz.scala.domain.enums.UserStatus
import uz.scala.shared.EmailAddress
import uz.scala.syntax.circe._

@JsonCodec
case class User(
    id: UserId,
    email: EmailAddress,
    firstName: NonEmptyString,
    lastName: NonEmptyString,
    phone: Phone,
    role: Role,
    status: UserStatus,
    emailVerified: Boolean,
    phoneVerified: Boolean,
    createdAt: ZonedDateTime,
  )
