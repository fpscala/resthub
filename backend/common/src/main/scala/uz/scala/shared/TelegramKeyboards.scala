package uz.scala.shared

import telegramium.bots.InlineKeyboardButton
import telegramium.bots.InlineKeyboardMarkup

import uz.scala.Language
import uz.scala.Language._
import uz.scala.shared.BotMessages

object TelegramKeyboards {
  // Main menu keyboard for /start command
  def mainMenuKeyboard(language: Language): InlineKeyboardMarkup = {
    val (searchText, helpText, settingsText) = language match {
      case Uz =>
        ("🔍 Uylarni qidirish", "ℹ️ Yordam", "⚙️ Sozlamalar")
      case Ru =>
        ("🔍 Поиск жилья", "ℹ️ Помощь", "⚙️ Настройки")
      case En => // English by default
        ("🔍 Search for homes", "ℹ️ Help", "⚙️ Settings")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(searchText, callbackData = Some("start_search"))
      ),
      List(
        InlineKeyboardButton(helpText, callbackData = Some("show_help")),
        InlineKeyboardButton(settingsText, callbackData = Some("open_settings"))
      )
    )

    InlineKeyboardMarkup(buttons)
  }

  // Mode selection keyboard
  def modeSelectionKeyboard(language: Language): InlineKeyboardMarkup = {
    val buyerModeText = BotMessages.BUYER_MODE_DESCRIPTION(language)
    val brokerModeText = BotMessages.BROKER_MODE_DESCRIPTION(language)

    val buttons = List(
      List(
        InlineKeyboardButton(buyerModeText, callbackData = Some("select_buyer_mode"))
      ),
      List(
        InlineKeyboardButton(brokerModeText, callbackData = Some("select_broker_mode"))
      )
    )

    InlineKeyboardMarkup(buttons)
  }

  // Settings keyboard
  def settingsKeyboard(language: Language): InlineKeyboardMarkup = {
    val changeModeText = BotMessages.CHANGE_MODE(language)
    val changeLanguageText = BotMessages.CHANGE_LANGUAGE(language)

    val buttons = List(
      List(
        InlineKeyboardButton(changeModeText, callbackData = Some("change_mode"))
      ),
      List(
        InlineKeyboardButton(changeLanguageText, callbackData = Some("change_language"))
      )
    )

    InlineKeyboardMarkup(buttons)
  }

  // Quick actions keyboard (shown during search process)
  def quickActionsKeyboard(language: Language): InlineKeyboardMarkup = {
    val (newSearch, help, backToMenu) = language match {
      case Uz =>
        ("🔄 Qayta qidirish", "ℹ️ Yordam", "🏠 Bosh menyu")
      case Ru =>
        ("🔄 Новый поиск", "ℹ️ Помощь", "🏠 Главное меню")
      case En => // English by default
        ("🔄 New search", "ℹ️ Help", "🏠 Main menu")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(newSearch, callbackData = Some("new_search"))
      ),
      List(
        InlineKeyboardButton(help, callbackData = Some("show_help")),
        InlineKeyboardButton(backToMenu, callbackData = Some("back_to_menu"))
      )
    )

    InlineKeyboardMarkup(buttons)
  }

  // Mode-specific keyboards
  def buyerModeKeyboard(language: Language): InlineKeyboardMarkup = {
    val (searchText, settingsText) = language match {
      case Uz =>
        ("🔍 Uylarni qidirish", "⚙️ Sozlamalar")
      case Ru =>
        ("🔍 Поиск жилья", "⚙️ Настройки")
      case En =>
        ("🔍 Search for homes", "⚙️ Settings")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(searchText, callbackData = Some("start_search"))
      ),
      List(
        InlineKeyboardButton(settingsText, callbackData = Some("open_settings"))
      )
    )

    InlineKeyboardMarkup(buttons)
  }

  def brokerModeKeyboard(language: Language): InlineKeyboardMarkup = {
    val (postText, settingsText) = language match {
      case Uz =>
        ("📝 E'lon joylash", "⚙️ Sozlamalar")
      case Ru =>
        ("📝 Разместить объявление", "⚙️ Настройки")
      case En =>
        ("📝 Post listing", "⚙️ Settings")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(postText, callbackData = Some("admin_post"))
      ),
      List(
        InlineKeyboardButton(settingsText, callbackData = Some("open_settings"))
      )
    )

    InlineKeyboardMarkup(buttons)
  }

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
