package com.tuvin.pixelautomations

import com.tuvin.pixelautomations.engine.RunRules
import com.tuvin.pixelautomations.model.Action
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.AutomationJson
import com.tuvin.pixelautomations.model.RunMode
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.model.label
import com.tuvin.pixelautomations.triggers.ChargerTrigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RulesAndModelTest {
    private val sample = Automation(
        id = 3,
        name = "Morning",
        trigger = Trigger.TimeOfDay(7, 30, setOf(1, 2, 3, 4, 5)),
        actions = listOf(Action.ShowNotification("Good morning", "Coffee time")),
        runMode = RunMode.IMMEDIATELY,
        notifyWhenRun = false,
    )

    @Test fun chargerMatchesOnlyItsDirection() {
        assertTrue(ChargerTrigger.matches(Trigger.Charger(connected = true), connected = true))
        assertFalse(ChargerTrigger.matches(Trigger.Charger(connected = true), connected = false))
        assertTrue(ChargerTrigger.matches(Trigger.Charger(connected = false), connected = false))
    }

    @Test fun runImmediatelyHonoursNotify() {
        assertEquals(RunRules.Decision.RunNow(notify = false), RunRules.decide(sample))
        assertEquals(RunRules.Decision.RunNow(notify = true), RunRules.decide(sample.copy(notifyWhenRun = true)))
    }

    @Test fun afterConfirmationAlwaysAsks() {
        val a = sample.copy(runMode = RunMode.AFTER_CONFIRMATION, notifyWhenRun = false)
        assertEquals(RunRules.Decision.AskFirst, RunRules.decide(a))
    }

    @Test fun jsonRoundTrip() {
        val charger = sample.copy(id = 4, trigger = Trigger.Charger(false))
        assertEquals(sample, AutomationJson.decode(AutomationJson.encode(sample)))
        assertEquals(listOf(sample, charger), AutomationJson.decodeAll(AutomationJson.encodeAll(listOf(sample, charger))))
    }

    @Test fun jsonIgnoresUnknownFieldsFromNewerVersions() {
        val json = AutomationJson.encode(sample).replaceFirst("{", "{\"futureField\":1,")
        assertEquals(sample, AutomationJson.decode(json))
    }

    @Test fun labelsReadLikeIos() {
        assertEquals("At 07:30, weekdays", sample.trigger.label())
        assertEquals("When charger is connected", Trigger.Charger(true).label())
        assertEquals("Morning", sample.title)
        assertEquals("At 07:30, weekdays", sample.copy(name = "").title)
    }
}
