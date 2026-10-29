package com.tuvin.pixelautomations

import com.tuvin.pixelautomations.actions.Conditions
import com.tuvin.pixelautomations.actions.Fix
import com.tuvin.pixelautomations.actions.Geo
import com.tuvin.pixelautomations.actions.PhoneState
import com.tuvin.pixelautomations.editor.isComplete
import com.tuvin.pixelautomations.model.Action
import com.tuvin.pixelautomations.model.AppRef
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.AutomationJson
import com.tuvin.pixelautomations.model.BtDevice
import com.tuvin.pixelautomations.model.Condition
import com.tuvin.pixelautomations.model.Internet
import com.tuvin.pixelautomations.model.Place
import com.tuvin.pixelautomations.model.RingerMode
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.model.label
import com.tuvin.pixelautomations.setup.Permissions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The If conditions added on top of the original seven (battery, charging, Wi-Fi name, time, DND, weekday). */
class IfConditionsTest {
    private val buds = BtDevice("AA:BB:CC:DD:EE:FF", "Buds")
    private val home = Place("Home", 32.0853, 34.7818, radius = 150)
    private val netflix = AppRef("com.netflix.mediaclient", "Netflix")

    // Evening on battery saver, airplane off, dark mode, on mobile data with the buds connected, ringer on
    // vibrate at 70 % volume, music playing, not on a call, 100 m from home, phone unlocked, Netflix in front.
    private val s = PhoneState(
        batteryPercent = 30, charging = false, ssid = null, minuteOfDay = 21 * 60, dayOfWeek = 3, dndOn = false,
        batterySaver = true, airplane = false, darkMode = true, internet = Internet.MOBILE,
        bluetooth = setOf("AA:BB:CC:DD:EE:FF"), headphones = true, ringer = RingerMode.VIBRATE, volumePercent = 70,
        musicPlaying = true, onCall = false, here = Fix(32.0853, 34.7829, accuracy = 20f), locked = false,
        foregroundApp = netflix.pkg, roll = 29,
    )

    private fun holds(c: Condition, state: PhoneState = s) = Conditions.holds(c, state)

    @Test fun powerAndDeviceConditions() {
        assertTrue(holds(Condition.BatterySaver(on = true)))
        assertFalse(holds(Condition.BatterySaver(on = false)))
        assertFalse(holds(Condition.Airplane(on = true)))
        assertTrue(holds(Condition.Airplane(on = false)))
        assertTrue(holds(Condition.DarkMode(on = true)))
        assertFalse(holds(Condition.DarkMode(on = true), s.copy(darkMode = false)))
    }

    @Test fun connectionConditions() {
        assertTrue(holds(Condition.InternetVia(Internet.MOBILE)))
        assertFalse(holds(Condition.InternetVia(Internet.WIFI)))
        assertTrue(holds(Condition.InternetVia(Internet.NONE), s.copy(internet = Internet.NONE)))
        // Any device, or one device by address (whatever its letter case).
        assertTrue(holds(Condition.BluetoothConnected(device = null, connected = true)))
        assertFalse(holds(Condition.BluetoothConnected(device = null, connected = true), s.copy(bluetooth = emptySet())))
        assertTrue(holds(Condition.BluetoothConnected(device = buds.copy(address = "aa:bb:cc:dd:ee:ff"), connected = true)))
        assertFalse(holds(Condition.BluetoothConnected(device = BtDevice("11:22:33:44:55:66", "Car"), connected = true)))
        assertTrue(holds(Condition.BluetoothConnected(device = BtDevice("11:22:33:44:55:66", "Car"), connected = false)))
        assertTrue(holds(Condition.Headphones(connected = true)))
        assertTrue(holds(Condition.Headphones(connected = false), s.copy(headphones = false)))
    }

    @Test fun soundAndCallConditions() {
        assertTrue(holds(Condition.Ringer(RingerMode.VIBRATE)))
        assertFalse(holds(Condition.Ringer(RingerMode.SILENT)))
        assertTrue(holds(Condition.Volume(above = true, percent = 50)))
        assertFalse(holds(Condition.Volume(above = true, percent = 70))) // "above" is strict, like battery
        assertTrue(holds(Condition.Volume(above = false, percent = 80)))
        assertTrue(holds(Condition.MusicPlaying(yes = true)))
        assertFalse(holds(Condition.OnCall(yes = true)))
        assertTrue(holds(Condition.OnCall(yes = true), s.copy(onCall = true)))
    }

