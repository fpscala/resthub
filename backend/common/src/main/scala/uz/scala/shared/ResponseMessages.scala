package uz.scala.shared

import uz.scala.Language
import uz.scala.Language._

object ResponseMessages {
  val FILE_NOT_FOUND: Map[Language, String] = Map(
    En -> "File not found",
    Ru -> "Файл не найден",
    Uz -> "Fayl topilmadi",
  )

  val FILE_CREATED: Map[Language, String] = Map(
    En -> "File successfully created",
    Ru -> "Файл успешно создан",
    Uz -> "Fayl yaratildi",
  )

  val USER_NOT_FOUND: Map[Language, String] = Map(
    En -> "User not found",
    Ru -> "Пользователь не найден",
    Uz -> "Foydalanuvchi topilmadi",
  )

  val PHONE_Y_EXISTS: Map[Language, String] = Map(
    En -> "This phone number already exists",
    Ru -> "Этот номер телефона уже существует",
    Uz -> "Ushbu telefon raqam allaqachon mavjud",
  )

  val USER_CREATED: Map[Language, String] = Map(
    En -> "User successfully created",
    Ru -> "Пользователь успешно создан",
    Uz -> "Foydalanuvchi yaratildi",
  )

  val USER_UPDATED: Map[Language, String] = Map(
    En -> "User updated",
    Ru -> "Пользователь обновлен",
    Uz -> "Foydalanuvchi yangilandi",
  )

  val USER_DELETED: Map[Language, String] = Map(
    En -> "User deleted",
    Ru -> "Пользователь удален",
    Uz -> "Foydalanuvchi o'chirildi",
  )

  val PASSWORD_UPDATED: Map[Language, String] = Map(
    En -> "Password updated",
    Ru -> "Пароль обновлен",
    Uz -> "Parol yangilandi",
  )

  val WRONG_PASSWORD: Map[Language, String] = Map(
    En -> "Wrong password",
    Ru -> "Неверный пароль",
    Uz -> "Noto'g'ri parol",
  )

  val PRIVILEGE_CREATE_USER: Map[Language, String] = Map(
    En -> "You have no privileges to create user",
    Ru -> "У вас нет привилегий создавать пользователя",
    Uz -> "Foydalanuvchi yaratish uchun ruxsat yo'q",
  )

  val CREATE_SUPER_USER: Map[Language, String] = Map(
    En -> "Can't create super user",
    Ru -> "Нельзя создавать суперпользователя",
    Uz -> "Super foydalanuvchi yaratish uchun ruxsat yo'q",
  )

  val PRIVILEGE_CREATE_SUPER_USER: Map[Language, String] = Map(
    En -> "You have no privileges to create super user",
    Ru -> "У вас нет привилегий создавать суперпользователя",
    Uz -> "Sizda super foydalanuvchi yaratish uchun ruxsat yo'q",
  )

  val PASSWORD_DOES_NOT_MATCH: Map[Language, String] = Map(
    En -> "Sms code does not match",
    Ru -> "Код подтверждения не совпадает",
    Uz -> "SMS kodi mos kelmadi",
  )

  val INSUFFICIENT_PRIVILEGES: Map[Language, String] = Map(
    En -> "Forbidden. Insufficient privileges",
    Ru -> "Запрещено. Недостаточно привилегий",
    Uz -> "Taqiqlangan. Imtiyozlar yetarli emas",
  )

  val AUTHENTICATION_REQUIRED: Map[Language, String] = Map(
    En -> "Authentication required",
    Ru -> "Требуется аутентификация",
    Uz -> "Autentifikatsiya talab qilinadi",
  )

  val INVALID_TOKEN: Map[Language, String] = Map(
    En -> "Invalid token or expired",
    Ru -> "Неверный токен или токен устарел",
    Uz -> "Yaroqsiz yoki eskirgan token",
  )

  val INVALID_REFRESH_TOKEN: Map[Language, String] = Map(
    En -> "Invalid refresh token",
    Ru -> "Неверный refresh token",
    Uz -> "Yaroqsiz refresh token",
  )

  val REFRESH_TOKEN_EXPIRED: Map[Language, String] = Map(
    En -> "Refresh token has expired",
    Ru -> "Refresh token истек",
    Uz -> "Refresh token muddati o'tgan",
  )

  val REFRESH_TOKEN_EMPTY: Map[Language, String] = Map(
    En -> "Refresh token is required",
    Ru -> "Refresh token обязателен",
    Uz -> "Refresh token talab qilinadi",
  )

  val BEARER_TOKEN_NOT_FOUND: Map[Language, String] = Map(
    En -> "Bearer token not found",
    Ru -> "Токен не найден",
    Uz -> "Bearer token topilmadi",
  )

  val ROLE_NOT_FOUND: Map[Language, String] = Map(
    En -> "Role not found",
    Ru -> "Роль не найдена",
    Uz -> "Rol topilmadi",
  )

  val ROLE_CREATED: Map[Language, String] = Map(
    En -> "Role successfully created",
    Ru -> "Роль успешно создана",
    Uz -> "Rol yaratildi",
  )

  val ROLE_UPDATED: Map[Language, String] = Map(
    En -> "Role successfully updated",
    Ru -> "Роль успешно обновлена",
    Uz -> "Rol yangilandi",
  )

