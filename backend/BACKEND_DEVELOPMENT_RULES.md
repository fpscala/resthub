# Backend Development Qoidalari va Talablar

Bu fayl backend'da ishlovchi dasturchi uchun qat'iy qoidalar va talablarni o'z ichiga oladi. Har bir feature yoki funksiya yaratilganda bu qoidalardan chiqmaslik shart.

## Asosiy Qoidalar

### 1. Database Schema Tekshiruvi
**Qoida:** Har qanday ishni boshlashdan oldin database table mavjudligini tekshirish kerak.

**Amaliyot:**
- PostgreSQL schema fayllarini ko'rib chiqish
- Migration fayllarini tekshirish
- Table strukturasi va maydonlarini o'rganish

### 2. Repository Layer Strukturasi
**Qoida:** Repos moduleda Sql object va repository yaratilishi shart.

**Amaliyot:**
```
backend/endpoints/01-repos/
├── src/main/scala/uz/scala/
│   ├── db/sql/         # SQL objectlari
│   └── repos/          # Repository implementatsiyalari
```

**Misol:**
```scala
object UsersSql extends Sql[User] {
  // SQL query'lar
  // Sql trait'dan extend qilinganda automatic table name "users" bo'ladi
}

trait UsersRepository[F[_]] {
  // Repository metodlari
}
```

### 3. DTO Folder Strukturasi
**Qoida:** `dto` folderda faqat SQL table va view'larga tegishli objectlar turadi. DTO objectlar oxirida DTO so'zi kerak emas, chunki `Sql[User]` dan extend qilinadi va bu automatic table name beradi.

**Amaliyot:**
```
backend/endpoints/01-repos/src/main/scala/uz/scala/db/dto/
├── User.scala          # ✅ Table DTO (Sql[User] dan extend)
├── Job.scala           # ✅ Table DTO (Sql[Job] dan extend)
└── UserView.scala      # ✅ View DTO
```

**Misol:**
```scala
// dto/User.scala
case class User(
  id: UserId,
  email: EmailAddress,
  firstName: String,
  // ...
)

object User extends Sql[User] {
  // table name automatic "users" bo'ladi
  // Sql trait'da ko'p fragmentlar yozilgan (columns, etc.)
}
```

**Noto'g'ri misollar:**
```
dto/
├── UserStatus.scala       # ❌ Bu enum - domain/enums da bo'lishi kerak
└── UserFilters.scala      # ❌ Bu filter - domain da bo'lishi kerak
```

### 4. Enum Joylashuvi
**Qoida:** Barcha enumlar domain moduleda `enums` folderda saqlanadi.

**Amaliyot:**
```
backend/endpoints/00-domain/src/main/scala/uz/scala/domain/enums/
├── UserStatus.scala
├── UserRole.scala
└── JobStatus.scala
```

### 5. Filter va Input Objectlari Strukturasi
**Qoida:** Filter objectlari domain moduleda har bir objectga tegishli folderda saqlanadi. Iloji boricha `inputs` folder ham bo'lishi kerak.

**Amaliyot:**
```
backend/endpoints/00-domain/src/main/scala/uz/scala/domain/
├── users/
│   ├── inputs/
│   │   ├── CreateUserInput.scala
│   │   ├── UpdateUserInput.scala
│   │   └── UserFilters.scala
│   ├── User.scala
│   └── UserOutput.scala
├── jobs/
│   ├── inputs/
│   │   ├── CreateJobInput.scala
│   │   └── JobFilters.scala
│   └── Job.scala
```

**Prinsiplar:**
- **Kirivchi objectlar**: `inputs` folderda
- **Chiquvchi objectlar**: O'ziga tegishli folderda
- **Domain modellar**: Asosiy folderda

### 6. Business Logic (Algebras)
**Qoida:** Algebralar core moduleda yoziladi.

**Amaliyot:**
```
backend/endpoints/02-core/src/main/scala/uz/scala/algebras/
├── UsersAlgebra.scala
├── JobsAlgebra.scala
└── AuthAlgebra.scala
```

**Struktura:**
```scala
trait UsersAlgebra[F[_]] {
  def create(input: CreateUserInput): F[User]
  def findById(id: UserId): F[Option[User]]
  // ...
}
```

