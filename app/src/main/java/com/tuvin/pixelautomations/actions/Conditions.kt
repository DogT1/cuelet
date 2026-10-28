package com.tuvin.pixelautomations.actions

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import com.tuvin.pixelautomations.model.Condition
import com.tuvin.pixelautomations.triggers.WifiTrigger
import java.time.LocalDateTime

/** The phone's state at the moment an If action runs. */
data class PhoneState(
    val batteryPercent: Int,
    val charging: Boolean,
    val ssid: String?,
    val minuteOfDay: Int,
    val dayOfWeek: Int,
    val dndOn: Boolean,
)

object Conditions {
    fun holds(c: Condition, s: PhoneState): Boolean = when (c) {
        is Condition.BatteryAbove -> s.batteryPercent > c.percent
        is Condition.BatteryBelow -> s.batteryPercent < c.percent
        is Condition.Charging -> s.charging == c.yes
        is Condition.WifiIs -> s.ssid != null && s.ssid.equals(c.ssid.trim(), ignoreCase = false)
        is Condition.TimeBetween -> c.window.contains(s.minuteOfDay)
        is Condition.DndOn -> s.dndOn == c.yes
        is Condition.Weekday -> s.dayOfWeek in c.days
    }

    @Suppress("DEPRECATION")
    fun read(ctx: Context): PhoneState {
        val battery = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val plugged = (battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        val ssid = runCatching { WifiTrigger.normalize(ctx.getSystemService(WifiManager::class.java).connectionInfo.ssid) }.getOrNull()
        val now = LocalDateTime.now()
        val filter = ctx.getSystemService(NotificationManager::class.java).currentInterruptionFilter
        return PhoneState(
            batteryPercent = if (level >= 0 && scale > 0) level * 100 / scale else 0,
            charging = plugged,
            ssid = ssid,
            minuteOfDay = now.hour * 60 + now.minute,
            dayOfWeek = now.dayOfWeek.value,
            dndOn = filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN,
        )
    }
}
