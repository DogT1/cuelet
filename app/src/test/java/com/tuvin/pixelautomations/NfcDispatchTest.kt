package com.tuvin.pixelautomations

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.tuvin.pixelautomations.engine.NfcTagActivity
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.triggers.NfcTrigger
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/** NfcTagActivity takes part in Android's NFC dispatch only while an enabled NFC automation exists. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class NfcDispatchTest {
    @Test fun listenerFollowsTheSavedAutomations() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val listener = ComponentName(ctx, NfcTagActivity::class.java)
        fun state() = ctx.packageManager.getComponentEnabledSetting(listener)
        val nfc = Automation(id = 1, trigger = Trigger.Nfc("04A2B3FF", "Desk"))

        NfcTrigger.sync(ctx, emptyList())
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, state())

        NfcTrigger.sync(ctx, listOf(nfc))
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, state())

        NfcTrigger.sync(ctx, listOf(nfc.copy(enabled = false)))
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, state())
    }
}
