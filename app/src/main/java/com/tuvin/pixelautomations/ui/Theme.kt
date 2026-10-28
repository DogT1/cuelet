package com.tuvin.pixelautomations.ui

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * The app's own brand, not the Pixel wallpaper's dynamic color: teal primary with a warm orange accent,
 * flat and high-contrast. One place, so every screen stays consistent (ui-ux-pro-max: centralized theme).
 */
object Brand {
    val Teal = Color(0xFF0D9488)
    val Orange = Color(0xFFEA580C)

    val Light: ColorScheme = lightColorScheme(
        primary = Teal,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFA7F3E4),
        onPrimaryContainer = Color(0xFF00201B),
        secondary = Color(0xFF4A635E),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCCE8E1),
        onSecondaryContainer = Color(0xFF06201C),
        tertiary = Orange,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFDBC8),
        onTertiaryContainer = Color(0xFF351000),
        background = Color(0xFFF5FAF8),
        onBackground = Color(0xFF171D1C),
        surface = Color(0xFFF5FAF8),
        onSurface = Color(0xFF171D1C),
        surfaceVariant = Color(0xFFDAE5E1),
        onSurfaceVariant = Color(0xFF3F4946),
        surfaceContainer = Color(0xFFEAF1EE),
        surfaceContainerHigh = Color(0xFFE4EBE8),
        outline = Color(0xFF6F7976),
        outlineVariant = Color(0xFFBEC9C4),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
    )

    val Dark: ColorScheme = darkColorScheme(
        primary = Color(0xFF5BD6C5),
        onPrimary = Color(0xFF003731),
        primaryContainer = Color(0xFF005048),
        onPrimaryContainer = Color(0xFFA7F3E4),
        secondary = Color(0xFFB1CCC5),
        onSecondary = Color(0xFF1C3531),
        secondaryContainer = Color(0xFF334B47),
        onSecondaryContainer = Color(0xFFCCE8E1),
        tertiary = Color(0xFFFFB68E),
        onTertiary = Color(0xFF561F00),
        tertiaryContainer = Color(0xFF7A2E00),
        onTertiaryContainer = Color(0xFFFFDBC8),
        background = Color(0xFF0E1513),
        onBackground = Color(0xFFDEE4E1),
        surface = Color(0xFF0E1513),
        onSurface = Color(0xFFDEE4E1),
        surfaceVariant = Color(0xFF3F4946),
        onSurfaceVariant = Color(0xFFBEC9C4),
        surfaceContainer = Color(0xFF1B211F),
        surfaceContainerHigh = Color(0xFF252B29),
        outline = Color(0xFF899390),
        outlineVariant = Color(0xFF3F4946),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )
}

/** Pure-black surfaces for OLED screens (AMOLED): the dark brand with black background and near-black cards. */
val Amoled: ColorScheme = Brand.Dark.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainer = Color(0xFF0C0C0C),
    surfaceContainerHigh = Color(0xFF161616),
    surfaceVariant = Color(0xFF1C1C1C),
)

/** The theme mode the user picked. SYSTEM follows the phone's light/dark setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

/** The theme choice, saved on the phone. Snapshot state so the app recolours the moment it changes. */
object ThemeSettings {
    private const val PREFS = "settings"
    private const val KEY = "theme_mode"

    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun load(ctx: Context) {
        val saved = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        mode = saved?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    fun set(ctx: Context, value: ThemeMode) {
        mode = value
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, value.name).apply()
    }

    /** The scheme to apply now. [systemDark] is the phone's dark-mode setting (used only for SYSTEM). */
    fun scheme(systemDark: Boolean): ColorScheme = when (mode) {
        ThemeMode.SYSTEM -> if (systemDark) Brand.Dark else Brand.Light
        ThemeMode.LIGHT -> Brand.Light
        ThemeMode.DARK -> Brand.Dark
        ThemeMode.AMOLED -> Amoled
    }

    /** Whether the current theme is a dark one (for status-bar icon colour, etc.). */
    fun isDark(systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        else -> true
    }
}

/** A small, meaningful palette for the per-automation icon tiles, so the list reads at a glance. */
object TileColors {
    val Time = Color(0xFF0D9488)      // teal — time/alarm/sleep/workout
    val Place = Color(0xFFEA580C)     // orange — arrive/leave/commute/car
    val Comms = Color(0xFF4F46E5)     // indigo — sms/email/message
    val Connect = Color(0xFF0891B2)   // cyan — wifi/bluetooth/airplane/charger/battery/focus
    val Device = Color(0xFF7C3AED)    // violet — app/nfc/sound/transaction
}
