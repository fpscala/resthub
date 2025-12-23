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

  // Settings keyboard (language change disabled)
  def settingsKeyboard(language: Language): InlineKeyboardMarkup = {
    val changeModeText = BotMessages.CHANGE_MODE(language)

    val buttons = List(
      List(
        InlineKeyboardButton(changeModeText, callbackData = Some("change_mode"))
      )
    )

    InlineKeyboardMarkup(buttons)
  }

  // Mode change confirmation keyboard
  def modeChangeConfirmationKeyboard(language: Language): InlineKeyboardMarkup = {
    val (confirmText, cancelText) = language match {
      case Uz =>
        ("✅ Ha, o'zgartirish", "❌ Yo'q, bekor qilish")
      case Ru =>
        ("✅ Да, изменить", "❌ Нет, отменить")
      case En =>
        ("✅ Yes, change", "❌ No, cancel")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(confirmText, callbackData = Some("confirm_change_mode"))
      ),
      List(
        InlineKeyboardButton(cancelText, callbackData = Some("cancel_change_mode"))
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

  // Mode-specific home keyboards
  def buyerHomeKeyboard(language: Language): InlineKeyboardMarkup = {
    val (searchByCity, settings) = language match {
      case Uz =>
        ("🏙️ Shahar bo'yicha qidirish", "⚙️ Sozlamalar")
      case Ru =>
        ("🏙️ Поиск по городу", "⚙️ Настройки")
      case En =>
        ("🏙️ Search by city", "⚙️ Settings")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(searchByCity, callbackData = Some("start_search"))
      ),
      List(
        InlineKeyboardButton(settings, callbackData = Some("open_settings"))
      )
    )

    InlineKeyboardMarkup(buttons)
  }

  def brokerHomeKeyboard(language: Language): InlineKeyboardMarkup = {
    val (createListing, manageDrafts, postToChannel, settings) = language match {
      case Uz =>
        ("📝 Yangi e'lon", "📋 Qoralamalar", "📢 Kanalga joylash", "⚙️ Sozlamalar")
      case Ru =>
        ("📝 Новое объявление", "📋 Черновики", "📢 Разместить в канал", "⚙️ Настройки")
      case En =>
        ("📝 New listing", "📋 Manage drafts", "📢 Post to channel", "⚙️ Settings")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(createListing, callbackData = Some("admin_post"))
      ),
      List(
        InlineKeyboardButton(manageDrafts, callbackData = Some("manage_drafts"))
      ),
      List(
        InlineKeyboardButton(postToChannel, callbackData = Some("post_to_channel"))
      ),
      List(
        InlineKeyboardButton(settings, callbackData = Some("open_settings"))
      )
    )

    InlineKeyboardMarkup(buttons)
  }

  // Listing type keyboard for admin posting
  def listingTypeKeyboard(language: Language): InlineKeyboardMarkup = {
    val (forRent, forSale) = language match {
      case Uz =>
        ("🏢 Ijaraga", "🏡 Sotishga")
      case Ru =>
        ("🏢 В аренду", "🏡 На продажу")
      case En =>
        ("🏢 For Rent", "🏡 For Sale")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(forRent, callbackData = Some("listing_type_apartment"))
      ),
      List(
        InlineKeyboardButton(forSale, callbackData = Some("listing_type_house"))
      )
    )

    InlineKeyboardMarkup(buttons)
  }

  // Legacy keyboards for backward compatibility
  def buyerModeKeyboard(language: Language): InlineKeyboardMarkup = buyerHomeKeyboard(language)

  def brokerModeKeyboard(language: Language): InlineKeyboardMarkup = brokerHomeKeyboard(language)

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

  // ============================================================
  // BROKER POSTING - BUTTON-FIRST UX KEYBOARDS
  // ============================================================

  // Price selection keyboard with preset amounts
  def priceKeyboard(language: Language): InlineKeyboardMarkup = {
    val (price200, price300, price500, custom, skip) = language match {
      case Uz =>
        ("💰 200$", "💰 300$", "💰 500$", "✍️ Boshqa summa", "⏭ Oʻtkazib yuborish")
      case Ru =>
        ("💰 200$", "💰 300$", "💰 500$", "✍️ Другая сумма", "⏭ Пропустить")
      case En =>
        ("💰 200$", "💰 300$", "💰 500$", "✍️ Other amount", "⏭ Skip")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(price200, callbackData = Some("price_200")),
        InlineKeyboardButton(price300, callbackData = Some("price_300")),
        InlineKeyboardButton(price500, callbackData = Some("price_500")),
      ),
      List(
        InlineKeyboardButton(custom, callbackData = Some("price_custom"))
      ),
      List(
        InlineKeyboardButton(skip, callbackData = Some("price_skip"))
      ),
    )

    InlineKeyboardMarkup(buttons)
  }

  // City selection keyboard with major cities
  def cityKeyboard(language: Language): InlineKeyboardMarkup = {
    val (tashkent, samarqand, andijon, bukhara, custom) = language match {
      case Uz =>
        ("📍 Toshkent", "📍 Samarqand", "📍 Andijon", "📍 Buxoro", "✍️ Boshqa")
      case Ru =>
        ("📍 Ташкент", "📍 Самарканд", "📍 Андижан", "📍 Бухара", "✍️ Другой")
      case En =>
        ("📍 Tashkent", "📍 Samarkand", "📍 Andijan", "📍 Bukhara", "✍️ Other")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(tashkent, callbackData = Some("city_tashkent")),
        InlineKeyboardButton(samarqand, callbackData = Some("city_samarqand")),
      ),
      List(
        InlineKeyboardButton(andijon, callbackData = Some("city_andijan")),
        InlineKeyboardButton(bukhara, callbackData = Some("city_bukhara")),
      ),
      List(
        InlineKeyboardButton(custom, callbackData = Some("city_custom"))
      ),
    )

    InlineKeyboardMarkup(buttons)
  }

  // Phone number keyboard with contact request
  def phoneKeyboard(language: Language): InlineKeyboardMarkup = {
    val (shareContact, manual, skip) = language match {
      case Uz =>
        ("📲 Telegram raqamni yuborish", "✍️ Qo'lda kiritish", "⏭ Oʻtkazib yuborish")
      case Ru =>
        ("📲 Отправить номер", "✍️ Вручную", "⏭ Пропустить")
      case En =>
        ("📲 Share Telegram number", "✍️ Enter manually", "⏭ Skip")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(shareContact, callbackData = Some("phone_share_contact"))
      ),
      List(
        InlineKeyboardButton(manual, callbackData = Some("phone_manual"))
      ),
      List(
        InlineKeyboardButton(skip, callbackData = Some("phone_skip"))
      ),
    )

    InlineKeyboardMarkup(buttons)
  }

  // District keyboard with popular districts
  def districtKeyboard(language: Language): InlineKeyboardMarkup = {
    val (chilonzor, sergeli, yunusobod, custom, skip) = language match {
      case Uz =>
        ("🏙 Chilonzor", "🏙 Sergeli", "🏙 Yunusobod", "✍️ Boshqa", "⏭ Oʻtkazib yuborish")
      case Ru =>
        ("🏙 Чиланзар", "🏙 Сергели", "🏙 Юнусабад", "✍️ Другой", "⏭ Пропустить")
      case En =>
        ("🏙 Chilonzor", "🏙 Sergeli", "🏙 Yunusobod", "✍️ Other", "⏭ Skip")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(chilonzor, callbackData = Some("district_chilonzor")),
        InlineKeyboardButton(sergeli, callbackData = Some("district_sergeli")),
      ),
      List(
        InlineKeyboardButton(yunusobod, callbackData = Some("district_yunusobod")),
      ),
      List(
        InlineKeyboardButton(custom, callbackData = Some("district_custom"))
      ),
      List(
        InlineKeyboardButton(skip, callbackData = Some("district_skip"))
      ),
    )

    InlineKeyboardMarkup(buttons)
  }

  // Floor selection keyboard
  def floorKeyboard(language: Language): InlineKeyboardMarkup = {
    val (floor1, floor2, floor3, floor4plus, skip) = language match {
      case Uz =>
        ("1", "2", "3", "4+", "⏭ Oʻtkazib yuborish")
      case Ru =>
        ("1", "2", "3", "4+", "⏭ Пропустить")
      case En =>
        ("1", "2", "3", "4+", "⏭ Skip")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(floor1, callbackData = Some("floor_1")),
        InlineKeyboardButton(floor2, callbackData = Some("floor_2")),
        InlineKeyboardButton(floor3, callbackData = Some("floor_3")),
        InlineKeyboardButton(floor4plus, callbackData = Some("floor_4+")),
      ),
      List(
        InlineKeyboardButton(skip, callbackData = Some("floor_skip"))
      ),
    )

    InlineKeyboardMarkup(buttons)
  }

  // Total floors selection keyboard
  def totalFloorsKeyboard(language: Language): InlineKeyboardMarkup = {
    val (floors3, floors5, floors9, skip) = language match {
      case Uz =>
        ("3", "5", "9", "⏭ Oʻtkazib yuborish")
      case Ru =>
        ("3", "5", "9", "⏭ Пропустить")
      case En =>
        ("3", "5", "9", "⏭ Skip")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(floors3, callbackData = Some("total_floors_3")),
        InlineKeyboardButton(floors5, callbackData = Some("total_floors_5")),
        InlineKeyboardButton(floors9, callbackData = Some("total_floors_9")),
      ),
      List(
        InlineKeyboardButton(skip, callbackData = Some("total_floors_skip"))
      ),
    )

    InlineKeyboardMarkup(buttons)
  }

  // Building type selection keyboard
  def buildingTypeKeyboard(language: Language): InlineKeyboardMarkup = {
    val (apartment, house, office, skip) = language match {
      case Uz =>
        ("🏢 Kvartira", "🏠 Hovli", "🏢 Ofis", "⏭ Oʻtkazib yuborish")
      case Ru =>
        ("🏢 Квартира", "🏠 Дом", "🏢 Офис", "⏭ Пропустить")
      case En =>
        ("🏢 Apartment", "🏠 House", "🏢 Office", "⏭ Skip")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(apartment, callbackData = Some("building_apartment")),
        InlineKeyboardButton(house, callbackData = Some("building_house")),
      ),
      List(
        InlineKeyboardButton(office, callbackData = Some("building_office"))
      ),
      List(
        InlineKeyboardButton(skip, callbackData = Some("building_skip"))
      ),
    )

    InlineKeyboardMarkup(buttons)
  }

  // Condition selection keyboard
  def conditionKeyboard(language: Language): InlineKeyboardMarkup = {
    val (good, excellent, needsRepair, skip) = language match {
      case Uz =>
        ("✅ Yaxshi", "⭐ Zo'r", "🛠 Ta'mir talab", "⏭ Oʻtkazib yuborish")
      case Ru =>
        ("✅ Хорошее", "⭐ Отличное", "🛠 Требует ремонта", "⏭ Пропустить")
      case En =>
        ("✅ Good", "⭐ Excellent", "🛠 Needs repair", "⏭ Skip")
    }

    val buttons = List(
      List(
        InlineKeyboardButton(good, callbackData = Some("condition_good")),
        InlineKeyboardButton(excellent, callbackData = Some("condition_excellent")),
        InlineKeyboardButton(needsRepair, callbackData = Some("condition_needs_repair")),
      ),
      List(
        InlineKeyboardButton(skip, callbackData = Some("condition_skip"))
      ),
    )

    InlineKeyboardMarkup(buttons)
  }
}