    @Test fun placeConditionAllowsForGpsErrorAndNeedsAFix() {
        assertEquals(1000.0, Geo.distanceMeters(32.0, 34.0, 32.0, 34.01059), 15.0) // ~1 km east
        assertTrue(holds(Condition.AtPlace(home, there = true)))                    // 100 m from a 150 m circle
        assertFalse(holds(Condition.AtPlace(home, there = false)))
        val far = s.copy(here = Fix(32.0853, 34.8300, accuracy = 20f))                // ~4.5 km east
        assertFalse(holds(Condition.AtPlace(home, there = true), far))
        assertTrue(holds(Condition.AtPlace(home, there = false), far))
        // Just outside the circle, but inside the circle + GPS accuracy (same slack as Before I Commute).
        val edge = s.copy(here = Fix(32.0853, 34.78397, accuracy = 60f))              // ~205 m
        assertTrue(holds(Condition.AtPlace(home, there = true), edge))
        assertFalse(holds(Condition.AtPlace(home, there = true), edge.copy(here = edge.here!!.copy(accuracy = 10f))))
        // No location: neither "at" nor "not at" can be confirmed.
        assertFalse(holds(Condition.AtPlace(home, there = true), s.copy(here = null)))
        assertFalse(holds(Condition.AtPlace(home, there = false), s.copy(here = null)))
        assertFalse(holds(Condition.AtPlace(place = null, there = true)))
    }

    @Test fun screenConditions() {
        assertTrue(holds(Condition.Locked(yes = false)))
        assertTrue(holds(Condition.Locked(yes = true), s.copy(locked = true)))
        assertTrue(holds(Condition.AppInFront(netflix, yes = true)))
        assertFalse(holds(Condition.AppInFront(AppRef("com.whatsapp", "WhatsApp"), yes = true)))
        assertTrue(holds(Condition.AppInFront(AppRef("com.whatsapp", "WhatsApp"), yes = false)))
        assertFalse(holds(Condition.AppInFront(netflix, yes = true), s.copy(foregroundApp = null)))
    }

    @Test fun chanceUsesTheRoll() {
        assertTrue(holds(Condition.Chance(30)))                    // roll 29 < 30
        assertFalse(holds(Condition.Chance(29)))
        assertFalse(holds(Condition.Chance(0), s.copy(roll = 0)))  // never
        assertTrue(holds(Condition.Chance(100), s.copy(roll = 99))) // always
    }

    @Test fun labels() {
        assertEquals("battery saver is on", Condition.BatterySaver(true).label())
        assertEquals("internet is over mobile data", Condition.InternetVia(Internet.MOBILE).label())
        assertEquals("Buds is connected over Bluetooth", Condition.BluetoothConnected(buds).label())
        assertEquals("no Bluetooth device is connected", Condition.BluetoothConnected(null, connected = false).label())
        assertEquals("ringer is on vibrate", Condition.Ringer(RingerMode.VIBRATE).label())
        assertEquals("volume is below 20%", Condition.Volume(above = false, percent = 20).label())
        assertEquals("I'm not at Home", Condition.AtPlace(home, there = false).label())
        assertEquals("Netflix is in front", Condition.AppInFront(netflix).label())
        assertEquals("30% of the time", Condition.Chance(30).label())
        assertEquals("If the phone is locked", Action.If(Condition.Locked(true)).label())
    }

    @Test fun completeness() {
        assertFalse(Action.If(Condition.AtPlace(place = null)).isComplete())
        assertTrue(Action.If(Condition.AtPlace(home)).isComplete())
        assertFalse(Action.If(Condition.AppInFront(AppRef("", ""))).isComplete())
        assertTrue(Action.If(Condition.BluetoothConnected(device = null)).isComplete()) // any device is fine
        assertTrue(Action.If(Condition.Chance(0)).isComplete())
    }

    @Test fun conditionsDeclareTheirPermissions() {
        assertEquals(listOf(Permissions.FINE_LOCATION, Permissions.BACKGROUND_LOCATION), Permissions.needed(Action.If(Condition.AtPlace(home))))
        assertEquals(listOf(Permissions.NEARBY), Permissions.needed(Action.If(Condition.BluetoothConnected(buds))))
        assertEquals(listOf(Permissions.ACCESSIBILITY), Permissions.needed(Action.If(Condition.AppInFront(netflix))))
        assertEquals(emptyList(), Permissions.needed(Action.If(Condition.Headphones())))
    }

    @Test fun jsonRoundTrip() {
        val all = listOf(
            Condition.BatterySaver(), Condition.Airplane(), Condition.DarkMode(), Condition.InternetVia(Internet.NONE),
            Condition.BluetoothConnected(buds), Condition.BluetoothConnected(null), Condition.Headphones(),
            Condition.Ringer(RingerMode.SILENT), Condition.Volume(above = false, percent = 10), Condition.MusicPlaying(),
            Condition.OnCall(), Condition.AtPlace(home), Condition.Locked(), Condition.AppInFront(netflix), Condition.Chance(5),
        )
        val a = Automation(id = 4, trigger = Trigger.Charger(), actions = all.map { Action.If(it) })
        assertEquals(a, AutomationJson.decode(AutomationJson.encode(a)))
    }
}
