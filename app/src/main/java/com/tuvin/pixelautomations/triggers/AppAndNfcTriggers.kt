package com.tuvin.pixelautomations.triggers

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.tuvin.pixelautomations.engine.NfcTagActivity
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.Trigger

/**
 * Follows which app is in front, from window-change events. Only launchable apps (and the home screen) count,
 * so the notification shade, the keyboard and system dialogs don't look like app switches.
 */
class AppTracker {
    sealed interface Event {
        val pkg: String
        data class Opened(override val pkg: String) : Event
        data class Closed(override val pkg: String) : Event
    }

    var foreground: String? = null
        private set

    /** Call for every window change; returns Closed(previous) and Opened(new) when the front app changed. */
    fun onWindow(pkg: String, isApp: (String) -> Boolean): List<Event> {
        if (!isApp(pkg) || pkg == foreground) return emptyList()
        val previous = foreground
        foreground = pkg
        return listOfNotNull(previous?.let { Event.Closed(it) }, Event.Opened(pkg))
    }
}

object AppTrigger {
    fun matches(t: Trigger.App, e: AppTracker.Event): Boolean {
        val rightKind = if (e is AppTracker.Event.Opened) t.opened else t.closed
        return rightKind && t.apps.any { it.pkg == e.pkg }
    }
}

object NfcTrigger {
    /** Tag IDs are stored as upper-case hex, e.g. "04A2B3C4D5E680". */
    fun idOf(bytes: ByteArray): String = bytes.joinToString("") { "%02X".format(it) }

    fun matches(t: Trigger.Nfc, tagId: String): Boolean = t.tagId.equals(tagId, ignoreCase = true)

    /** The URI written to tags with "Write tag", so Android routes them to this app. */
    const val SCHEME = "pixelautomations"
    fun uriFor(tagId: String) = "$SCHEME://tag/$tagId"

    /** True while at least one enabled automation waits for a tag. */
    fun wanted(all: List<Automation>): Boolean = all.any { it.enabled && it.trigger is Trigger.Nfc }

    /**
     * Switches [NfcTagActivity] on or off. Its tag filter claims every NFC card the phone sees (a transit or bank
     * card in the phone case included), so it stays off until an enabled NFC automation needs it.
     */
    fun sync(ctx: Context, all: List<Automation>) {
        val pm = ctx.packageManager
        val listener = ComponentName(ctx, NfcTagActivity::class.java)
        val state = if (wanted(all)) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        if (pm.getComponentEnabledSetting(listener) != state) {
            pm.setComponentEnabledSetting(listener, state, PackageManager.DONT_KILL_APP)
        }
    }
}
