package com.tuvin.pixelautomations.actions

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import com.tuvin.pixelautomations.engine.AppWatchService
import com.tuvin.pixelautomations.model.Condition
import com.tuvin.pixelautomations.model.Internet
import com.tuvin.pixelautomations.model.RingerMode
import com.tuvin.pixelautomations.setup.Permissions
import com.tuvin.pixelautomations.triggers.Locations
import com.tuvin.pixelautomations.triggers.WifiTrigger
import java.time.LocalDateTime
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** A location fix: where the phone is, and how far off that may be, in metres. */
data class Fix(val lat: Double, val lng: Double, val accuracy: Float)

/** The phone's state at the moment an If action runs. */
data class PhoneState(
    val batteryPercent: Int,
    val charging: Boolean,
    val ssid: String?,
    val minuteOfDay: Int,
    val dayOfWeek: Int,
    val dndOn: Boolean,
    val batterySaver: Boolean = false,
    val airplane: Boolean = false,
    val darkMode: Boolean = false,
    val internet: Internet = Internet.NONE,
    /** Upper-case addresses of the Bluetooth devices connected right now. */
    val bluetooth: Set<String> = emptySet(),
    val headphones: Boolean = false,
    val ringer: RingerMode = RingerMode.NORMAL,
    val volumePercent: Int = 0,
    val musicPlaying: Boolean = false,
    val onCall: Boolean = false,
    /** Only read when the If asks about a place, since a fix can take seconds. */
    val here: Fix? = null,
    val locked: Boolean = false,
    /** Package of the app in front; known only while the App-trigger accessibility service runs. */
    val foregroundApp: String? = null,
    /** 0..99, rolled fresh for each If; Chance(p) holds when it's below p. */
    val roll: Int = 0,
)

object Geo {
    /** Great-circle distance in metres (haversine), plenty accurate at automation scale. */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * 6_371_000.0 * asin(sqrt(a))
    }
}

object Conditions {
    fun holds(c: Condition, s: PhoneState): Boolean = when (c) {
        is Condition.BatteryAbove -> s.batteryPercent > c.percent
        is Condition.BatteryBelow -> s.batteryPercent < c.percent
        is Condition.Charging -> s.charging == c.yes
        is Condition.WifiIs -> s.ssid != null && s.ssid.equals(c.ssid.trim(), ignoreCase = false)
        is Condition.TimeBetween -> c.window.contains(s.minuteOfDay)
        is Condition.DndOn -> s.dndOn == c.yes
        is Condition.Weekday -> s.dayOfWeek in c.days
        is Condition.BatterySaver -> s.batterySaver == c.on
        is Condition.Airplane -> s.airplane == c.on
        is Condition.DarkMode -> s.darkMode == c.on
        is Condition.InternetVia -> s.internet == c.via
        is Condition.BluetoothConnected -> {
            val hit = if (c.device == null) s.bluetooth.isNotEmpty() else c.device.address.uppercase() in s.bluetooth
            hit == c.connected
        }
        is Condition.Headphones -> s.headphones == c.connected
        is Condition.Ringer -> s.ringer == c.mode
        is Condition.Volume -> if (c.above) s.volumePercent > c.percent else s.volumePercent < c.percent
        is Condition.MusicPlaying -> s.musicPlaying == c.yes
        is Condition.OnCall -> s.onCall == c.yes
        is Condition.AtPlace -> {
            val place = c.place
            val here = s.here
            // Without a fix neither "at" nor "not at" can be confirmed.
            if (place == null || here == null) false
            else {
                // Allow for GPS error on top of the place's radius, like Before I Commute.
                val d = Geo.distanceMeters(here.lat, here.lng, place.lat, place.lng)
                (d <= place.radius + maxOf(here.accuracy, 50f)) == c.there
            }
        }
        is Condition.Locked -> s.locked == c.yes
        is Condition.AppInFront -> (s.foregroundApp == c.app.pkg) == c.yes
        is Condition.Chance -> s.roll < c.percent
    }

    /** Reads what [c] needs: a location fix is only requested for a place check. */
    suspend fun read(ctx: Context, c: Condition): PhoneState {
        val state = read(ctx)
        if (c !is Condition.AtPlace) return state
        val fix = Locations.current(ctx, timeoutMs = 15_000)
        return state.copy(here = fix?.let { Fix(it.latitude, it.longitude, it.accuracy) })
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
        val audio = ctx.getSystemService(AudioManager::class.java)
        val outputs = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val caps = ctx.getSystemService(ConnectivityManager::class.java).let { cm -> cm.activeNetwork?.let(cm::getNetworkCapabilities) }
        return PhoneState(
            batteryPercent = if (level >= 0 && scale > 0) level * 100 / scale else 0,
            charging = plugged,
            ssid = ssid,
            minuteOfDay = now.hour * 60 + now.minute,
            dayOfWeek = now.dayOfWeek.value,
            dndOn = filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN,
            batterySaver = ctx.getSystemService(PowerManager::class.java).isPowerSaveMode,
            airplane = Settings.Global.getInt(ctx.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0,
            darkMode = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES,
            internet = when {
                caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> Internet.NONE
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Internet.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Internet.MOBILE
                else -> Internet.NONE
            },
            bluetooth = (outputs.filter { it.type in BLUETOOTH_AUDIO }.map { it.address } + leDevices(ctx))
                .map { it.uppercase() }.filter { it.isNotBlank() }.toSet(),
            headphones = outputs.any { it.type in HEADPHONES },
            ringer = when (audio.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> RingerMode.SILENT
                AudioManager.RINGER_MODE_VIBRATE -> RingerMode.VIBRATE
                else -> RingerMode.NORMAL
            },
            volumePercent = if (maxVolume > 0) audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / maxVolume else 0,
            musicPlaying = audio.isMusicActive,
            onCall = audio.mode == AudioManager.MODE_IN_CALL || audio.mode == AudioManager.MODE_IN_COMMUNICATION,
            locked = ctx.getSystemService(KeyguardManager::class.java).isKeyguardLocked,
            foregroundApp = AppWatchService.foregroundApp,
            roll = Random.nextInt(100),
        )
    }

    private val BLUETOOTH_AUDIO = setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER, AudioDeviceInfo.TYPE_HEARING_AID,
    )

    /** Wired or Bluetooth audio worn on the head (a Bluetooth speaker counts too; Android can't tell them apart). */
    private val HEADPHONES = BLUETOOTH_AUDIO + setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_USB_HEADSET,
    )

    /** Devices connected over Bluetooth LE (watches, trackers); empty without the Nearby devices permission. */
    @SuppressLint("MissingPermission")
    private fun leDevices(ctx: Context): List<String> {
        if (!Permissions.granted(ctx, Permissions.NEARBY)) return emptyList()
        return runCatching {
            ctx.getSystemService(BluetoothManager::class.java).getConnectedDevices(BluetoothProfile.GATT).map { it.address }
        }.getOrDefault(emptyList())
    }
}
