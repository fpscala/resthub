package uz.scala.shared

import uz.scala.Language
import uz.scala.Language._

object BotMessages {
  val WELCOME_NEW: Map[Language, String] = Map(
    En -> """🏠 Welcome to NestHub!

Your friendly rental assistant in Uzbekistan. Let's find you the perfect home! 🏡

🔍 Quick commands:
/search - Search for rental homes
/help - Get help

💡 Tip: Use the interactive buttons below for easier searching!""",
    Ru -> """🏠 Добро пожаловать в NestHub!

Ваш дружелюбный помощник по аренде жилья в Узбекистане. Найдем вам идеальный дом! 🏡

🔍 Быстрые команды:
/search - Поиск жилья для аренды
/help - Получить помощь

💡 Совет: Используйте интерактивные кнопки для удобного поиска!""",
    Uz -> """🏠 NestHub botiga xush kelibsiz!

O'zbekistondagi ijaraga uylar bo'yicha sizning do'stona yordamchingiz. Ajoyib uyni topamiz! 🏡

🔍 Tezkor buyruqlar:
/search - Ijaraga uylarni qidirish
/help - Yordam olish

💡 Maslahat: Qulayroq qidirish uchun interaktiv tugmalardan foydalaning!""",
  )

  val SELECT_CITY: Map[Language, String] = Map(
    En -> "📍 Which city are you looking for rentals in?",
    Ru -> "📍 В каком городе вы ищете жилье для аренды?",
    Uz -> "📍 Qaysi shaharda ijaraga uy qidiryapsiz?",
  )

  val ENTER_CUSTOM_CITY: Map[Language, String] = Map(
    En -> "✍️ Please type the city name:",
    Ru -> "✍️ Пожалуйста, введите название города:",
    Uz -> "✍️ Iltimos, shahar nomini kiriting:",
  )

  val SELECT_PRICE_RANGE: Map[Language, String] = Map(
    En -> "💰 What's your monthly budget? (USD)",
    Ru -> "💰 Какой ваш месячный бюджет? (USD)",
    Uz -> "💰 Oylik byudjetingiz qancha? (USD)",
  )

  val SELECT_ROOMS: Map[Language, String] = Map(
    En -> "🏠 How many rooms do you need? (Optional)",
    Ru -> "🏠 Сколько комнат вам нужно? (Необязательно)",
    Uz -> "🏠 Nechta xona kerak? (Ixtiyoriy)",
  )

  val SEARCHING: Map[Language, String] = Map(
    En -> "🔍 Searching for perfect matches...",
    Ru -> "🔍 Поиск подходящих вариантов...",
    Uz -> "🔍 Mos variantlar qidirilmoqda...",
  )

  val SEARCH_RESULTS_HEADER: Map[Language, String] = Map(
    En -> "🎉 Found {count} properties for you!",
    Ru -> "🎉 Найдено {count} объектов для вас!",
    Uz -> "🎉 Siz uchun {count} ta uy topildi!",
  )

  val NO_RESULTS: Map[Language, String] = Map(
    En -> """😔 No properties found matching your criteria.

💡 Try:
• Different price range
• Nearby cities
• Removing filters""",
    Ru -> """😔 Объекты, соответствующие вашим критериям, не найдены.

💡 Попробуйте:
• Другой ценовой диапазон
• Ближайшие города
• Убрать фильтры""",
    Uz -> """😔 Sizning talablaringizga mos uy topilmadi.

💡 Quyidagilarni urinib ko'ring:
• Boshqa narx oralig'i
• Yaqin atrofdagi shaharlar
• Filterlarni olib tashlash""",
  )

  val INVALID_PRICE: Map[Language, String] = Map(
    En -> "❌ Please enter a valid number. Example: 500",
    Ru -> "❌ Пожалуйста, введите корректное число. Пример: 500",
    Uz -> "❌ Iltimos, to'g'ri son kiriting. Masalan: 500",
  )

  val INVALID_PRICE_RANGE: Map[Language, String] = Map(
    En -> "❌ Minimum price should be less than maximum price. Please try again.",
    Ru -> "❌ Минимальная цена должна быть меньше максимальной. Попробуйте еще раз.",
    Uz -> "❌ Minimal narx maksimal narxdan kichik bo'lishi kerak. Qaytadan urinib ko'ring.",
  )

