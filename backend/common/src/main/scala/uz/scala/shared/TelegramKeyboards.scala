package uz.scala.shared

import telegramium.bots.InlineKeyboardButton
import telegramium.bots.InlineKeyboardMarkup

object TelegramKeyboards {
  // City selection keyboard
  def citySelectionKeyboard(language: String): InlineKeyboardMarkup = {
    val (cities, otherCity) = language match {
      case "uz" | "uz_UZ" =>
        List("Toshkent", "Samarqand", "Buxoro", "Andijon", "Farg'ona") -> "Boshqa shahar"
      case "ru" | "ru_RU" =>
        List("Ташкент", "Самарканд", "Бухара", "Андижан", "Фергана") -> "Другой город"
      case _ => // English by default
        List("Tashkent", "Samarkand", "Bukhara", "Andijan", "Fergana") -> "Other city"
    }

    val buttons = cities.map(city => InlineKeyboardButton(city, callbackData = Some(s"city_$city")))
    val otherButton = InlineKeyboardButton(otherCity, callbackData = Some("city_other"))

    InlineKeyboardMarkup(
      (buttons :+ otherButton).grouped(2).toList
    )
  }

  // Price range selection keyboard
  def priceRangeSelectionKeyboard(language: String): InlineKeyboardMarkup = {
    val (price200_500, price500_800, price800_1200, price1200_2000, custom) = language match {
      case "uz" | "uz_UZ" =>
        ("200 – 500 $", "500 – 800 $", "800 – 1200 $", "1200 – 2000 $", "O'zi kiritaman")
      case "ru" | "ru_RU" =>
        ("200 – 500 $", "500 – 800 $", "800 – 1200 $", "1200 – 2000 $", "Ввести самому")
      case _ => // English by default
        ("200 – 500 $", "500 – 800 $", "800 – 1200 $", "1200 – 2000 $", "Enter custom")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(price200_500, callbackData = Some("price_200_500")),
        InlineKeyboardButton(price500_800, callbackData = Some("price_500_800"))
      ),
      List(
        InlineKeyboardButton(price800_1200, callbackData = Some("price_800_1200")),
        InlineKeyboardButton(price1200_2000, callbackData = Some("price_1200_2000"))
      ),
      List(
        InlineKeyboardButton(custom, callbackData = Some("price_custom"))
      )
    )

    InlineKeyboardMarkup(buttons)
  }

  // Room count selection keyboard (optional)
  def roomsSelectionKeyboard(language: String): InlineKeyboardMarkup = {
    val (rooms1, rooms2, rooms3, rooms4plus, skip) = language match {
      case "uz" | "uz_UZ" =>
        ("1 xona", "2 xona", "3 xona", "4+ xona", "O'tkazish")
      case "ru" | "ru_RU" =>
        ("1 комната", "2 комнаты", "3 комнаты", "4+ комнаты", "Пропустить")
      case _ => // English by default
        ("1 room", "2 rooms", "3 rooms", "4+ rooms", "Skip")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(rooms1, callbackData = Some("rooms_1")),
        InlineKeyboardButton(rooms2, callbackData = Some("rooms_2"))
      ),
      List(
        InlineKeyboardButton(rooms3, callbackData = Some("rooms_3")),
        InlineKeyboardButton(rooms4plus, callbackData = Some("rooms_4+"))
      ),
      List(
        InlineKeyboardButton(skip, callbackData = Some("rooms_skip"))
      )
    )

    InlineKeyboardMarkup(buttons)
  }

  // Results navigation keyboard
  def resultsNavigationKeyboard(
    hasMore: Boolean,
    language: String
  ): InlineKeyboardMarkup = {
    val (more, newSearch) = language match {
      case "uz" | "uz_UZ" =>
        ("⏭ Yana ko'rsatish", "🔄 Qayta qidirish")
      case "ru" | "ru_RU" =>
        ("⏭ Показать еще", "� Новый поиск")
      case _ => // English by default
        ("⏭ Show more", "🔄 New search")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(newSearch, callbackData = Some("new_search"))
      )
    )

    val navButtons = if (hasMore) {
      List(
        InlineKeyboardButton(more, callbackData = Some("show_more"))
      )
    } else List.empty

    InlineKeyboardMarkup(
      if (navButtons.nonEmpty) navButtons :: buttons else buttons
    )
  }

  // Listing action buttons
  def listingActionButtons(language: String): InlineKeyboardMarkup = {
    val (contact, save, next) = language match {
      case "uz" | "uz_UZ" =>
        ("📞 Bog'lanish", "❤️ Saqlash", "➡️ Keyingisi")
      case "ru" | "ru_RU" =>
        ("📞 Связаться", "❤️ Сохранить", "➡️ Следующий")
      case _ => // English by default
        ("📞 Contact", "❤️ Save", "➡️ Next")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(contact, callbackData = Some("contact")),
        InlineKeyboardButton(save, callbackData = Some("save"))
      ),
      List(
        InlineKeyboardButton(next, callbackData = Some("next"))
      )
    )

    InlineKeyboardMarkup(buttons)
  }
}