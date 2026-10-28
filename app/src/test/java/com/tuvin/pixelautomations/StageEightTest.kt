package com.tuvin.pixelautomations

import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.AutomationJson
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.model.label
import com.tuvin.pixelautomations.triggers.WorkoutTrigger
import com.tuvin.pixelautomations.triggers.WorkoutTrigger.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StageEightTest {
    @Test fun messages() {
        assertEquals(Event(true, "RUNNING"), WorkoutTrigger.decode("start|RUNNING".toByteArray()))
        assertEquals(Event(false, "BIKING"), WorkoutTrigger.decode("end|BIKING".toByteArray()))
        assertEquals(Event(true, "WORKOUT"), WorkoutTrigger.decode("start|weird type!".toByteArray()))
        assertNull(WorkoutTrigger.decode("hello".toByteArray()))
        assertEquals("end|YOGA", WorkoutTrigger.encode(Event(false, "YOGA")).decodeToString())
    }

    @Test fun matching() {
        val anyStart = Trigger.Workout(starts = true)
        val runEnd = Trigger.Workout(listOf("RUNNING"), starts = false)
        assertTrue(WorkoutTrigger.matches(anyStart, Event(true, "YOGA")))
        assertFalse(WorkoutTrigger.matches(anyStart, Event(false, "YOGA")))
        assertTrue(WorkoutTrigger.matches(runEnd, Event(false, "RUNNING")))
        assertFalse(WorkoutTrigger.matches(runEnd, Event(false, "WALKING")))
    }

    @Test fun jsonAndLabel() {
        val a = Automation(id = 1, trigger = Trigger.Workout(listOf("RUNNING", "BIKING"), starts = false))
        assertEquals(a, AutomationJson.decode(AutomationJson.encode(a)))
        assertEquals("When Running, Cycling ends on the watch", a.trigger.label())
        assertEquals("When a workout starts on the watch", Trigger.Workout().label())
    }
}
