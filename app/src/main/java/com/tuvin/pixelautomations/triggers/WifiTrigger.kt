package com.tuvin.pixelautomations.triggers

import com.tuvin.pixelautomations.model.Trigger

/**
 * iOS "Wi-Fi": the phone Joins / Leaves a chosen network (or any network).
 * SSIDs are only readable with location access; without it Android reports "<unknown ssid>",
 * which still matches "any network" but never a named one.
 */
object WifiTrigger {
    sealed interface Event {
        val ssid: String?
        data class Joined(override val ssid: String?) : Event
        data class Left(override val ssid: String?) : Event
    }

    /** Android wraps SSIDs in quotes and uses a placeholder when it hides them. */
    fun normalize(raw: String?): String? {
        if (raw == null) return null
        val s = raw.removeSurrounding("\"")
        return if (s.isBlank() || s == "<unknown ssid>") null else s
    }

    /** The events to fire when the connected network goes from [before] to [after] (null = no Wi-Fi). */
    fun transition(before: String?, after: String?, wasConnected: Boolean, isConnected: Boolean): List<Event> = buildList {
        if (wasConnected && (!isConnected || before != after)) add(Event.Left(before))
        if (isConnected && (!wasConnected || before != after)) add(Event.Joined(after))
    }

    fun matches(t: Trigger.WiFi, e: Event): Boolean {
        val rightKind = (e is Event.Joined) == t.joins
        val rightNetwork = t.networks.isEmpty() || (e.ssid != null && t.networks.any { it == e.ssid })
        return rightKind && rightNetwork
    }
}
