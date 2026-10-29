package com.tuvin.pixelautomations.setup

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.tuvin.pixelautomations.engine.AppWatchService
import com.tuvin.pixelautomations.engine.NotifListener
import com.tuvin.pixelautomations.actions.ShizukuShell
import com.tuvin.pixelautomations.model.Action
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.Condition
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.model.VolumeStream
import com.tuvin.pixelautomations.model.flatten

/** Which permissions each trigger needs. Asked for only when an automation uses that trigger. */
object Permissions {
    const val NEARBY = Manifest.permission.BLUETOOTH_CONNECT
    const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    const val BACKGROUND_LOCATION = Manifest.permission.ACCESS_BACKGROUND_LOCATION
    const val CAMERA = Manifest.permission.CAMERA
    const val RECORD_AUDIO = Manifest.permission.RECORD_AUDIO
    const val SEND_SMS = Manifest.permission.SEND_SMS
    const val POST_NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS
    /** Not a runtime permission: switched on in Settings › Notification read, reply & control. */
    const val NOTIFICATION_ACCESS = "special:notification_access"
    /** Not a runtime permission: switched on in Settings › Accessibility › Automations: App trigger. */
    const val ACCESSIBILITY = "special:accessibility"
    /** Do Not Disturb access: change ringer mode, Do Not Disturb, ring/notification volume. */
    const val DND_ACCESS = "special:dnd_access"
    /** "Modify system settings": screen brightness. */
    const val WRITE_SETTINGS = "special:write_settings"
    /** Shizuku running and this app allowed in it: Wi-Fi/Bluetooth/data/airplane switches, force-stop. */
    const val SHIZUKU = "special:shizuku"
    /** Details entered in the app's Connections screen. */
    const val HOME_ASSISTANT = "special:home_assistant"
    /** Battery optimisation exemption, so triggers fire on time in the background. */
    const val BATTERY = "special:battery"

    /** Every permission the app can use, for the Settings checklist (ordered most-to-least foundational). */
    fun all(): List<String> = listOf(
        NOTIFICATION_ACCESS, ACCESSIBILITY, POST_NOTIFICATIONS,
        FINE_LOCATION, BACKGROUND_LOCATION, NEARBY,
        CAMERA, RECORD_AUDIO, SEND_SMS,
        DND_ACCESS, WRITE_SETTINGS,
        SHIZUKU, HOME_ASSISTANT, BATTERY,
    )

    fun needed(a: Action): List<String> = when (a) {
        is Action.SetRinger, is Action.SetDnd -> listOf(DND_ACCESS)
        is Action.SetVolume -> if (a.stream == VolumeStream.RING || a.stream == VolumeStream.NOTIFICATION) listOf(DND_ACCESS) else emptyList()
        is Action.Brightness -> listOf(WRITE_SETTINGS)
        // SetRadio / ForceStop work without Shizuku too (they open the switch for a tap), so not listed as needed.
        is Action.HomeAssistant -> listOf(HOME_ASSISTANT)
        // Anti-theft actions run from the background/lock screen, so location must be "all the time".
        is Action.SendLocation -> listOf(FINE_LOCATION, BACKGROUND_LOCATION)
        is Action.CapturePhoto -> listOf(CAMERA)
        is Action.RecordAudio -> listOf(RECORD_AUDIO)
        is Action.LockScreen, is Action.TakeScreenshot -> listOf(ACCESSIBILITY)
        is Action.SendSms -> listOf(SEND_SMS)
        is Action.If -> needed(a.condition)
        else -> emptyList()
    }

    fun needed(c: Condition): List<String> = when (c) {
        // The check runs from the background service.
        is Condition.AtPlace -> listOf(FINE_LOCATION, BACKGROUND_LOCATION)
        is Condition.BluetoothConnected -> listOf(NEARBY)
        is Condition.AppInFront -> listOf(ACCESSIBILITY)
        else -> emptyList()
    }

