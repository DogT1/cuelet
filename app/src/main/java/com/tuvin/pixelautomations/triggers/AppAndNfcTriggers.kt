package com.tuvin.pixelautomations.triggers

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
}