  val ADMIN_POST_PROMPT: Map[Language, String] = Map(
    En -> "Please forward the listing you want to post to the platform:",
    Ru -> "Пожалуйста, перешлите мне объявление, которое вы хотите разместить на платформе:",
    Uz -> "Iltimos, platformaga joylamoqchi bo'lgan e'lonni menga forward qiling:",
  )

  val ADMIN_POST_CONFIRM: Map[Language, String] = Map(
    En -> "Do you want to post this listing to the platform?",
    Ru -> "Хотите разместить это объявление на платформе?",
    Uz -> "Bu e'lonni platformaga joylamoqchimisiz?",
  )

  val BUTTON_YES: Map[Language, String] = Map(
    En -> "✅ Yes",
    Ru -> "✅ Да",
    Uz -> "✅ Ha",
  )

  val BUTTON_NO: Map[Language, String] = Map(
    En -> "❌ No",
    Ru -> "❌ Нет",
    Uz -> "❌ Yo'q",
  )

  val BUTTON_CONFIRM_POST: Map[Language, String] = Map(
    En -> "✅ Yes, post",
    Ru -> "✅ Да, разместить",
    Uz -> "✅ Ha, joylash",
  )

  val BUTTON_CANCEL_POST: Map[Language, String] = Map(
    En -> "❌ No, cancel",
    Ru -> "❌ Нет, отменить",
    Uz -> "❌ Yo'q, bekor qilish",
  )

  val OTHER_CITY: Map[Language, String] = Map(
    En -> "Other city",
    Ru -> "Другой город",
    Uz -> "Boshqa shahar",
  )

  val WHAT_WOULD_YOU_LIKE_TO_DO: Map[Language, String] = Map(
    En -> "What would you like to do?",
    Ru -> "Что бы вы хотели сделать?",
    Uz -> "Nima qilishni istaysiz?",
  )

  val NEED_MORE_OPTIONS: Map[Language, String] = Map(
    En -> "Need more options?",
    Ru -> "Нужно больше вариантов?",
    Uz -> "Ko'proq variantlar kerakmi?",
  )

  val USE_BUTTONS_TO_NAVIGATE: Map[Language, String] = Map(
    En -> "Use the buttons below to navigate through results.",
    Ru -> "Используйте кнопки ниже для навигации по результатам.",
    Uz -> "Natijalar o'rtasida harakatlanish uchun quyidagi tugmalardan foydalaning.",
  )

  val LAST_LISTING_ON_PAGE: Map[Language, String] = Map(
    En -> "This is the last listing on this page. Use 'Show more' to see more properties.",
    Ru -> "Это последнее объявление на этой странице. Используйте 'Показать больше' для просмотра больше свойств.",
    Uz -> "Bu sahifadagi so'ngi e'lon. Ko'proq mulklarni ko'rish uchun 'Ko'proq ko'rsatish' tugmasini bosing.",
  )

  val ERROR_NO_CONTEXT_FOUND: Map[Language, String] = Map(
    En -> "Error: No context found.",
    Ru -> "Ошибка: Контекст не найден.",
    Uz -> "Xatolik: Konteks topilmadi.",
  )

  val ERROR_CHECKING_PERMISSIONS: Map[Language, String] = Map(
    En -> "Error checking permissions.",
    Ru -> "Ошибка проверки разрешений.",
    Uz -> "Ruxsatlarni tekshirishda xatolik.",
  )

  val NO_CHANNEL_SELECTED: Map[Language, String] = Map(
    En -> "No channel selected.",
    Ru -> "Канал не выбран.",
    Uz -> "Kanal tanlanmagan.",
  )

  val PERMISSION_DENIED: Map[Language, String] = Map(
    En -> "You don't have permission to post in this channel!",
    Ru -> "У вас нет прав для размещения объявлений в этом канале!",
    Uz -> "Sizda ushbu kanalda e'lon joylash uchun ruxsat yo'q!",
  )

  val LISTING_POSTED_SUCCESSFULLY: Map[Language, String] = Map(
    En -> "Listing successfully posted!",
    Ru -> "Объявление успешно размещено!",
    Uz -> "E'lon muvaffaqiyatli joylandi!",
  )