    /** Everything an automation still needs: its trigger's and its actions' permissions. */
    fun missing(ctx: Context, a: Automation): List<String> =
        (needed(a.trigger) + flatten(a.actions).flatMap(::needed)).distinct().filterNot { granted(ctx, it) }

    /** Special items that are set up inside this app rather than in Android's Settings. */
    fun isInApp(permission: String) = permission == HOME_ASSISTANT

    fun needed(t: Trigger): List<String> = when (t) {
        is Trigger.Bluetooth -> listOf(NEARBY)
        // Reading a network's name needs location; "any network" works without it.
        is Trigger.WiFi -> if (t.networks.isEmpty()) emptyList() else listOf(FINE_LOCATION, BACKGROUND_LOCATION)
        // Geofences and the commute check run in the background.
        is Trigger.Arrive, is Trigger.Leave, is Trigger.BeforeCommute -> listOf(FINE_LOCATION, BACKGROUND_LOCATION)
        // Sunrise/sunset store the place when the automation is made; location is only needed then.
        is Trigger.TimeOfDay -> if (t.solar != null && t.lat == null) listOf(FINE_LOCATION) else emptyList()
        // Sleep works from Do Not Disturb/Mode changes, which need no special access.
        is Trigger.Email, is Trigger.Message, is Trigger.Alarm,
        is Trigger.Transaction, is Trigger.SoundRecognition -> listOf(NOTIFICATION_ACCESS)
        is Trigger.App -> listOf(ACCESSIBILITY)
        else -> emptyList()
    }

    fun granted(ctx: Context, permission: String): Boolean = when (permission) {
        NOTIFICATION_ACCESS -> ctx.getSystemService(NotificationManager::class.java)
            .isNotificationListenerAccessGranted(ComponentName(ctx, NotifListener::class.java))
        ACCESSIBILITY -> {
            val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
            val me = ComponentName(ctx, AppWatchService::class.java)
            enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
        DND_ACCESS -> ctx.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
        WRITE_SETTINGS -> Settings.System.canWrite(ctx)
        SHIZUKU -> ShizukuShell.permitted()
        HOME_ASSISTANT -> Connections(ctx).homeAssistantReady
        BATTERY -> ctx.getSystemService(android.os.PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
        else -> ctx.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    fun missing(ctx: Context, t: Trigger): List<String> = needed(t).filterNot { granted(ctx, it) }

    fun isSpecial(permission: String) = permission.startsWith("special:")

    /** The Settings page that turns on a special permission. */
    fun settingsIntent(ctx: Context, permission: String): Intent? = when (permission) {
        NOTIFICATION_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, ComponentName(ctx, NotifListener::class.java).flattenToString())
        // The per-service details page needs a system-only permission, so open the Accessibility list.
        ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        DND_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        WRITE_SETTINGS -> Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, android.net.Uri.parse("package:${ctx.packageName}"))
        else -> null
    }

    fun describe(permission: String): String = t(describeEnglish(permission))

    private fun describeEnglish(permission: String): String = when (permission) {
        CAMERA -> "Camera"
        RECORD_AUDIO -> "Microphone"
        SEND_SMS -> "Send SMS"
        POST_NOTIFICATIONS -> "Show notifications"
        BATTERY -> "Unrestricted battery"
        NEARBY -> "Nearby devices"
        FINE_LOCATION -> "Location"
        BACKGROUND_LOCATION -> "Location: Allow all the time"
        NOTIFICATION_ACCESS -> "Notification access"
        ACCESSIBILITY -> "Accessibility (App trigger)"
        DND_ACCESS -> "Do Not Disturb access"
        WRITE_SETTINGS -> "Modify system settings"
        SHIZUKU -> if (ShizukuShell.available()) "Shizuku permission" else "Shizuku running"
        HOME_ASSISTANT -> "Home Assistant details (Connections)"
        else -> permission.substringAfterLast('.')
    }
}
