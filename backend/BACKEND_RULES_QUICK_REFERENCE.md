# Backend Development Rules - Quick Reference

## 📁 Project Structure
```
backend/
├── endpoints/
│   ├── 00-domain/         # Domain models, enums, inputs/filters
│   ├── 01-repos/          # Repository layer with SQL and DTOs
│   ├── 02-core/           # Business logic (Algebras)
│   ├── 03-api/            # HTTP routes
│   ├── 03-jobs/           # Background jobs
│   └── 04-server/         # Server configuration
├── integrations/          # External integrations (AWS S3, etc.)
├── supports/              # Common utilities (error handling, mailer, etc.)
└── project/               # SBT configuration
```

## 🔑 Key Rules Summary

### 1. Database First
- Always check if database tables exist before starting
- Review migration files in `01-repos/src/main/resources/db/migration/`
- Check table structure and fields

### 2. Repository Layer Structure (01-repos)
**Rule:** Must create Sql object and repository in repos module.

**Structure:**
```
backend/endpoints/01-repos/
├── src/main/scala/uz/scala/
│   ├── db/sql/         # SQL objects
│   └── repos/          # Repository implementations
```

**Example:**
```scala
object UsersSql extends Sql[User] {
  // SQL queries
  // Automatic table name "users" when extending Sql trait
}

trait UsersRepository[F[_]] {
  // Repository methods
}
```

### 3. DTO Folder Structure
**Rule:** `dto` folder contains only SQL table and view related objects. DTO objects should NOT have DTO suffix, as they extend `Sql[User]` which provides automatic table naming.

**Structure:**
```
backend/endpoints/01-repos/src/main/scala/uz/scala/db/dto/
├── User.scala          # ✅ Table DTO (extends Sql[User])
├── Job.scala           # ✅ Table DTO (extends Sql[Job])
└── UserView.scala      # ✅ View DTO
```

**Example:**
```scala
// dto/User.scala
case class User(
  id: UserId,
  email: EmailAddress,
  firstName: String,
  // ...
)

object User extends Sql[User] {
  // automatic table name "users"
  // Sql trait has many fragments written (columns, etc.)
}
```

**Wrong examples:**
```
dto/
├── UserStatus.scala       # ❌ This is enum - should be in domain/enums
└── UserFilters.scala      # ❌ This is filter - should be in domain
```

### 4. Enum Placement
**Rule:** All enums are saved in domain module in `enums` folder.

**Structure:**
```
backend/endpoints/00-domain/src/main/scala/uz/scala/domain/enums/
├── UserStatus.scala
├── UserRole.scala
└── JobStatus.scala
```

### 5. Filter and Input Objects Structure
**Rule:** Filter objects are saved in domain module in folders specific to each object. Should also have `inputs` folder if possible.

**Structure:**
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

**Principles:**
- **Input objects**: In `inputs` folder
- **Output objects**: In their own folder
- **Domain models**: In main folder

### 6. Business Logic (Algebras)
**Rule:** Algebras are written in core module.

**Structure:**
```
backend/endpoints/02-core/src/main/scala/uz/scala/algebras/
├── UsersAlgebra.scala
├── JobsAlgebra.scala
└── AuthAlgebra.scala
```

**Structure:**
```scala
trait UsersAlgebra[F[_]] {
  def create(input: CreateUserInput): F[User]
  def findById(id: UserId): F[Option[User]]
  // ...
}
```

### 7. Don't Break Existing Structure
**Rule:** Must write without breaking current structure.

**Requirements:**
- Follow existing package structure
- Follow naming conventions
- Use existing patterns
- Don't break module dependencies

### 8. Error Handling
**Rule:** Use existing errors, not create new ones.

**Structure:**
```
backend/supports/services/src/main/scala/uz/scala/http4s/
├── HttpErrorHandler.scala
└── utils/errors/
```

**IMPORTANT:** Errors should NOT be in form `ValidationError()`, `NotFoundError()`!

### 9. Error Messages (I18n)
**Rule:** Error texts are already placed, should be written as Map and used via `AError`.

**Correct practice:**
```scala
// 1. Create error message Map
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

// 2. Use error
AError.BadRequest(USER_NOT_FOUND(lang))
AError.Conflict(EMAIL_ALREADY_EXISTS(lang))
AError.NotFound(USER_NOT_FOUND(lang))
```

**Wrong examples:**
```scala
// ❌ DON'T DO THIS
ValidationError("Field is required")
NotFoundError("User not found")
ConflictError("Email already exists")
```

### 10. API Error Handling
**Rule:** Don't need to write error handler in API module, because error handlers are already written in support.

**Practice:**
- Use `HttpErrorHandler` from support module
- Don't create custom error handlers
- Use existing error handling middleware

**Example:**
```scala
// Support's error handler works automatically
override val public: HttpRoutes[F] =
  HttpRoutes.of[F] {
    case req @ POST -> Root / "register" =>
      implicit val lang: Language = req.lang
      req.decodeR[RegisterInput] { input =>
        usersAlgebra
          .register(input)
          .flatMap(Ok(_))
          // Error handling automatic, just need to throw AError
      }
  }
```

## 🔄 Module Dependencies
- `00-domain` ← No dependencies
- `01-repos` ← Depends on `00-domain`
- `02-core` ← Depends on `00-domain` and `01-repos`
- `03-api` ← Depends on all modules

## 📝 Naming Conventions
- **Domain models**: Singular (User, Job, Company)
- **DTOs**: No DTO suffix, just model name (User, Job) - extends Sql[T]
- **Inputs**: Suffix `Input` (CreateUserInput, UpdateUserInput)
- **Outputs**: Suffix `Output` or the model itself
- **Filters**: Suffix `Filters` (UserFilters)
- **Repositories**: Suffix `Repository` (UsersRepository)
- **Algebras**: Suffix `Algebra` (UsersAlgebra)

## 🏗️ Code Organization
1. Domain models - lowest layer
2. Repository - data access layer
3. Algebra - business logic layer
4. Routes - HTTP endpoints layer

## 💡 About Sql Trait
Many fragments are already written in `Sql` trait:
- Table columns
- Table name (automatic: User → "users")
- Common SQL fragments
- Base CRUD operations

Therefore DTO objects must extend `Sql[T]`.

## ⚠️ Summary

These rules are created to make backend code consistent, maintainable, and scalable. Strict adherence to these rules is required when implementing each new feature.

**IMPORTANT:** If new rules or requirements appear, they should be added to this file.

---
*This file is a quick reference. For detailed rules, see BACKEND_DEVELOPMENT_RULES.md*