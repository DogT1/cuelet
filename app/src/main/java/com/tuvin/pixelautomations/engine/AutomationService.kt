package com.tuvin.pixelautomations.engine

import com.tuvin.pixelautomations.setup.t

import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.os.BatteryManager
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.Observer
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.triggers.AirplaneModeTrigger
import com.tuvin.pixelautomations.triggers.AndroidAutoTrigger
import com.tuvin.pixelautomations.triggers.BatteryLevelTrigger
import com.tuvin.pixelautomations.triggers.BluetoothTrigger
import com.tuvin.pixelautomations.triggers.ChargerTrigger
import com.tuvin.pixelautomations.triggers.LowPowerModeTrigger
import com.tuvin.pixelautomations.triggers.SleepRules
import com.tuvin.pixelautomations.triggers.StateTracker
import com.tuvin.pixelautomations.triggers.WifiTrigger

/**
 * Always-on listener for system events that Android only delivers to running apps.
 * Started while at least one automation is enabled.
 */
class AutomationService : Service() {

    private val receivers = mutableListOf<BroadcastReceiver>()
    private val powerSave = StateTracker<Boolean>()
    private val airplane = StateTracker<Boolean>()
    private val car = StateTracker<Boolean>()
    private val focus = StateTracker<Boolean>()
    private var carConnection: CarConnection? = null
    private val carObserver = Observer<Int> { type ->
        val connected = type != CarConnection.CONNECTION_TYPE_NOT_CONNECTED
        if (car.changed(connected)) {
            fire(t(if (connected) "Android Auto connected" else "Android Auto disconnected")) { t ->
                t is Trigger.AndroidAuto && AndroidAutoTrigger.matches(t, connected)
            }
        }
    }
    private var wifiCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(Notifications.SERVICE_ID, Notifications.service(this), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        listenCharger()
        listenBattery()
        listenPowerSave()
        listenAirplane()
        listenBluetooth()
        listenWifi()
        listenFocus()
        carConnection = CarConnection(this).also { it.type.observeForever(carObserver) }
        Log.i(TAG, "service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        receivers.forEach { runCatching { unregisterReceiver(it) } }
        wifiCallback?.let { runCatching { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(it) } }
        carConnection?.type?.removeObserver(carObserver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Fires every enabled automation whose trigger passes [matches]. */
    private fun fire(why: String, matches: (Trigger) -> Boolean) {
        val ctx = applicationContext
        Engine.scope.launchCatching { Engine.onEvent(ctx, why) { a: Automation -> matches(a.trigger) } }
    }

    /**
     * [fromOtherProcess]: the broadcast comes from a system app other than Android's core (e.g. the Bluetooth
     * stack). NOT_EXPORTED receivers silently drop those, so they need EXPORTED. The actions are protected
     * broadcasts, which other apps can't send.
     */
    private fun listen(vararg actions: String, fromOtherProcess: Boolean = false, onReceive: (Intent) -> Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) = onReceive(intent)
        }
        val flags = if (fromOtherProcess) RECEIVER_EXPORTED else RECEIVER_NOT_EXPORTED
        registerReceiver(r, IntentFilter().apply { actions.forEach(::addAction) }, flags)
        receivers += r
    }

    private fun listenCharger() = listen(Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED) { i ->
        val connected = i.action == Intent.ACTION_POWER_CONNECTED
        fire(t(if (connected) "Charger connected" else "Charger disconnected")) { t ->
            t is Trigger.Charger && ChargerTrigger.matches(t, connected)
        }
    }

    private fun listenBattery() = listen(Intent.ACTION_BATTERY_CHANGED) { i ->
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level < 0 || scale <= 0) return@listen
        val percent = level * 100 / scale
        val previous = lastBatteryPercent
        lastBatteryPercent = percent
        // The first (sticky) reading only primes; temperature/voltage updates repeat the same percent.
        if (previous != null && previous != percent) {
            fire(t("Battery at %d%%", percent)) { t -> t is Trigger.BatteryLevel && BatteryLevelTrigger.matches(t, previous, percent) }
        }
    }
    private var lastBatteryPercent: Int? = null

