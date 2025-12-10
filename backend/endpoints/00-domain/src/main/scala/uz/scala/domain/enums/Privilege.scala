package uz.scala.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._
import io.circe.Json

sealed trait Privilege extends Snakecase {
  val group: String
  val description: String
}
object Privilege extends Enum[Privilege] with CirceEnum[Privilege] with DoobieEnum[Privilege] {
  // User management
  case object CreateUser extends Privilege {
    override val group: String = "USER"
    override val description: String = "Yangi foydalanuvchi yaratish huquqi"
  }
  case object UpdateUser extends Privilege {
    override val group: String = "USER"
    override val description: String = "O'z profilini yangilash huquqi"
  }
  case object UpdateAnyUser extends Privilege {
    override val group: String = "USER"
    override val description: String = "Barcha foydalanuvchilarni yangilash huquqi"
  }
  case object DeleteUser extends Privilege {
    override val group: String = "USER"
    override val description: String = "Foydalanuvchilarni o'chirish huquqi"
  }
  case object ViewUsers extends Privilege {
    override val group: String = "USER"
    override val description: String = "Foydalanuvchilar ro'yxatini ko'rish huquqi"
  }
  case object CreateSuperUser extends Privilege {
    override val group: String = "USER"
    override val description: String = "Super admin yaratish huquqi (eng yuqori huquq)"
  }

  // Role management
  case object CreateRole extends Privilege {
    override val group: String = "ROLE"
    override val description: String = "Yangi rol yaratish huquqi"
  }
  case object UpdateRole extends Privilege {
    override val group: String = "ROLE"
    override val description: String = "Rollarni tahrirlash huquqi"
  }
  case object DeleteRole extends Privilege {
    override val group: String = "ROLE"
    override val description: String = "Rollarni o'chirish huquqi"
  }
  case object ViewRoles extends Privilege {
    override val group: String = "ROLE"
    override val description: String = "Barcha rollarni ko'rish huquqi"
  }

  // Assets
  case object CreateAsset extends Privilege {
    override val group: String = "ASSETS"
    override val description: String = "Yangi fayl yuklash huquqi (rasm, PDF, etc)"
  }

  override def values: IndexedSeq[Privilege] = findValues

  val groupedValues: List[Json] = values.groupBy(_.group).toList.map {
    case groupName -> values =>
      Json.obj(
        "groupName" -> Json.fromString(groupName),
        "values" -> Json.fromValues(
          values
            .toList
            .filterNot(_ == CreateSuperUser)
            .map(value =>
              Json.obj(
                "name" -> Json.fromString(value.entryName),
                "description" -> Json.fromString(value.description),
              )
            )
        ),
      )
  }
}