  val LISTING_POSTED_PENDING_MODERATION: Map[Language, String] = Map(
    En -> "✅ Listing successfully posted! It will be visible on the platform after moderation.",
    Ru -> "✅ Объявление успешно размещено! После модерации оно будет видно на платформе.",
    Uz -> "✅ E'lon muvaffaqiyatli joylandi! Moderatorlar tomonidan ko'rib chiqilgach, u platformada ko'rinadi.",
  )

  val LISTING_POSTING_CANCELLED: Map[Language, String] = Map(
    En -> "Listing posting cancelled.",
    Ru -> "Размещение объявления отменено.",
    Uz -> "E'lon joylash bekor qilindi.",
  )

  val CANCELLED: Map[Language, String] = Map(
    En -> "Cancelled.",
    Ru -> "Отменено.",
    Uz -> "Bekor qilindi.",
  )

  val NEED_TO_REGISTER_FIRST: Map[Language, String] = Map(
    En -> "You need to register with the bot first to post listings!",
    Ru -> "Для размещения объявления вам нужно сначала зарегистрироваться в боте!",
    Uz -> "E'lon joylash uchun avval botni ro'yhatdan o'tkazishingiz kerak!",
  )

  val ERROR_LISTING_TYPE_NOT_SPECIFIED: Map[Language, String] = Map(
    En -> "Error: Listing type not specified.",
    Ru -> "Ошибка: Тип объявления не определен.",
    Uz -> "Xatolik: E'lon turi aniqlanmagan.",
  )

  val DEFAULT_LISTING_TITLE: Map[Language, String] = Map(
    En -> "Listing",
    Ru -> "Объявление",
    Uz -> "E'lon",
  )

  // Admin posting prompts
  val PROMPT_LISTING_TYPE: Map[Language, String] = Map(
    En -> "Listing type (For Rent/For Sale):",
    Ru -> "Тип объявления (Иjaraga/Sotiladi):",
    Uz -> "E'lon turi (Ijaraga/Sotiladi):",
  )

  val PROMPT_PRICE: Map[Language, String] = Map(
    En -> "Price (USD):",
    Ru -> "Цена (USD):",
    Uz -> "Narx (USD):",
  )

  val PROMPT_CITY: Map[Language, String] = Map(
    En -> "City:",
    Ru -> "Город:",
    Uz -> "Shahar:",
  )

  val PROMPT_ROOMS: Map[Language, String] = Map(
    En -> "Number of rooms (or 'skip'):",
    Ru -> "Количество комнат (или 'пропустить'):",
    Uz -> "Xonalar soni (yoki 'skip'):",
  )

  val PROMPT_PHONE: Map[Language, String] = Map(
    En -> "Phone number:",
    Ru -> "Номер телефона:",
    Uz -> "Telefon raqami:",
  )

  val PROMPT_DISTRICT: Map[Language, String] = Map(
    En -> "District (or 'skip'):",
    Ru -> "Район (или 'пропустить'):",
    Uz -> "Tuman (yoki 'skip'):",
  )

  val PROMPT_FLOOR: Map[Language, String] = Map(
    En -> "Floor (or 'skip'):",
    Ru -> "Этаж (или 'пропустить'):",
    Uz -> "Qavat (yoki 'skip'):",
  )

  val PROMPT_TOTAL_FLOORS: Map[Language, String] = Map(
    En -> "Total floors (or 'skip'):",
    Ru -> "Всего этажей (или 'пропустить'):",
    Uz -> "Umumiy qavatlar (yoki 'skip'):",
  )

  val PROMPT_BUILDING_TYPE: Map[Language, String] = Map(
    En -> "Building type - Kvartira/Hovli/Ofis (or 'skip'):",
    Ru -> "Тип здания - Квартира/Дом/Офис (или 'пропустить'):",
    Uz -> "Bino turi - Kvartira/Hovli/Ofis (yoki 'skip'):",
  )

  val PROMPT_CONDITION: Map[Language, String] = Map(
    En -> "Condition - Yaxshi/Zo‘r/Ta'mirlangan (or 'skip'):",
    Ru -> "Состояние - Хороший/Отличный/Ремонт (или 'пропустить'):",
    Uz -> "Holati - Yaxshi/Zo‘r/Ta'mirlangan (yoki 'skip'):",
  )

