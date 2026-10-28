package com.tuvin.pixelautomations.actions

import com.tuvin.pixelautomations.setup.t

import android.app.Notification
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.KeyEvent
import com.tuvin.pixelautomations.triggers.Locations
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
import com.tuvin.pixelautomations.R
import com.tuvin.pixelautomations.app
import com.tuvin.pixelautomations.engine.Notifications
import com.tuvin.pixelautomations.model.Action
import com.tuvin.pixelautomations.model.MediaCommand
import com.tuvin.pixelautomations.model.Radio
import com.tuvin.pixelautomations.model.RingerMode
import com.tuvin.pixelautomations.model.SendMode
import com.tuvin.pixelautomations.model.SendVia
import com.tuvin.pixelautomations.model.Toggle
import com.tuvin.pixelautomations.model.VolumeStream
import com.tuvin.pixelautomations.model.label
import com.tuvin.pixelautomations.setup.Connections
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** Runs one action. Throws with a readable message when it can't; the engine logs it and stops the automation. */
object ActionRunner {
    /** "Run automation" may nest, but not forever (a loop of automations running each other). */
    const val MAX_DEPTH = 5

    /** [sender] is the phone number that triggered an SMS, for a "Send SMS" action that replies to it. */
    suspend fun run(ctx: Context, action: Action, depth: Int = 0, sender: String? = null): Unit = when (action) {
        is Action.ShowNotification -> ShowNotificationAction.run(ctx, action)
        is Action.OpenApp -> {
            val intent = ctx.packageManager.getLaunchIntentForPackage(action.app.pkg)
                ?: error(t("%s isn't installed", action.app.name))
            startFromBackground(ctx, intent, action.app.name)
        }
        is Action.OpenUrl -> startFromBackground(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(action.url)), action.url)
        is Action.Webhook -> {
            val r = Http.request(action.url, action.method, Http.parseHeaders(action.headers), action.body.ifBlank { null })
            check(r.ok) { "${action.method} ${action.url} → HTTP ${r.code}" }
        }
        is Action.HomeAssistant -> {
            val c = Connections(ctx)
            check(c.homeAssistantReady) { t("Add the Home Assistant address and token in Connections") }
            val r = Http.request(
                c.homeAssistantUrl + HomeAssistantApi.path(action.service), "POST",
                mapOf("Authorization" to "Bearer ${c.homeAssistantToken}"),
                HomeAssistantApi.body(action.entityId, action.extraJson),
            )
            check(r.ok) { "Home Assistant → HTTP ${r.code}" }
        }
        is Action.SetVolume -> {
            val am = ctx.getSystemService(AudioManager::class.java)
            val stream = when (action.stream) {
                VolumeStream.MEDIA -> AudioManager.STREAM_MUSIC
                VolumeStream.RING -> AudioManager.STREAM_RING
                VolumeStream.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
                VolumeStream.ALARM -> AudioManager.STREAM_ALARM
            }
            val max = am.getStreamMaxVolume(stream)
            am.setStreamVolume(stream, (action.percent.coerceIn(0, 100) / 100.0 * max).roundToInt(), 0)
        }
        is Action.SetRinger -> {
            ctx.getSystemService(AudioManager::class.java).ringerMode = when (action.mode) {
                RingerMode.NORMAL -> AudioManager.RINGER_MODE_NORMAL
                RingerMode.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
                RingerMode.SILENT -> AudioManager.RINGER_MODE_SILENT
            }
        }
        is Action.SetDnd -> ctx.getSystemService(NotificationManager::class.java).setInterruptionFilter(
            if (action.on) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL,
        )
        is Action.Flashlight -> Torch.set(ctx, action.mode)
        is Action.Brightness -> {
            check(Settings.System.canWrite(ctx)) { t("Allow “Modify system settings” for Automations") }
            val cr = ctx.contentResolver
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, (action.percent.coerceIn(1, 100) * 255 / 100))
            Unit
        }
        is Action.Speak -> Speech.say(ctx, action.text)
        is Action.CopyToClipboard -> ctx.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("Cuelet", action.text))
        is Action.Wait -> delay(action.seconds.coerceIn(0, 3600) * 1000L)
        is Action.Media -> {
            val am = ctx.getSystemService(AudioManager::class.java)
            val key = when (action.command) {
                MediaCommand.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                MediaCommand.PLAY -> KeyEvent.KEYCODE_MEDIA_PLAY
                MediaCommand.PAUSE -> KeyEvent.KEYCODE_MEDIA_PAUSE
                MediaCommand.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
                MediaCommand.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            }
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
        }
        is Action.Vibrate -> ctx.getSystemService(VibratorManager::class.java).defaultVibrator
            .vibrate(VibrationEffect.createOneShot(action.millis.coerceIn(10, 5_000).toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
        is Action.RunAutomation -> {
            check(depth < MAX_DEPTH) { t("Stopped: automations run each other more than %d deep", MAX_DEPTH) }
            val other = ctx.app.repo.get(action.automationId) ?: error(t("“%s” no longer exists", action.name))
            other.actions.forEach { run(ctx, it, depth + 1, sender) }
        }
        is Action.SetRadio -> {
            val on = when (action.mode) {
                Toggle.ON -> true
                Toggle.OFF -> false
                Toggle.TOGGLE -> !radioIsOn(ctx, action.radio)
            }
            if (ShizukuShell.permitted()) {
                ShizukuShell.run(ShizukuShell.radioCommand(action.radio, on))
            } else {
                // Without Shizuku, Android only lets apps open the switch for the user (one tap).
                startFromBackground(ctx, Intent(radioPanel(action.radio)), action.radio.label)
            }
            Unit
        }
        is Action.SendLocation -> {
            val loc = Locations.current(ctx) ?: error(t("Couldn't get a location fix"))
            val maps = "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
            val text = if (action.note.isBlank()) maps else "${action.note}\n$maps"
            val number = action.to.ifBlank { sender }
            when (action.via) {
                SendVia.SMS -> Sms.send(ctx, number ?: error(t("No one to send to; pick a contact")), text)
                SendVia.WHATSAPP -> {
                    val n = number ?: error(t("No one to send to; pick a contact"))
                    if (action.mode == SendMode.AUTO) AutoSend.arm(Messaging.WHATSAPP)
                    startFromBackground(ctx, Messaging.whatsappIntent(n, text), "WhatsApp")
                }
                SendVia.TELEGRAM -> {
                    if (action.mode == SendMode.AUTO) AutoSend.arm(Messaging.TELEGRAM)
                    startFromBackground(ctx, Messaging.telegramShareIntent(text), "Telegram")
                }
            }
        }
        is Action.CapturePhoto -> {
            check(action.front || action.back) { "pick at least one camera" }
            CaptureService.start(ctx, CaptureService.TASK_PHOTO) {
                putExtra(CaptureService.EXTRA_FRONT, action.front)
                putExtra(CaptureService.EXTRA_BACK, action.back)
            }
        }
        is Action.RecordAudio -> CaptureService.start(ctx, CaptureService.TASK_AUDIO) {
            putExtra(CaptureService.EXTRA_SECONDS, action.seconds)
        }
        is Action.LockScreen -> check(com.tuvin.pixelautomations.engine.AppWatchService.globalAction(GLOBAL_ACTION_LOCK_SCREEN)) {
            t("Turn on the App-trigger accessibility service first")
        }
        is Action.TakeScreenshot -> check(com.tuvin.pixelautomations.engine.AppWatchService.globalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)) {
            t("Turn on the App-trigger accessibility service first")
        }
        is Action.SetAlarm -> {
            val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM)
                .putExtra(android.provider.AlarmClock.EXTRA_HOUR, action.hour)
                .putExtra(android.provider.AlarmClock.EXTRA_MINUTES, action.minute)
                .putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
            if (action.label.isNotBlank()) intent.putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, action.label)
            startFromBackground(ctx, intent, "Clock")
        }
        is Action.StartTimer -> {
            val intent = Intent(android.provider.AlarmClock.ACTION_SET_TIMER)
                .putExtra(android.provider.AlarmClock.EXTRA_LENGTH, action.seconds)
                .putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
            if (action.label.isNotBlank()) intent.putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, action.label)
            startFromBackground(ctx, intent, "Clock")
        }
        is Action.SendSms -> {
            val to = action.to.ifBlank { sender } ?: error(t("No one to text; set a number"))
            val body = buildString {
                append(action.body)
                if (action.includeLocation) {
                    val loc = Locations.current(ctx)
                    if (loc != null) {
                        if (isNotEmpty()) append("\n")
                        append("https://maps.google.com/?q=${loc.latitude},${loc.longitude}")
                    }
                }
            }
            Sms.send(ctx, to, body)
        }
        is Action.If -> {
            val branch = if (Conditions.holds(action.condition, Conditions.read(ctx))) action.then else action.otherwise
            branch.forEach { run(ctx, it, depth, sender) }
        }
        // The engine handles menus at the top level; nested ones can't wait for a tap.
        is Action.ChooseFromMenu -> error(t("Choose from Menu only works as a top-level action"))
        is Action.ForceStop -> {
            if (ShizukuShell.permitted()) {
                ShizukuShell.run(ShizukuShell.forceStopCommand(action.app.pkg))
            } else {
                startFromBackground(
                    ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${action.app.pkg}")),
                    t("%s info", action.app.name),
                )
            }
            Unit
        }
    }

    /**
     * Android blocks background apps from opening screens unless they hold an exemption: an enabled Accessibility
     * service (the App trigger) or "Display over other apps". Without one, the user gets a tap-to-open notification.
     */
    private fun startFromBackground(ctx: Context, intent: Intent, what: String) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Settings.canDrawOverlays(ctx) || com.tuvin.pixelautomations.setup.Permissions.granted(
                ctx, com.tuvin.pixelautomations.setup.Permissions.ACCESSIBILITY,
            )
        ) {
            ctx.startActivity(intent)
        } else {
            ShowNotificationAction.tapToOpen(ctx, t("Tap to open %s", what), intent)
        }
    }

    /** The settings panel or page with the switch, for when Shizuku isn't available. */
    private fun radioPanel(radio: Radio): String = when (radio) {
        Radio.WIFI -> Settings.Panel.ACTION_WIFI
        Radio.MOBILE_DATA -> Settings.Panel.ACTION_INTERNET_CONNECTIVITY
        Radio.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
        Radio.AIRPLANE -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
    }

    private fun radioIsOn(ctx: Context, radio: Radio): Boolean = when (radio) {
        Radio.WIFI -> ctx.getSystemService(WifiManager::class.java).isWifiEnabled
        Radio.BLUETOOTH -> ctx.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
        Radio.MOBILE_DATA -> Settings.Global.getInt(ctx.contentResolver, "mobile_data", 1) == 1
        Radio.AIRPLANE -> Settings.Global.getInt(ctx.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
    }
}