### 7. Mavjud Strukturani Buzmaslik
**Qoida:** Hozirgi strukturani buzmagan holda yozish qat'iy talab qilinadi.

**Talablar:**
- Mavjud package strukturasiga rioya qilish
- Naming convention'larga amal qilish
- Existing pattern'lardan foydalanish
- Module dependencies'ni buzmaslik

### 8. Error Handling
**Qoida:** Errorlar avvaldan yaratilgan, shulardan foydalanish kerak.

**Amaliyot:**
```
backend/supports/services/src/main/scala/uz/scala/http4s/
├── HttpErrorHandler.scala
└── utils/errors/
```

**IMPORTANT:** Errorlar `ValidationError()`, `NotFoundError()` kabi shaklda EMAS!

### 9. Error Messages (I18n)
**Qoida:** Error textlari allaqachon joylashtirilgan, Map shaklida yozilishi kerak va `AError` orqali ishlatilishi kerak.

**To'g'ri amaliyot:**
```scala
// 1. Error message Map yaratish
val USER_NOT_FOUND: Map[Language, String] = Map(
  Language.UZ -> "Foydalanuvchi topilmadi",
  Language.EN -> "User not found",
  Language.RU -> "Пользователь не найден"
)

val EMAIL_ALREADY_EXISTS: Map[Language, String] = Map(
  Language.UZ -> "Bu email allaqachon ro'yxatdan o'tgan",
  Language.EN -> "Email already exists",
  Language.RU -> "Email уже зарегистрирован"
)

// 2. Error'ni ishlatish
AError.BadRequest(USER_NOT_FOUND(lang))
AError.Conflict(EMAIL_ALREADY_EXISTS(lang))
AError.NotFound(USER_NOT_FOUND(lang))
```

**Noto'g'ri misollar:**
```scala
// ❌ BUNDAY QILMASLIK KERAK
ValidationError("Field is required")
NotFoundError("User not found")
ConflictError("Email already exists")
```

### 10. API Error Handling
**Qoida:** API moduleda error handler yozish shart emas, chunki support ichida allaqachon error handlerlar yozilgan.

**Amaliyot:**
- Support moduledagi `HttpErrorHandler`dan foydalanish
- Custom error handler yaratmaslik
- Mavjud error handling middleware'dan foydalanish

**Misol:**
```scala
// Support'dagi error handler avtomatik ishlaydi
override val public: HttpRoutes[F] =
  HttpRoutes.of[F] {
    case req @ POST -> Root / "register" =>
      implicit val lang: Language = req.lang
      req.decodeR[RegisterInput] { input =>
        usersAlgebra
          .register(input)
          .flatMap(Ok(_))
          // Error handling avtomatik, faqat AError throw qilish kerak
      }
  }
```

## Qo'shimcha Qoidalar

### Module Dependencies
- `00-domain` - Hech kimga bog'liq emas
- `01-repos` - `00-domain`ga bog'liq
- `02-core` - `00-domain` va `01-repos`ga bog'liq
- `03-api` - Barcha modullarga bog'liq

### Naming Conventions
- **Domain models**: Singular (User, Job, Company)
- **DTO'lar**: DTO suffix yo'q, faqat model nomi (User, Job) - Sql[T] dan extend
- **Inputs**: Suffix `Input` (CreateUserInput, UpdateUserInput)
- **Outputs**: Suffix `Output` yoki model o'zi
- **Filters**: Suffix `Filters` (UserFilters)
- **Repositories**: Suffix `Repository` (UsersRepository)
- **Algebras**: Suffix `Algebra` (UsersAlgebra)

### Code Organization
1. Domain models - eng pastki qatlam
2. Repository - data access layer
3. Algebra - business logic layer
4. Routes - HTTP endpoints layer

### Sql Trait Haqida
`Sql` trait'da ko'p fragmentlar allaqachon yozilgan:
- Table columns
- Table name (automatic: User -> "users")
- Common SQL fragments
- Base CRUD operations

Shuning uchun DTO objectlarni `Sql[T]` dan extend qilish kerak.