  // Error messages for validation
  val ERROR_INVALID_LISTING_TYPE: Map[Language, String] = Map(
    En -> "Please enter a valid type: 'For Rent' or 'For Sale'",
    Ru -> "Пожалуйста, введите правильный тип: 'Иjaraga' или 'Sotiladi'",
    Uz -> "Iltimos, to'g'ri turdagi kiriting: 'Ijaraga' yoki 'Sotiladi'",
  )

  val ERROR_INVALID_PRICE: Map[Language, String] = Map(
    En -> "Please enter a valid price (numbers only):",
    Ru -> "Пожалуйста, введите правильную цену (только цифры):",
    Uz -> "Iltimos, to'g'ri narx kiriting (faqat raqamlar):",
  )

  val ERROR_EMPTY_CITY: Map[Language, String] = Map(
    En -> "Please enter the city name:",
    Ru -> "Пожалуйста, введите название города:",
    Uz -> "Iltimos, shahar nomini kiriting:",
  )

  val ERROR_INVALID_ROOMS: Map[Language, String] = Map(
    En -> "Please enter number of rooms (1, 2, 3, ...) or 'skip':",
    Ru -> "Пожалуйста, введите количество комнат (1, 2, 3, ...) или 'пропустить':",
    Uz -> "Iltimos, xonalar sonini kiriting (1, 2, 3, ...) yoki 'skip':",
  )

  val ERROR_INVALID_PHONE: Map[Language, String] = Map(
    En -> "Please enter a valid phone number:",
    Ru -> "Пожалуйста, введите правильный номер телефона:",
    Uz -> "Iltimos, to'g'ri telefon raqamini kiriting:",
  )

  val ERROR_INVALID_DISTRICT: Map[Language, String] = Map(
    En -> "Please enter district name or 'skip':",
    Ru -> "Пожалуйста, введите название района или 'пропустить':",
    Uz -> "Iltimos, tuman nomini kiriting yoki 'skip':",
  )

  val ERROR_INVALID_FLOOR: Map[Language, String] = Map(
    En -> "Please enter floor number or 'skip':",
    Ru -> "Пожалуйста, введите номер этажа или 'пропустить':",
    Uz -> "Iltimos, qavat raqamini kiriting yoki 'skip':",
  )

  val ERROR_INVALID_TOTAL_FLOORS: Map[Language, String] = Map(
    En -> "Please enter total floors or 'skip':",
    Ru -> "Пожалуйста, введите общее количество этажей или 'пропустить':",
    Uz -> "Iltimos, umumiy qavatlar sonini kiriting yoki 'skip':",
  )

  val ERROR_INVALID_BUILDING_TYPE: Map[Language, String] = Map(
    En -> "Please enter building type: 'Kvartira', 'Hovli', 'Ofis' or 'skip':",
    Ru -> "Пожалуйста, введите тип здания: 'Квартира', 'Дом', 'Офис' или 'пропустить':",
    Uz -> "Iltimos, bino turini kiriting: 'Kvartira', 'Hovli', 'Ofis' yoki 'skip':",
  )

  val ERROR_INVALID_CONDITION: Map[Language, String] = Map(
    En -> "Please enter condition: 'Yaxshi', 'Zo‘r', 'Ta'mirlangan' or 'skip':",
    Ru -> "Пожалуйста, введите состояние: 'Хороший', 'Отличный', 'Ремонт' или 'пропустить':",
    Uz -> "Iltimos, holatini kiriting: 'Yaxshi', 'Zo‘r', 'Ta'mirlangan' yoki 'skip':",
  )

  val ERROR_INVALID_CHANNEL_ID: Map[Language, String] = Map(
    En -> "Please enter a valid channel ID:",
    Ru -> "Пожалуйста, введите правильный ID канала:",
    Uz -> "Iltimos, to'g'ri kanal ID sini kiriting:",
  )

  val ERROR_BROKER_FEATURE_IN_BUYER_MODE: Map[Language, String] = Map(
    En -> "❌ This feature is only available in Broker mode!\n\nSwitch to Broker mode in Settings to post listings.",
    Ru -> "❌ Эта функция доступна только в режиме Брокера!\n\nПереключитесь в режим Брокера в Настройках для размещения объявлений.",
    Uz -> "❌ Bu funktsiya faqat Broker rejimida mavjud!\n\nE'lon joylash uchun Sozlamalarda Broker rejimiga o'ting."
  )

