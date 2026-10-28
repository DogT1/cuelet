package com.tuvin.pixelautomations.setup

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** The app's language setting. SYSTEM follows the phone's language. */
enum class AppLanguage { SYSTEM, ENGLISH, HEBREW, RUSSIAN, GERMAN, FRENCH, SPANISH, ITALIAN }

/**
 * Small in-app translation layer: UI text is written in English and wrapped in [t]; other languages come from
 * their own string maps. Any string without a translation falls back to English, so a partially translated
 * language still works. Switching language is instant: Compose reads [active], which is snapshot state.
 */
object I18n {
    private const val PREFS = "settings"
    private const val KEY = "language"

    var setting by mutableStateOf(AppLanguage.SYSTEM)
        private set

    /** True while the app shows Hebrew (right-to-left). */
    var hebrew by mutableStateOf(false)
        private set

    /** The active language's map, or null for English. Snapshot state so `t()` recomposes on change. */
    private var active by mutableStateOf<Map<String, String>?>(null)

    private fun stringsFor(language: AppLanguage): Map<String, String>? = when (language) {
        AppLanguage.HEBREW -> HebrewStrings.map
        AppLanguage.RUSSIAN -> RussianStrings.map
        AppLanguage.GERMAN -> GermanStrings.map
        AppLanguage.FRENCH -> FrenchStrings.map
        AppLanguage.SPANISH -> SpanishStrings.map
        AppLanguage.ITALIAN -> ItalianStrings.map
        else -> null // English, or SYSTEM that resolved to English
    }

    /** The phone's language → one of ours (Android reports Hebrew as "he", older code "iw"). */
    private fun forLocale(): AppLanguage = when (Locale.getDefault().language) {
        "he", "iw" -> AppLanguage.HEBREW
        "ru" -> AppLanguage.RUSSIAN
        "de" -> AppLanguage.GERMAN
        "fr" -> AppLanguage.FRENCH
        "es" -> AppLanguage.SPANISH
        "it" -> AppLanguage.ITALIAN
        else -> AppLanguage.ENGLISH
    }

    fun load(ctx: Context) {
        val saved = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        apply(saved?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.SYSTEM)
    }

    fun set(ctx: Context, language: AppLanguage) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language.name).apply()
        apply(language)
    }

    private fun apply(language: AppLanguage) {
        setting = language
        val effective = if (language == AppLanguage.SYSTEM) forLocale() else language
        hebrew = effective == AppLanguage.HEBREW
        active = stringsFor(effective)
    }

    /** For tests: force Hebrew (true) or English (false) without Android. */
    fun useForTest(hebrewOn: Boolean) {
        hebrew = hebrewOn
        active = if (hebrewOn) HebrewStrings.map else null
    }

    fun t(english: String, vararg args: Any?): String {
        val pattern = active?.get(english) ?: english
        return if (args.isEmpty()) pattern else pattern.format(*args)
    }
}

/** Shorthand used across the UI and labels. */
fun t(english: String, vararg args: Any?): String = I18n.t(english, *args)

/** Days in display order: Sunday first in Hebrew (Israeli week), Monday first in English. ISO numbers (1 = Monday). */
fun weekOrder(): List<Int> = if (I18n.hebrew) listOf(7, 1, 2, 3, 4, 5, 6) else (1..7).toList()

/** One-letter day name for an ISO day number. */
fun dayLetter(isoDay: Int): String =
    (if (I18n.hebrew) listOf("ב", "ג", "ד", "ה", "ו", "ש", "א") else listOf("M", "T", "W", "T", "F", "S", "S"))[isoDay - 1]