    private fun listenPowerSave() = listen(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
        val on = getSystemService(PowerManager::class.java).isPowerSaveMode
        if (powerSave.changedOrFirst(on)) {
            fire(t(if (on) "Battery Saver on" else "Battery Saver off")) { t -> t is Trigger.LowPowerMode && LowPowerModeTrigger.matches(t, on) }
        }
    }

    /** iOS "Focus": Android reports Do Not Disturb (any Mode) as an interruption filter, not by Mode name. */
    private fun listenFocus() = listen(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED) {
        val filter = getSystemService(NotificationManager::class.java).currentInterruptionFilter
        if (filter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) return@listen
        val on = filter != NotificationManager.INTERRUPTION_FILTER_ALL
        if (focus.changedOrFirst(on)) {
            fire(t(if (on) "Do Not Disturb on" else "Do Not Disturb off")) { t -> t is Trigger.Focus && t.on == on }
            val now = java.time.LocalTime.now()
            val minute = now.hour * 60 + now.minute
            fire(t(if (on) "Bedtime began" else "Woke up")) { t -> t is Trigger.Sleep && SleepRules.matchesModeChange(t, on, minute) }
        }
    }

    private fun listenAirplane() = listen(Intent.ACTION_AIRPLANE_MODE_CHANGED) { i ->
        val on = i.getBooleanExtra("state", false)
        if (airplane.changedOrFirst(on)) {
            fire(t(if (on) "Airplane Mode on" else "Airplane Mode off")) { t -> t is Trigger.AirplaneMode && AirplaneModeTrigger.matches(t, on) }
        }
    }

    /** Needs the Nearby devices (BLUETOOTH_CONNECT) permission; without it Android doesn't deliver these. */
    private fun listenBluetooth() = listen(
        BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED, fromOtherProcess = true,
    ) { i ->
        val device = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return@listen
        val connected = i.action == BluetoothDevice.ACTION_ACL_CONNECTED
        val name = runCatching { device.name }.getOrNull() ?: device.address
        fire(t(if (connected) "%s connected" else "%s disconnected", name)) { t ->
            t is Trigger.Bluetooth && BluetoothTrigger.matches(t, device.address, connected)
        }
    }

    private fun listenWifi() {
        val cm = getSystemService(ConnectivityManager::class.java)
        val registeredAt = SystemClock.elapsedRealtime()
        var current: Network? = null
        var ssid: String? = null
        val cb = object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val newSsid = WifiTrigger.normalize((caps.transportInfo as? WifiInfo)?.ssid)
                if (network == current && newSsid == ssid) return
                // Android replays the current network right after registering; that only primes the state.
                val priming = SystemClock.elapsedRealtime() - registeredAt < 1500
                val events = WifiTrigger.transition(ssid, newSsid, current != null, true)
                current = network
                ssid = newSsid
                if (!priming) events.forEach(::fireWifi)
            }

            override fun onLost(network: Network) {
                if (network != current) return
                val events = WifiTrigger.transition(ssid, null, true, false)
                current = null
                ssid = null
                events.forEach(::fireWifi)
            }
        }
        val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        cm.registerNetworkCallback(request, cb)
        wifiCallback = cb
    }

    private fun fireWifi(e: WifiTrigger.Event) {
        val name = e.ssid ?: t("Wi-Fi")
        fire(t(if (e is WifiTrigger.Event.Joined) "Joined %s" else "Left %s", name)) { t ->
            t is Trigger.WiFi && WifiTrigger.matches(t, e)
        }
    }

    companion object {
        fun update(ctx: Context, needed: Boolean) {
            val intent = Intent(ctx, AutomationService::class.java)
            try {
                if (needed) ctx.startForegroundService(intent) else ctx.stopService(intent)
            } catch (e: Exception) {
                // Android refuses a start from some background states; the next boot or app open retries.
                Log.w(TAG, "service start refused", e)
            }
        }
    }
}
