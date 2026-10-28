package com.tuvin.pixelautomations.triggers

import com.tuvin.pixelautomations.model.Trigger

/** iOS "Bluetooth": a chosen device (or any device) Is Connected / Is Disconnected. */
object BluetoothTrigger {
    fun matches(t: Trigger.Bluetooth, address: String, connected: Boolean): Boolean =
        t.connected == connected && (t.devices.isEmpty() || t.devices.any { it.address.equals(address, ignoreCase = true) })
}
