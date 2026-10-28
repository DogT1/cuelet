package com.tuvin.pixelautomations.triggers

import com.tuvin.pixelautomations.model.Trigger

/** iOS "Low Power Mode" (Android Battery Saver) and "Airplane Mode": fire when the switch turns the chosen way. */
object LowPowerModeTrigger {
    fun matches(t: Trigger.LowPowerMode, on: Boolean): Boolean = t.on == on
}

object AirplaneModeTrigger {
    fun matches(t: Trigger.AirplaneMode, on: Boolean): Boolean = t.on == on
}

/** iOS "CarPlay", mapped to Android Auto. */
object AndroidAutoTrigger {
    fun matches(t: Trigger.AndroidAuto, connected: Boolean): Boolean = t.connects == connected
}

/**
 * Remembers the last state of a switch so a repeated broadcast with the same state
 * (Android sends some on every screen-on or reconnect) does not fire twice.
 */
class StateTracker<T> {
    private var last: T? = null
    private var known = false

    /** Returns true when [value] differs from the last one seen; the first value only primes the tracker. */
    fun changed(value: T): Boolean {
        val isChange = known && value != last
        last = value
        known = true
        return isChange
    }

    /** Like [changed], but the first value counts as a change (for events that are changes by definition). */
    fun changedOrFirst(value: T): Boolean {
        val isChange = !known || value != last
        last = value
        known = true
        return isChange
    }
}