  val ERROR_BUYER_FEATURE_IN_BROKER_MODE: Map[Language, String] = Map(
    En -> "❌ This feature is only available in Buyer mode!\n\nSwitch to Buyer mode in Settings to search properties.",
    Ru -> "❌ Эта функция доступна только в режиме Покупателя!\n\nПереключитесь в режим Покупателя в Настройках для поиска недвижимости.",
    Uz -> "❌ Bu funktsiya faqat Xaridor rejimida mavjud!\n\nUylarni qidirish uchun Sozlamalarda Xaridor rejimiga o'ting."
  )

  val PROMPT_CONFIRM_YES_NO: Map[Language, String] = Map(
    En -> "Please answer 'yes' or 'no':",
    Ru -> "Пожалуйста, ответьте 'да' или 'нет':",
    Uz -> "Iltimos, 'ha' yoki 'yo'q' deb javob bering:",
  )

  // Listing preview header
  val LISTING_PREVIEW_HEADER: Map[Language, String] = Map(
    En -> "📋 Listing Preview:",
    Ru -> "📋 Предварительный просмотр объявления:",
    Uz -> "📋 E'lon ko'rinishi:",
  )

  val CONTACT_FEATURE_COMING_SOON: Map[Language, String] = Map(
    En -> "📞 Contact feature coming soon! Please call the number listed in the property details.",
    Ru -> "📞 Функция контактов скоро будет доступна! Пожалуйста, позвоните по номеру, указанному в данных об объекте.",
    Uz -> "📞 Bog'lanish funktsiyasi yaqin kunda qo'shiladi! Iltimos, obyekt tafsilotlarida ko'rsatilgan raqamga qo'ng'iroq qiling."
  )

  val SAVE_FEATURE_COMING_SOON: Map[Language, String] = Map(
    En -> "❤️ Save feature coming soon! You'll be able to save your favorite properties.",
    Ru -> "❤️ Функция сохранения скоро будет доступна! Вы сможете сохранять избранные объекты.",
    Uz -> "❤️ Saqlash funktsiyasi yaqin kunda qo'shiladi! Sevimli obyektlaringizni saqlab olishingiz mumkin bo'ladi."
  )

  val CONFIRM_ASK: Map[Language, String] = Map(
    En -> "Do you confirm?",
    Ru -> "Подтверждаете?",
    Uz -> "Tasdiqlaysizmi?"
  )

  val PROMPT_MAX_PRICE: Map[Language, String] = Map(
    En -> "💰 Maximum price (USD):",
    Ru -> "💰 Максимальная цена (USD):",
    Uz -> "💰 Maksimal narx (USD):"
  )

  // Help messages
  val HELP_HEADER: Map[Language, String] = Map(
    En -> "🤖 NestHub Bot Help",
    Ru -> "🤖 Помощь бота NestHub",
    Uz -> "🤖 NestHub Bot yordami"
  )

  val HELP_SEARCH_SECTION: Map[Language, String] = Map(
    En -> "🔍 **To search:**",
    Ru -> "🔍 **Для поиска:**",
    Uz -> "🔍 **Qidirish uchun:**"
  )

  val HELP_SEARCH_STEPS: Map[Language, String] = Map(
    En -> "• Press \"🔍 Search for homes\" button\n• Select or enter city\n• Set price range\n• Choose number of rooms (optional)",
    Ru -> "• Нажмите \"🔍 Поиск жилья\"\n• Выберите или введите город\n• Укажите ценовой диапазон\n• Выберите количество комнат (опционально)",
    Uz -> "• \"🔍 Uylarni qidirish\" tugmasini bosing\n• Shaharni tanlang yoki kiriting\n• Narx oralig'ini belgilang\n• Xonalar sonini tanlang (ixtiyoriy)"
  )

  val HELP_COMMANDS_SECTION: Map[Language, String] = Map(
    En -> "📱 **Other commands:**",
    Ru -> "📱 **Другие команды:**",
    Uz -> "📱 **Boshqa buyruqlar:**"
  )