  val ROLE_DELETED: Map[Language, String] = Map(
    En -> "Role successfully deleted",
    Ru -> "Роль успешно удалена",
    Uz -> "Rol o'chirildi",
  )

  val EMAIL_ALREADY_EXISTS: Map[Language, String] = Map(
  En -> "Email already exists",
  Ru -> "Email уже зарегистрирован",
  Uz -> "Bu email allaqachon ro'yxatdan o'tgan",
  )

  val REGISTRATION_SUCCESS: Map[Language, String] = Map(
  En -> "Registration successful. Activation link sent to your email",
  Ru -> "Регистрация успешна. Ссылка активации отправлена на ваш email",
  Uz -> "Ro'yxatdan o'tdingiz. Email manzilingizga aktivatsiya havolasi yuborildi",
  )

  val INVALID_ACTIVATION_TOKEN: Map[Language, String] = Map(
  En -> "Invalid or expired activation token",
  Ru -> "Неверный или истекший токен активации",
  Uz -> "Noto'g'ri yoki muddati o'tgan aktivatsiya tokeni",
  )

  val ACTIVATION_SUCCESS: Map[Language, String] = Map(
  En -> "Account activated successfully",
  Ru -> "Аккаунт успешно активирован",
  Uz -> "Hisobingiz muvaffaqiyatli faollashtirildi",
  )

  val ACCOUNT_NOT_ACTIVE: Map[Language, String] = Map(
  En -> "Account is not active",
  Ru -> "Аккаунт не активен",
  Uz -> "Hisob faol emas",
  )

  val ACCOUNT_PENDING_VERIFICATION: Map[Language, String] = Map(
  En -> "Account pending email verification. Please check your email",
  Ru -> "Аккаунт ожидает подтверждения email. Пожалуйста, проверьте вашу почту",
  Uz -> "Hisob email tasdiqlanishini kutmoqda. Emailingizni tekshiring",
  )

  val ACCOUNT_SUSPENDED: Map[Language, String] = Map(
  En -> "Account has been suspended. Please contact support",
  Ru -> "Аккаунт заблокирован. Пожалуйста, свяжитесь с поддержкой",
  Uz -> "Hisob bloklangan. Qo'llab-quvvatlash xizmatiga murojaat qiling",
  )

  // NestHub Listing Messages
  val LISTING_NOT_FOUND: Map[Language, String] = Map(
    En -> "Listing not found",
    Ru -> "Объявление не найдено",
    Uz -> "E'lon topilmadi",
  )

  val LISTING_CREATED: Map[Language, String] = Map(
    En -> "Listing created successfully. Awaiting admin approval",
    Ru -> "Объявление успешно создано. Ожидает проверки администратора",
    Uz -> "E'lon yaratildi. Admin tasdiqini kutmoqda",
  )

  val LISTING_DELETED: Map[Language, String] = Map(
    En -> "Listing deleted successfully",
    Ru -> "Объявление успешно удалено",
    Uz -> "E'lon o'chirildi",
  )

  val LISTING_APPROVED: Map[Language, String] = Map(
    En -> "Listing approved successfully",
    Ru -> "Объявление одобрено",
    Uz -> "E'lon tasdiqlandi",
  )

  val LISTING_REJECTED: Map[Language, String] = Map(
    En -> "Listing rejected",
    Ru -> "Объявление отклонено",
    Uz -> "E'lon rad etildi",
  )

  val LISTING_NOT_PENDING: Map[Language, String] = Map(
    En -> "Listing is not in pending status",
    Ru -> "Объявление не в статусе ожидания",
    Uz -> "E'lon kutish holatida emas",
  )

  val LISTING_NOT_APPROVED: Map[Language, String] = Map(
    En -> "Listing is not approved",
    Ru -> "Объявление не одобрено",
    Uz -> "E'lon tasdiqlanmagan",
  )

  val NOT_LISTING_OWNER: Map[Language, String] = Map(
    En -> "You are not the owner of this listing",
    Ru -> "Вы не являетесь владельцем этого объявления",
    Uz -> "Siz bu e'lon egasi emassiz",
  )

  val INVALID_IMAGES: Map[Language, String] = Map(
    En -> "At least one image is required",
    Ru -> "Требуется хотя бы одно изображение",
    Uz -> "Kamida bitta rasm kerak",
  )

  val INVALID_PRICE: Map[Language, String] = Map(
    En -> "Price must be greater than zero",
    Ru -> "Цена должна быть больше нуля",
    Uz -> "Narx noldan katta bo'lishi kerak",
  )

  val INSUFFICIENT_PRIVILEGES_ADMIN: Map[Language, String] = Map(
    En -> "Admin privileges required",
    Ru -> "Требуются права администратора",
    Uz -> "Admin huquqlari kerak",
  )

  val CONTRACT_GENERATED: Map[Language, String] = Map(
    En -> "Contract generated successfully",
    Ru -> "Контракт успешно сгенерирован",
    Uz -> "Shartnoma yaratildi",
  )

  val TOKEN_REUSE_DETECTED_ALL_REVOKED: Map[Language, String] = Map(
    En -> "Token reuse detected. All refresh tokens have been revoked",
    Ru -> "Обнаружено повторное использование токена. Все refresh токены отозваны",
    Uz -> "Token qayta ishlatilishi aniqlandi. Barcha refresh tokenlar bekor qilindi",
  )

}
