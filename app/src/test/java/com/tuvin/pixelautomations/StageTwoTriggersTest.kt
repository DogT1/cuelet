package com.tuvin.pixelautomations

import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.AutomationJson
import com.tuvin.pixelautomations.model.BtDevice
import com.tuvin.pixelautomations.model.LevelCondition
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.model.label
import com.tuvin.pixelautomations.triggers.AirplaneModeTrigger
import com.tuvin.pixelautomations.triggers.AndroidAutoTrigger
import com.tuvin.pixelautomations.triggers.BatteryLevelTrigger
import com.tuvin.pixelautomations.triggers.BluetoothTrigger
import com.tuvin.pixelautomations.triggers.LowPowerModeTrigger
import com.tuvin.pixelautomations.triggers.StateTracker
import com.tuvin.pixelautomations.triggers.WifiTrigger
import com.tuvin.pixelautomations.triggers.WifiTrigger.Event.Joined
import com.tuvin.pixelautomations.triggers.WifiTrigger.Event.Left
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StageTwoTriggersTest {

    // Battery Level
    private val below20 = Trigger.BatteryLevel(LevelCondition.FALLS_BELOW, 20)
    private val above80 = Trigger.BatteryLevel(LevelCondition.RISES_ABOVE, 80)
    private val equals50 = Trigger.BatteryLevel(LevelCondition.EQUALS, 50)

    @Test fun fallsBelowFiresOnceWhenCrossing() {
        assertTrue(BatteryLevelTrigger.matches(below20, 20, 19))
        assertTrue(BatteryLevelTrigger.matches(below20, 21, 19))
        assertFalse(BatteryLevelTrigger.matches(below20, 19, 18)) // already below
        assertFalse(BatteryLevelTrigger.matches(below20, 18, 19)) // rising
        assertFalse(BatteryLevelTrigger.matches(below20, null, 10)) // first reading only primes
    }

    @Test fun risesAboveFiresOnceWhenCrossing() {
        assertTrue(BatteryLevelTrigger.matches(above80, 80, 81))
        assertFalse(BatteryLevelTrigger.matches(above80, 81, 82))
        assertFalse(BatteryLevelTrigger.matches(above80, 82, 81))
    }

    @Test fun equalsFiresOnArrivalFromEitherSide() {
        assertTrue(BatteryLevelTrigger.matches(equals50, 51, 50))
        assertTrue(BatteryLevelTrigger.matches(equals50, 49, 50))
        assertFalse(BatteryLevelTrigger.matches(equals50, 50, 50))
        assertFalse(BatteryLevelTrigger.matches(equals50, 50, 49))
    }

    // Switches
    @Test fun switchTriggersMatchDirection() {
        assertTrue(LowPowerModeTrigger.matches(Trigger.LowPowerMode(on = true), on = true))
        assertFalse(LowPowerModeTrigger.matches(Trigger.LowPowerMode(on = true), on = false))
        assertTrue(AirplaneModeTrigger.matches(Trigger.AirplaneMode(on = false), on = false))
        assertTrue(AndroidAutoTrigger.matches(Trigger.AndroidAuto(connects = true), connected = true))
        assertFalse(AndroidAutoTrigger.matches(Trigger.AndroidAuto(connects = false), connected = true))
    }

    @Test fun stateTrackerIgnoresRepeats() {
        val t = StateTracker<Boolean>()
        assertFalse(t.changed(true)) // priming
        assertFalse(t.changed(true))
        assertTrue(t.changed(false))
        val u = StateTracker<Boolean>()
        assertTrue(u.changedOrFirst(true))
        assertFalse(u.changedOrFirst(true))
    }

    // Bluetooth
    private val buds = BtDevice("AA:BB:CC:DD:EE:01", "Galaxy Buds")

    @Test fun bluetoothMatchesChosenDeviceOrAny() {
        val chosen = Trigger.Bluetooth(listOf(buds), connected = true)
        assertTrue(BluetoothTrigger.matches(chosen, "aa:bb:cc:dd:ee:01", connected = true))
        assertFalse(BluetoothTrigger.matches(chosen, "AA:BB:CC:DD:EE:02", connected = true))
        assertFalse(BluetoothTrigger.matches(chosen, "AA:BB:CC:DD:EE:01", connected = false))
        assertTrue(BluetoothTrigger.matches(Trigger.Bluetooth(connected = false), "11:22:33:44:55:66", connected = false))
    }

    // Wi-Fi
    @Test fun ssidNormalising() {
        assertEquals("kolbasa", WifiTrigger.normalize("\"kolbasa\""))
        assertEquals(null, WifiTrigger.normalize("<unknown ssid>"))
        assertEquals(null, WifiTrigger.normalize(null))
    }

    @Test fun wifiTransitions() {
        assertEquals(listOf(Joined("home")), WifiTrigger.transition(null, "home", wasConnected = false, isConnected = true))
        assertEquals(listOf(Left("home")), WifiTrigger.transition("home", null, wasConnected = true, isConnected = false))
        assertEquals(
            listOf(Left("home"), Joined("work")),
            WifiTrigger.transition("home", "work", wasConnected = true, isConnected = true),
        )
        assertEquals(emptyList(), WifiTrigger.transition("home", "home", wasConnected = true, isConnected = true))
    }

    @Test fun wifiMatching() {
        val joinHome = Trigger.WiFi(listOf("home"), joins = true)
        assertTrue(WifiTrigger.matches(joinHome, Joined("home")))
        assertFalse(WifiTrigger.matches(joinHome, Joined("work")))
        assertFalse(WifiTrigger.matches(joinHome, Left("home")))
        assertFalse(WifiTrigger.matches(joinHome, Joined(null))) // hidden name never matches a named network
        val leaveAny = Trigger.WiFi(joins = false)
        assertTrue(WifiTrigger.matches(leaveAny, Left(null)))
        assertTrue(WifiTrigger.matches(leaveAny, Left("cafe")))
    }

    // JSON + labels for every new trigger
    @Test fun newTriggersRoundTripAndRead() {
        val triggers = listOf(
            below20, Trigger.LowPowerMode(false), Trigger.AirplaneMode(true),
            Trigger.Bluetooth(listOf(buds), false), Trigger.WiFi(listOf("home"), true), Trigger.AndroidAuto(true),
        )
        triggers.forEach { t ->
            val a = Automation(id = 1, trigger = t)
            assertEquals(a, AutomationJson.decode(AutomationJson.encode(a)))
        }
        assertEquals("When battery falls below 20%", below20.label())
        assertEquals("When Galaxy Buds disconnects", Trigger.Bluetooth(listOf(buds), false).label())
        assertEquals("When phone joins home", Trigger.WiFi(listOf("home"), true).label())
        assertEquals("When Android Auto connects", Trigger.AndroidAuto(true).label())
    }
}
