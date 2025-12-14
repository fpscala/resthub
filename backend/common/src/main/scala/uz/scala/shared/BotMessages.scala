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

💡 Maslahat: Qulayroq qidirish uchun interaktiv tugmalardan foydalaning!"""
  )

  val SELECT_CITY: Map[Language, String] = Map(
    En -> "📍 Which city are you looking for rentals in?",
    Ru -> "📍 В каком городе вы ищете жилье для аренды?",
    Uz -> "📍 Qaysi shaharda ijaraga uy qidiryapsiz?"
  )

  val ENTER_CUSTOM_CITY: Map[Language, String] = Map(
    En -> "✍️ Please type the city name:",
    Ru -> "✍️ Пожалуйста, введите название города:",
    Uz -> "✍️ Iltimos, shahar nomini kiriting:"
  )

  val SELECT_PRICE_RANGE: Map[Language, String] = Map(
    En -> "💰 What's your monthly budget? (USD)",
    Ru -> "💰 Какой ваш месячный бюджет? (USD)",
    Uz -> "💰 Oylik byudjetingiz qancha? (USD)"
  )

  val SELECT_ROOMS: Map[Language, String] = Map(
    En -> "🏠 How many rooms do you need? (Optional)",
    Ru -> "🏠 Сколько комнат вам нужно? (Необязательно)",
    Uz -> "🏠 Nechta xona kerak? (Ixtiyoriy)"
  )

  val SEARCHING: Map[Language, String] = Map(
    En -> "🔍 Searching for perfect matches...",
    Ru -> "🔍 Поиск подходящих вариантов...",
    Uz -> "🔍 Mos variantlar qidirilmoqda..."
  )

  val SEARCH_RESULTS_HEADER: Map[Language, String] = Map(
    En -> "🎉 Found {count} properties for you!",
    Ru -> "🎉 Найдено {count} объектов для вас!",
    Uz -> "🎉 Siz uchun {count} ta uy topildi!"
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
• Filterlarni olib tashlash"""
  )

  val INVALID_PRICE: Map[Language, String] = Map(
    En -> "❌ Please enter a valid number. Example: 500",
    Ru -> "❌ Пожалуйста, введите корректное число. Пример: 500",
    Uz -> "❌ Iltimos, to'g'ri son kiriting. Masalan: 500"
  )

  val INVALID_PRICE_RANGE: Map[Language, String] = Map(
    En -> "❌ Minimum price should be less than maximum price. Please try again.",
    Ru -> "❌ Минимальная цена должна быть меньше максимальной. Попробуйте еще раз.",
    Uz -> "❌ Minimal narx maksimal narxdan kichik bo'lishi kerak. Qaytadan urinib ko'ring."
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
    lang: Language
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
