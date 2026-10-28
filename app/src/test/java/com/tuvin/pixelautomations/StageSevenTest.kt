package com.tuvin.pixelautomations

import com.tuvin.pixelautomations.actions.Conditions
import com.tuvin.pixelautomations.actions.PhoneState
import com.tuvin.pixelautomations.editor.ImportRemap
import com.tuvin.pixelautomations.editor.isComplete
import com.tuvin.pixelautomations.model.Action
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.AutomationJson
import com.tuvin.pixelautomations.model.Condition
import com.tuvin.pixelautomations.model.MenuOption
import com.tuvin.pixelautomations.model.TimeWindow
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.model.flatten
import com.tuvin.pixelautomations.model.label
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StageSevenTest {
    // Tuesday 23:30, 41 %, charging, on "kolbasa", DND off.
    private val s = PhoneState(batteryPercent = 41, charging = true, ssid = "kolbasa", minuteOfDay = 23 * 60 + 30, dayOfWeek = 2, dndOn = false)

    @Test fun conditions() {
        assertTrue(Conditions.holds(Condition.BatteryAbove(40), s))
        assertFalse(Conditions.holds(Condition.BatteryAbove(41), s))
        assertTrue(Conditions.holds(Condition.BatteryBelow(50), s))
        assertTrue(Conditions.holds(Condition.Charging(true), s))
        assertFalse(Conditions.holds(Condition.Charging(false), s))
        assertTrue(Conditions.holds(Condition.WifiIs("kolbasa"), s))
        assertFalse(Conditions.holds(Condition.WifiIs("Kolbasa"), s)) // network names are case-sensitive
        assertFalse(Conditions.holds(Condition.WifiIs("kolbasa"), s.copy(ssid = null)))
        assertTrue(Conditions.holds(Condition.TimeBetween(TimeWindow(22 * 60, 6 * 60)), s))
        assertFalse(Conditions.holds(Condition.TimeBetween(TimeWindow(8 * 60, 20 * 60)), s))
        assertTrue(Conditions.holds(Condition.DndOn(false), s))
        assertTrue(Conditions.holds(Condition.Weekday(setOf(7, 1, 2, 3, 4)), s)) // Israeli work week
        assertFalse(Conditions.holds(Condition.Weekday(setOf(5, 6)), s))
    }

    private val note = Action.ShowNotification("Hi")
    private val nested = listOf(
        Action.If(Condition.Charging(), then = listOf(note, Action.RunAutomation(3, "Other")), otherwise = listOf(Action.Wait(1))),
        Action.ChooseFromMenu("Heading out?", listOf(MenuOption("Yes", listOf(Action.SetDnd(true))), MenuOption("No"))),
    )

    @Test fun flattenReachesNestedActions() {
        val all = flatten(nested)
        assertEquals(6, all.size)
        assertTrue(all.any { it is Action.SetDnd })
        assertTrue(all.any { it is Action.RunAutomation })
    }

    @Test fun ifAndMenuRoundTrip() {
        val a = Automation(id = 9, trigger = Trigger.Charger(), actions = nested)
        assertEquals(a, AutomationJson.decode(AutomationJson.encode(a)))
        assertEquals("If phone is charging", nested[0].label())
        assertEquals("Choose: Yes / No", nested[1].label())
    }

    @Test fun completeness() {
        assertTrue(nested.all { it.isComplete() })
        assertFalse(Action.If(then = listOf(Action.ShowNotification(""))).isComplete())
        assertFalse(Action.If(Condition.WifiIs("")).isComplete())
        assertFalse(Action.ChooseFromMenu("x", emptyList()).isComplete())
        assertFalse(Action.ChooseFromMenu("x", List(4) { MenuOption("o$it") }).isComplete()) // Android shows max 3 buttons
        assertFalse(Action.ChooseFromMenu("x", listOf(MenuOption(""))).isComplete())
    }

    @Test fun importRemapsRunAutomationEverywhere() {
        val remapped = ImportRemap.remap(nested + Action.RunAutomation(3, "Other") + Action.RunAutomation(99, "Gone"), mapOf(3L to 103L))
        val runs = flatten(remapped).filterIsInstance<Action.RunAutomation>()
        assertEquals(listOf(103L, 103L, 99L), runs.map { it.automationId }) // unknown IDs left as they are
    }
}