  val HELP_COMMANDS_LIST: Map[Language, String] = Map(
    En -> "/start - Main menu\n/search - New search\n💡 You can always use inline buttons!",
    Ru -> "/start - Главное меню\n/search - Новый поиск\n💡 Вы всегда можете использовать интерактивные кнопки!",
    Uz -> "/start - Bosh menyu\n/search - Yangi qidirish\n💡 Siz har doim inline tugmalardan foydalanishingiz mumkin!"
  )

  val HELP_CONTACT: Map[Language, String] = Map(
    En -> "❓ If you have questions, contact admin!",
    Ru -> "❓ Если есть вопросы, свяжитесь с администратором!",
    Uz -> "❓ Savollaringiz bo'lsa, admin bilan bog'laning!"
  )

  // Mode selection messages
  val MODE_SELECTION_PROMPT: Map[Language, String] = Map(
    En -> "Qaysi rejimda foydalanmoqchisiz?",
    Ru -> "В каком режиме вы хотите работать?",
    Uz -> "Qaysi rejimda foydalanmoqchisiz?"
  )

  val BUYER_MODE_DESCRIPTION: Map[Language, String] = Map(
    En -> "🏠 Xaridor (E'lon qidirish)",
    Ru -> "🏠 Покупатель (Поиск объявлений)",
    Uz -> "🏠 Xaridor (E'lon qidirish)"
  )

  val BROKER_MODE_DESCRIPTION: Map[Language, String] = Map(
    En -> "🏢 Makler (E'lon joylash)",
    Ru -> "🏢 Брокер (Размещение объявлений)",
    Uz -> "🏢 Makler (E'lon joylash)"
  )

  val CURRENT_MODE: Map[Language, String] = Map(
    En -> "🎯 Current mode:",
    Ru -> "🎯 Текущий режим:",
    Uz -> "🎯 Hozirgi rejim:",
  )

  val MODE_CHANGED_SUCCESSFULLY: Map[Language, String] = Map(
    En -> "✅ Rejim muvaffaqiyatli o'zgartirildi!",
    Ru -> "✅ Режим успешно изменен!",
    Uz -> "✅ Rejim muvaffaqiyatli o'zgartirildi!"
  )

  val SETTINGS_MENU: Map[Language, String] = Map(
    En -> "⚙️ Settings",
    Ru -> "⚙️ Настройки",
    Uz -> "⚙️ Sozlamalar"
  )

  val CHANGE_MODE: Map[Language, String] = Map(
    En -> "🔄 Rejimni o'zgartirish",
    Ru -> "🔄 Изменить режим",
    Uz -> "🔄 Rejimni o'zgartirish"
  )

  val CHANGE_LANGUAGE: Map[Language, String] = Map(
    En -> "🌐 Tilni o'zgartirish",
    Ru -> "🌐 Изменить язык",
    Uz -> "🌐 Tilni o'zgartirish"
  )

  def searchResultsHeader(count: Long, lang: Language): String =
    SEARCH_RESULTS_HEADER(lang).replace("{count}", count.toString)

  def formatListing(
      title: String,
      city: String,
      price: BigDecimal,
      rooms: Int,
      size: Option[Int],
      furnished: Boolean,
      contact: String,
      lang: Language,
    ): String = {
    val locationIcon = "📍"
    val priceIcon = "💰"
    val sizeIcon = "📐"
    val roomsIcon = "🏠"
    val furnishedIcon = if (furnished) "🛋️" else "🪑"
    val contactIcon = "📞"

    val (monthlyPer, roomLabel, sizeLabel, furnishedLabel) = lang match {
      case Uz =>
        ("/oy", "xonali", "m²", "Mebelli")
      case Ru =>
        ("/мес", "комнатная", "м²", "Мебелированная")
      case En =>
        ("/month", "-room", "m²", "Furnished")
    }

    val sizeStr = size.map(s => s"\n$sizeIcon ${sizeLabel}: $s").getOrElse("")
    val furnishedStr = if (furnished) s"\n$furnishedIcon $furnishedLabel" else ""

    s"""🏠 $title${if (rooms > 0) s" $rooms$roomLabel" else ""}
$locationIcon $city
$priceIcon $$${price.toInt} $monthlyPer
$roomsIcon $rooms $roomLabel
$sizeStr$furnishedStr
$contactIcon $contact"""
  }
}
