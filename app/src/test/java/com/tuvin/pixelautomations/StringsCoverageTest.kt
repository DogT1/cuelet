package com.tuvin.pixelautomations

import com.tuvin.pixelautomations.model.Action
import com.tuvin.pixelautomations.model.LevelCondition
import com.tuvin.pixelautomations.model.Place
import com.tuvin.pixelautomations.model.SolarEvent
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.model.label
import com.tuvin.pixelautomations.setup.FrenchStrings
import com.tuvin.pixelautomations.setup.GermanStrings
import com.tuvin.pixelautomations.setup.HebrewStrings
import com.tuvin.pixelautomations.setup.I18n
import com.tuvin.pixelautomations.setup.ItalianStrings
import com.tuvin.pixelautomations.setup.RussianStrings
import com.tuvin.pixelautomations.setup.SpanishStrings
import com.tuvin.pixelautomations.setup.dayLetter
import com.tuvin.pixelautomations.setup.weekOrder
import org.junit.After
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every English string the app shows must have a Hebrew translation with the same placeholders. */
class StringsCoverageTest {
    @After fun english() = I18n.useForTest(false)

    private val source: String = File("src/main/java").walkTopDown()
        .filter { it.extension == "kt" && it.name !in setOf("HebrewStrings.kt", "I18n.kt") }
        .joinToString("\n") { it.readText() }

    private val literal = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"")

    /** Strings handed to t(), to the translating helpers, the action catalog and the Kind labels. */
    private fun displayedStrings(): Set<String> {
        val found = mutableSetOf<String>()
        fun addFrom(pattern: Regex, whole: Boolean = false) = pattern.findAll(source).forEach { m ->
            if (whole) literal.findAll(m.value).forEach { found += it.groupValues[1] } else found += m.groupValues[1]
        }
        addFrom(Regex("\\bt\\(\\s*\"((?:[^\"\\\\]|\\\\.)*)\""))
        addFrom(Regex("\\bt\\(\\s*(?:if|when)[^\\n]*"), whole = true)
        addFrom(Regex("(?:Choice|Section|TimeField|PlacePicker)\\(\\s*\"((?:[^\"\\\\]|\\\\.)*)\""))
        addFrom(Regex("Field\\([^,\\n]+,\\s*\"((?:[^\"\\\\]|\\\\.)*)\""))
        addFrom(Regex("(?m)^\\s*\"((?:[^\"\\\\]|\\\\.)*)\" to (?:\\{|listOf)"))
        addFrom(Regex("Kind\\.\\w+ -> \"((?:[^\"\\\\]|\\\\.)*)\""))
        addFrom(Regex("\\b[A-Z_]+\\(\"((?:[^\"\\\\]|\\\\.)*)\"\\)"))
        // Kotlin escapes in the source (\" and \n) are real characters at runtime.
        return found.map { it.replace("\\\"", "\"").replace("\\n", "\n") }
            // Skip number formats ("%.5f, %.5f") and Kotlin templates ("${...}"), which aren't words to translate.
            .filter { s -> s.any { it.isLetter() } && !s.startsWith("%02d") && !s.startsWith("%.") && !s.contains("\${") }
            .toSet()
    }

    @Test fun everyDisplayedStringHasHebrew() {
        val missing = displayedStrings().filter { it !in HebrewStrings.map }.sorted()
        assertTrue(missing.isEmpty(), "No Hebrew for:\n" + missing.joinToString("\n"))
    }

    // Every translation map, Hebrew (full) plus the partial languages that fall back to English.
    private val allMaps = mapOf(
        "Hebrew" to HebrewStrings.map, "Russian" to RussianStrings.map, "German" to GermanStrings.map,
        "French" to FrenchStrings.map, "Spanish" to SpanishStrings.map, "Italian" to ItalianStrings.map,
    )

    @Test fun placeholdersMatch() {
        val placeholder = Regex("%(?:\\.\\d+)?[sdf%]|%0\\d+d")
        allMaps.forEach { (lang, map) ->
            map.forEach { (en, tr) ->
                assertEquals(
                    placeholder.findAll(en).map { it.value }.toList(), placeholder.findAll(tr).map { it.value }.toList(),
                    "$lang placeholders differ for “$en”",
                )
            }
        }
    }

    @Test fun labelsSwitchLanguage() {
        val battery = Trigger.BatteryLevel(LevelCondition.FALLS_BELOW, 20)
        assertEquals("When battery falls below 20%", battery.label())
        I18n.useForTest(true)
        assertEquals("כשהסוללה יורדת מתחת ל-20%", battery.label())
        assertEquals("15 דק׳ לפני השקיעה, כל יום",
            Trigger.TimeOfDay(0, 0, solar = SolarEvent.SUNSET, offsetMinutes = -15, lat = 32.0, lng = 34.8).label())
        assertEquals("כשאני מגיע אל בית", Trigger.Arrive(Place("בית", 32.0, 34.8)).label())
        assertEquals("שליחת מיקום", Action.SendLocation().label())
        // Hebrew week starts on Sunday, with Hebrew letters.
        assertEquals(listOf(7, 1, 2, 3, 4, 5, 6), weekOrder())
        assertEquals("א", dayLetter(7))
    }
}
