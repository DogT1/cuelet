package com.tuvin.pixelautomations.triggers

import com.tuvin.pixelautomations.model.Trigger

/**
 * iOS "Apple Watch Workout" via the Galaxy Watch companion. The watch sends a Data Layer message on
 * [PATH] with "start|RUNNING" or "end|RUNNING" (Health Services exercise type name).
 */
object WorkoutTrigger {
    const val PATH = "/automations/workout"

    data class Event(val started: Boolean, val type: String)

    fun encode(e: Event): ByteArray = ((if (e.started) "start" else "end") + "|" + e.type).toByteArray()

    fun decode(bytes: ByteArray): Event? {
        val parts = bytes.decodeToString().split('|', limit = 2)
        val started = when (parts.getOrNull(0)) {
            "start" -> true
            "end" -> false
            else -> return null
        }
        val type = parts.getOrNull(1)?.takeIf { Regex("[A-Z_]{1,40}").matches(it) } ?: "WORKOUT"
        return Event(started, type)
    }

    fun matches(t: Trigger.Workout, e: Event): Boolean =
        t.starts == e.started && (t.types.isEmpty() || e.type in t.types)
}
