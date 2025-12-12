package uz.scala.shared

import uz.scala.Language
import uz.scala.Language._

object BotMessages {
  val WELCOME_NEW: Map[Language, String] = Map(
    En -> """Welcome to NestHub! 🏠

I can help you find rental properties in Uzbekistan.

Available commands:
/search - Search for homes
/help - Show help
""",
    Ru -> """Добро пожаловать в NestHub! 🏠

Я помогу вам найти арендное жилье в Узбекистане.

Доступные команды:
/search - Поиск жилья
/help - Помощь
""",
    Uz -> """NestHub botiga xush kelibsiz! 🏠

Men sizga Oʻzbekistonda ijaraga uy topishda yordam beraman.

Mavjud buyruqlar:
/search - Uy qidirish
/help - Yordam
"""
  )

  val SELECT_CITY: Map[Language, String] = Map(
    En -> "Which city are you looking in?",
    Ru -> "В каком городе ищете жилье?",
    Uz -> "Qaysi shahardan uy qidiryapsiz?"
  )

  val SELECT_PRICE_RANGE: Map[Language, String] = Map(
    En -> "What is your price range per month?",
    Ru -> "Какой диапазон цен в месяц?",
    Uz -> "Oyiga qancha narxdagi uy kerak?"
  )

  val SEARCH_RESULTS: Map[Language, String] = Map(
    En -> "Found {count} listings:",
    Ru -> "Найдено {count} объявлений:",
    Uz -> "{count} ta e'lon topildi:"
  )

  val NO_RESULTS: Map[Language, String] = Map(
    En -> "No listings found. Try different filters.",
    Ru -> "Объявления не найдены. Попробуйте другие фильтры.",
    Uz -> "E'lon topilmadi. Boshqa filterlar bilan qidiring."
  )

  def searchResults(count: Int, lang: Language): String =
    SEARCH_RESULTS(lang).replace("{count}", count.toString)
}
