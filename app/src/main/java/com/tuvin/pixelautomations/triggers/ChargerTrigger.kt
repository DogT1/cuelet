package com.tuvin.pixelautomations.triggers

import com.tuvin.pixelautomations.model.Trigger

/**
 * iOS "Charger": Is Connected / Is Disconnected. Android sends one power broadcast per plug change,
 * which AutomationService receives, so each change fires once.
 */
object ChargerTrigger {
    fun matches(t: Trigger.Charger, connected: Boolean): Boolean = t.connected == connected
}
