package uz.scala.shared

import telegramium.bots.InlineKeyboardButton
import telegramium.bots.InlineKeyboardMarkup

import uz.scala.Language
import uz.scala.Language._

object TelegramKeyboards {
  // City selection keyboard
  def citySelectionKeyboard(language: Language): InlineKeyboardMarkup = {
    val (cities, otherCity) = language match {
      case Uz =>
        List("Toshkent", "Samarqand", "Buxoro", "Andijon", "Farg'ona") -> "Boshqa shahar"
      case Ru =>
        List("Ташкент", "Самарканд", "Бухара", "Андижан", "Фергана") -> "Другой город"
      case En => // English by default
        List("Tashkent", "Samarkand", "Bukhara", "Andijan", "Fergana") -> "Other city"
    }

    val buttons = cities.map(city => InlineKeyboardButton(city, callbackData = Some(s"city_$city")))
    val otherButton = InlineKeyboardButton(otherCity, callbackData = Some("city_other"))

    InlineKeyboardMarkup(
      (buttons :+ otherButton).grouped(2).toList
    )
  }

  // Price range selection keyboard
  def priceRangeSelectionKeyboard(language: Language): InlineKeyboardMarkup = {
    val (price200_500, price500_800, price800_1200, price1200_2000, custom) = language match {
      case Uz =>
        ("200 – 500 $", "500 – 800 $", "800 – 1200 $", "1200 – 2000 $", "O'zi kiritaman")
      case Ru =>
        ("200 – 500 $", "500 – 800 $", "800 – 1200 $", "1200 – 2000 $", "Ввести самому")
      case En => // English by default
        ("200 – 500 $", "500 – 800 $", "800 – 1200 $", "1200 – 2000 $", "Enter custom")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(price200_500, callbackData = Some("price_200_500")),
        InlineKeyboardButton(price500_800, callbackData = Some("price_500_800")),
      ),
      List(
        InlineKeyboardButton(price800_1200, callbackData = Some("price_800_1200")),
        InlineKeyboardButton(price1200_2000, callbackData = Some("price_1200_2000")),
      ),
      List(
        InlineKeyboardButton(custom, callbackData = Some("price_custom"))
      ),
    )

    InlineKeyboardMarkup(buttons)
  }

  // Room count selection keyboard (optional)
  def roomsSelectionKeyboard(language: Language): InlineKeyboardMarkup = {
    val (rooms1, rooms2, rooms3, rooms4plus, skip) = language match {
      case Uz =>
        ("1 xona", "2 xona", "3 xona", "4+ xona", "O'tkazish")
      case Ru =>
        ("1 комната", "2 комнаты", "3 комнаты", "4+ комнаты", "Пропустить")
      case En => // English by default
        ("1 room", "2 rooms", "3 rooms", "4+ rooms", "Skip")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(rooms1, callbackData = Some("rooms_1")),
        InlineKeyboardButton(rooms2, callbackData = Some("rooms_2")),
      ),
      List(
        InlineKeyboardButton(rooms3, callbackData = Some("rooms_3")),
        InlineKeyboardButton(rooms4plus, callbackData = Some("rooms_4+")),
      ),
      List(
        InlineKeyboardButton(skip, callbackData = Some("rooms_skip"))
      ),
    )

    InlineKeyboardMarkup(buttons)
  }

  // Results navigation keyboard
  def resultsNavigationKeyboard(
      hasMore: Boolean,
      language: Language,
    ): InlineKeyboardMarkup = {
    val (more, newSearch) = language match {
      case Uz =>
        ("⏭ Yana ko'rsatish", "🔄 Qayta qidirish")
      case Ru =>
        ("⏭ Показать еще", "🔄 Новый поиск")
      case En => // English by default
        ("⏭ Show more", "🔄 New search")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(newSearch, callbackData = Some("new_search"))
      )
    )

    val navButtons =
      if (hasMore)
        List(
          InlineKeyboardButton(more, callbackData = Some("show_more"))
        )
      else List.empty

    InlineKeyboardMarkup(
      if (navButtons.nonEmpty) navButtons :: buttons else buttons
    )
  }

  // Listing action buttons
  def listingActionButtons(language: Language): InlineKeyboardMarkup = {
    val (contact, save, next) = language match {
      case Uz =>
        ("📞 Bog'lanish", "❤️ Saqlash", "➡️ Keyingisi")
      case Ru =>
        ("📞 Связаться", "❤️ Сохранить", "➡️ Следующий")
      case En => // English by default
        ("📞 Contact", "❤️ Save", "➡️ Next")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(contact, callbackData = Some("contact")),
        InlineKeyboardButton(save, callbackData = Some("save")),
      ),
      List(
        InlineKeyboardButton(next, callbackData = Some("next"))
      ),
    )

    InlineKeyboardMarkup(buttons)
  }
}
