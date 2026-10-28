package com.tuvin.pixelautomations

import com.tuvin.pixelautomations.model.AppRef
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.AutomationJson
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.model.label
import com.tuvin.pixelautomations.triggers.AppTracker
import com.tuvin.pixelautomations.triggers.AppTracker.Event.Closed
import com.tuvin.pixelautomations.triggers.AppTracker.Event.Opened
import com.tuvin.pixelautomations.triggers.AppTrigger
import com.tuvin.pixelautomations.triggers.NfcTrigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StageFiveTriggersTest {
    private val apps = setOf("com.netflix.mediaclient", "com.whatsapp", "com.google.android.apps.nexuslauncher")
    private val isApp: (String) -> Boolean = { it in apps }

    @Test fun switchingAppsClosesTheOldAndOpensTheNew() {
        val t = AppTracker()
        assertEquals(listOf(Opened("com.whatsapp")), t.onWindow("com.whatsapp", isApp))
        assertEquals(emptyList(), t.onWindow("com.whatsapp", isApp)) // a dialog inside the same app
        assertEquals(
            listOf(Closed("com.whatsapp"), Opened("com.netflix.mediaclient")),
            t.onWindow("com.netflix.mediaclient", isApp),
        )
    }

    @Test fun overlaysAndKeyboardsAreNotAppSwitches() {
        val t = AppTracker()
        t.onWindow("com.whatsapp", isApp)
        assertEquals(emptyList(), t.onWindow("com.android.systemui", isApp)) // notification shade
        assertEquals(emptyList(), t.onWindow("com.google.android.inputmethod.latin", isApp))
        assertEquals("com.whatsapp", t.foreground)
        // Going home closes the app (the launcher counts as an app).
        assertEquals(
            listOf(Closed("com.whatsapp"), Opened("com.google.android.apps.nexuslauncher")),
            t.onWindow("com.google.android.apps.nexuslauncher", isApp),
        )
    }

    private val netflix = AppRef("com.netflix.mediaclient", "Netflix")

    @Test fun appTriggerMatchesKindAndApp() {
        val openOnly = Trigger.App(listOf(netflix), opened = true, closed = false)
        val both = Trigger.App(listOf(netflix), opened = true, closed = true)
        assertTrue(AppTrigger.matches(openOnly, Opened(netflix.pkg)))
        assertFalse(AppTrigger.matches(openOnly, Closed(netflix.pkg)))
        assertTrue(AppTrigger.matches(both, Closed(netflix.pkg)))
        assertFalse(AppTrigger.matches(both, Opened("com.whatsapp")))
    }

    @Test fun nfcIds() {
        assertEquals("04A2B3FF", NfcTrigger.idOf(byteArrayOf(0x04, 0xA2.toByte(), 0xB3.toByte(), 0xFF.toByte())))
        assertTrue(NfcTrigger.matches(Trigger.Nfc("04A2B3FF", "Desk"), "04a2b3ff"))
        assertFalse(NfcTrigger.matches(Trigger.Nfc("04A2B3FF", "Desk"), "04A2B300"))
        assertEquals("pixelautomations://tag/04A2B3FF", NfcTrigger.uriFor("04A2B3FF"))
    }

    @Test fun jsonAndLabels() {
        val app = Trigger.App(listOf(netflix), opened = true, closed = true)
        val nfc = Trigger.Nfc("04A2B3FF", "Bedside")
        listOf(app, nfc).forEach { t ->
            val a = Automation(id = 1, trigger = t)
            assertEquals(a, AutomationJson.decode(AutomationJson.encode(a)))
        }
        assertEquals("When Netflix is opened or closed", app.label())
        assertEquals("When I tap Bedside", nfc.label())
    }
}
