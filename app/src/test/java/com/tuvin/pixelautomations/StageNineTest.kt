package com.tuvin.pixelautomations

import com.tuvin.pixelautomations.editor.isComplete
import com.tuvin.pixelautomations.model.Action
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.AutomationJson
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.model.label
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StageNineTest {
    private val actions = listOf(
        Action.SendLocation(note = "lost"), Action.CapturePhoto(front = true, back = true),
        Action.RecordAudio(15), Action.LockScreen, Action.TakeScreenshot,
        Action.SetAlarm(7, 30, "wake"), Action.StartTimer(300, "tea"),
    )

    @Test fun roundTripIncludingObjects() {
        val a = Automation(id = 1, trigger = Trigger.Message(senders = listOf("Dad"), contains = "FIND MY PHONE"), actions = actions)
        assertEquals(a, AutomationJson.decode(AutomationJson.encode(a)))
    }

    @Test fun labels() {
        assertEquals("Send location", Action.SendLocation().label())
        assertEquals("Capture front & back photo", Action.CapturePhoto(true, true).label())
        assertEquals("Capture back photo", Action.CapturePhoto(false, true).label())
        assertEquals("Lock the screen", Action.LockScreen.label())
        assertEquals("Set alarm for 07:30", Action.SetAlarm(7, 30).label())
    }

    @Test fun capturePhotoNeedsACamera() {
        assertTrue(Action.CapturePhoto(front = true, back = false).isComplete())
        assertFalse(Action.CapturePhoto(front = false, back = false).isComplete())
    }

    @Test fun antiTheftAutomationShape() {
        // The example: text from a trusted contact → send location + photos, no confirmation.
        val a = Automation(
            name = "Lost phone",
            trigger = Trigger.Message(senders = listOf("+972...", "Dad"), contains = "FIND MY PHONE"),
            actions = listOf(Action.SendLocation(), Action.CapturePhoto(true, true)),
            runMode = com.tuvin.pixelautomations.model.RunMode.IMMEDIATELY,
            notifyWhenRun = false,
        )
        assertEquals(a, AutomationJson.decode(AutomationJson.encode(a)))
        assertTrue(a.actions.all { it.isComplete() })
    }
}