### 11. Circe Codecs
**Qoida:** Har doim circe codec'lar uchun `@JsonCodec` ishlatish kerak, `@derive(encoder, decoder)` emas.

**To'g'ri amaliyot:**
```scala
import io.circe.generic.JsonCodec

@JsonCodec
case class UserInput(
  name: String,
  email: String
)
```

**Noto'g'ri misollar:**
```scala
// ❌ BUNDAY QILMASLIK KERAK
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

@derive(encoder, decoder)
case class UserInput(
  name: String,
  email: String
)
```

### 12. Newtype Codecs
**Qoida:** Newtype'lar uchun ortiqcha decoder/encoder yozish shart emas, `import uz.scala.syntax.circe._` import qilsang yetarli.

**To'g'ri amaliyot:**
```scala
import uz.scala.syntax.circe._

case class UserListing(listingId: ListingId) // ListingId newtype
```

**Noto'g'ri misollar:**
```scala
// ❌ BUNDAY QILMASLIK KERAK
object ListingId {
  implicit val listingIdDecoder: Decoder[ListingId] = Decoder.forProduct1("listingId")(ListingId.apply)
  implicit val listingIdEncoder: Encoder[ListingId] = Encoder.forProduct1("listingId")(_.value)
}
```

### 13. JSONB Field Handling
**Qoida:** Agar DTO object da Json ishlatilsa `import doobie.postgres.circe.jsonb.implicits._` qo'shish kerak, bu jsonb uchun meta implicit beradi.

**To'g'ri amaliyot:**
```scala
import doobie.postgres.circe.jsonb.implicits._

case class RefreshToken(
    id: RefreshTokenId,
    deviceInfo: Option[io.circe.Json] = None, // Json field
    // ...
)
```

**Noto'g'ri misollar:**
```scala
// ❌ BUNDAY QILMASLIK KERAK
case class RefreshToken(
    id: RefreshTokenId,
    deviceInfo: Option[io.circe.Json] = None, // Json field lekin jsonb implicits yo'q
    // ...
)
```

### 14. DTO Transformations
**Qoida:** DTO objectlarda `toDomain` methodi case class ichida, `fromDomain` methodi companion object ichida yozish kerak, Chimney transformation dan foydalanmaslik kerak.

**To'g'ri amaliyot:**
```scala
case class Listing(
    id: ListingId,
    title: NonEmptyString,
    price: Money,
    // ...
) {
  def toDomain: ListingOutput =
    ListingOutput(
      id = id,
      title = title,
      price = price.amount, // Money to BigDecimal
      // ...
    )
}

object Listing {
  def fromDomain(domain: ListingOutput): Listing =
    Listing(
      id = domain.id,
      title = domain.title,
      price = Money(domain.price), // BigDecimal to Money
      // ...
    )
}
```

**Noto'g'ri misollar:**
```scala
// ❌ BUNDAY QILMASLIK KERAK
listing.into[ListingOutput]
  .withFieldConst(_.owner, ownerDomain)
  .withFieldComputed(_.price, _.price.amount)
  .transform
```

### 15. Doobie Import Conflicts
**Qoida:** `uz.scala.doobie.syntax.all._` va `doobie.postgres.implicits._` lar o'rtasida conflict bor. O'z librarylaringiz prioritetga ega, shuning uchun `doobie.postgres.implicits._` ni ishlatmaslik kerak.

**To'g'ri amaliyot:**
```scala
import doobie._
import doobie.implicits._
import doobie.refined.implicits._
import uz.scala.doobie.syntax.all._  // ✅ O'z library implicitlari
// ❌ doobie.postgres.implicits._ ni qo'shmang
```

**Noto'g'ri misollar:**
```scala
// ❌ BUNDAY QILMASLIK KERAK (conflict)
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._  // Conflict!
import uz.scala.doobie.syntax.all._
```

## Xulosa

Bu qoidalar backend kodini izchil, maintainable va scalable qilish uchun yaratilgan. Har bir yangi feature implement qilishda bu qoidalarga qat'iy amal qilish shart.

**IMPORTANT:** Agar yangi qoida yoki talab paydo bo'lsa, bu faylga qo'shib borish kerak.