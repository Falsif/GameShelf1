package com.lunexa.gameshelf.ui.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val RuLocale: Locale = Locale.forLanguageTag("ru-RU")

/** Варианты выбора платформ и жанров в формах. */
val PlatformPresets = listOf(
    "PC", "PlayStation 5", "PlayStation 4", "Xbox Series X|S", "Xbox One",
    "Nintendo Switch", "Steam Deck", "Android", "iOS", "Другое"
)

val GenrePresets = listOf(
    "Экшен", "Приключения", "RPG", "Шутер", "Стратегия", "Симулятор", "Гонки",
    "Спорт", "Головоломка", "Платформер", "Хоррор", "Файтинг", "Инди", "MMO",
    "Песочница", "Выживание"
)

fun formatMinutes(total: Int): String {
    if (total <= 0) return "0 мин"
    val h = total / 60
    val m = total % 60
    return when {
        h == 0 -> "$m мин"
        m == 0 -> "$h ч"
        else -> "$h ч $m мин"
    }
}

fun formatHoursShort(total: Int): String {
    val h = total / 60.0
    return if (h >= 10 || total % 60 == 0) "${total / 60} ч" else String.format(RuLocale, "%.1f ч", h)
}

fun formatDate(ms: Long?): String =
    ms?.let { SimpleDateFormat("d MMMM yyyy", RuLocale).format(Date(it)) } ?: "—"

fun formatShortDate(ms: Long?): String =
    ms?.let { SimpleDateFormat("dd.MM.yyyy", RuLocale).format(Date(it)) } ?: "—"

fun formatDateTime(ms: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", RuLocale).format(Date(ms))

fun yearOf(ms: Long?): Int? = ms?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.YEAR) }

fun formatPrice(value: Double?): String {
    if (value == null) return "—"
    return if (value % 1.0 == 0.0) "${value.toLong()} ₽" else String.format(RuLocale, "%.2f ₽", value)
}

/** Склонение: 1 игра, 2 игры, 5 игр. */
fun plural(n: Int, one: String, few: String, many: String): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> one
        mod10 in 2..4 && mod100 !in 12..14 -> few
        else -> many
    }
}

fun gamesCount(n: Int) = "$n ${plural(n, "игра", "игры", "игр")}"

/** DatePicker отдаёт полночь UTC — переводим в локальный полдень того же дня. */
fun pickerToLocal(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
    }.timeInMillis
}

/** Локальная дата → полночь UTC для начального значения DatePicker. */
fun localToPicker(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
    }.timeInMillis
}

val MonthShort = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