/** iOS "Show Notification". */
object ShowNotificationAction {
    private val nextId = AtomicInteger(300_000)

    fun run(ctx: Context, a: Action.ShowNotification) {
        val n = Notification.Builder(ctx, Notifications.FROM_ACTIONS)
            .setSmallIcon(R.drawable.ic_automation)
            .setContentTitle(a.title)
            .setContentText(a.body)
            .setStyle(Notification.BigTextStyle().bigText(a.body))
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(nextId.incrementAndGet(), n)
    }

    fun tapToOpen(ctx: Context, title: String, intent: Intent) {
        val pi = android.app.PendingIntent.getActivity(ctx, nextId.get(), intent, android.app.PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(ctx, Notifications.FROM_ACTIONS)
            .setSmallIcon(R.drawable.ic_automation)
            .setContentTitle(title)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(nextId.incrementAndGet(), n)
    }
}

/** Flashlight on/off/toggle; tracks the torch state so Toggle works. */
object Torch {
    @Volatile private var on = false
    @Volatile private var watching = false

    fun set(ctx: Context, mode: Toggle) {
        val cm = ctx.getSystemService(CameraManager::class.java)
        val id = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: error(t("No flashlight on this phone"))
        if (!watching) {
            cm.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId == id) on = enabled
                }
            }, android.os.Handler(android.os.Looper.getMainLooper())) // runner threads have no Looper
            watching = true
            SystemClock.sleep(150) // let the callback report the current state before toggling
        }
        val target = when (mode) {
            Toggle.ON -> true
            Toggle.OFF -> false
            Toggle.TOGGLE -> !on
        }
        cm.setTorchMode(id, target)
        on = target
    }
}

/** iOS "Speak Text" with the phone's text-to-speech; waits until speaking ends (max 60 s). */
object Speech {
    suspend fun say(ctx: Context, text: String) {
        var engine: TextToSpeech? = null
        try {
            withTimeout(60_000) {
                val tts = suspendCancellableCoroutine<TextToSpeech?> { cont ->
                    var created: TextToSpeech? = null
                    created = TextToSpeech(ctx.applicationContext) { status ->
                        if (status == TextToSpeech.SUCCESS) cont.resume(created!!) else cont.resume(null)
                    }
                } ?: error(t("Text-to-speech isn't available"))
                engine = tts
                tts.language = Locale.getDefault()
                suspendCancellableCoroutine { cont ->
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(id: String?) {}
                        override fun onDone(id: String?) { if (cont.isActive) cont.resume(Unit) }
                        @Deprecated("Deprecated in Java")
                        override fun onError(id: String?) { if (cont.isActive) cont.resume(Unit) }
                    })
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "automation")
                }
            }
        } finally {
            engine?.shutdown()
        }
    }
}
