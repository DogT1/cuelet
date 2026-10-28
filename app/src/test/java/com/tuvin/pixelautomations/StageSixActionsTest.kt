package com.tuvin.pixelautomations

import com.tuvin.pixelautomations.actions.HomeAssistantApi
import com.tuvin.pixelautomations.actions.Http
import com.tuvin.pixelautomations.actions.ShizukuShell
import com.tuvin.pixelautomations.editor.isComplete
import com.tuvin.pixelautomations.model.Action
import com.tuvin.pixelautomations.model.AppRef
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.AutomationJson
import com.tuvin.pixelautomations.model.MediaCommand
import com.tuvin.pixelautomations.model.Radio
import com.tuvin.pixelautomations.model.RingerMode
import com.tuvin.pixelautomations.model.Toggle
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.model.VolumeStream
import com.tuvin.pixelautomations.model.label
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StageSixActionsTest {

    @Test fun homeAssistantRequests() {
        assertEquals("/api/services/light/turn_on", HomeAssistantApi.path("light.turn_on"))
        assertFailsWith<IllegalArgumentException> { HomeAssistantApi.path("turn_on") }
        assertEquals("""{"entity_id":"light.living_room"}""", HomeAssistantApi.body("light.living_room", ""))
        assertEquals(
            """{"entity_id":"light.living_room","brightness_pct":40}""",
            HomeAssistantApi.body("light.living_room", """{"brightness_pct": 40}"""),
        )
    }

    @Test fun headerParsing() {
        assertEquals(
            mapOf("Authorization" to "Bearer x:y", "X-Test" to "1"),
            Http.parseHeaders("Authorization: Bearer x:y\n\nbroken line\nX-Test:1"),
        )
    }

    @Test fun shizukuCommands() {
        assertEquals("cmd wifi set-wifi-enabled disabled", ShizukuShell.radioCommand(Radio.WIFI, on = false))
        assertEquals("cmd bluetooth_manager enable", ShizukuShell.radioCommand(Radio.BLUETOOTH, on = true))
        assertEquals("svc data disable", ShizukuShell.radioCommand(Radio.MOBILE_DATA, on = false))
        assertEquals("cmd connectivity airplane-mode enable", ShizukuShell.radioCommand(Radio.AIRPLANE, on = true))
        assertEquals("am force-stop com.whatsapp", ShizukuShell.forceStopCommand("com.whatsapp"))
        // A package name goes into a shell command: anything else is refused.
        assertFailsWith<IllegalArgumentException> { ShizukuShell.forceStopCommand("x; reboot") }
    }

    private val allActions = listOf(
        Action.ShowNotification("Hi", "there"), Action.OpenApp(AppRef("com.whatsapp", "WhatsApp")),
        Action.OpenUrl("https://example.com"), Action.Webhook("http://example.com/hook", "POST", "X-A: 1", "{}"),
        Action.HomeAssistant("light.turn_on", "light.desk", """{"brightness_pct":40}"""),
        Action.SetVolume(VolumeStream.ALARM, 80), Action.SetRinger(RingerMode.SILENT), Action.SetDnd(false),
        Action.Flashlight(Toggle.ON), Action.Brightness(30), Action.Speak("Good morning"), Action.CopyToClipboard("x"),
        Action.Wait(10), Action.Media(MediaCommand.NEXT), Action.Vibrate(), Action.RunAutomation(7, "Morning"),
        Action.SetRadio(Radio.MOBILE_DATA, Toggle.OFF), Action.ForceStop(AppRef("com.whatsapp", "WhatsApp")),
    )

    @Test fun everyActionRoundTripsAndHasALabel() {
        val a = Automation(id = 1, trigger = Trigger.Charger(), actions = allActions)
        assertEquals(a, AutomationJson.decode(AutomationJson.encode(a)))
        allActions.forEach { assertTrue(it.label().isNotBlank()) }
        assertEquals("Mobile data off", Action.SetRadio(Radio.MOBILE_DATA, Toggle.OFF).label())
    }

    @Test fun completeness() {
        assertTrue(allActions.all { it.isComplete() })
        assertFalse(Action.ShowNotification("").isComplete())
        assertFalse(Action.OpenApp(AppRef("", "")).isComplete())
        assertFalse(Action.OpenUrl("https://").isComplete())
        assertFalse(Action.RunAutomation(0, "").isComplete())
    }
}
