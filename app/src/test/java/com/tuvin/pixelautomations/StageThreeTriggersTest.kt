package com.tuvin.pixelautomations

import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.AutomationJson
import com.tuvin.pixelautomations.model.Place
import com.tuvin.pixelautomations.model.SolarEvent
import com.tuvin.pixelautomations.model.TimeWindow
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.model.label
import com.tuvin.pixelautomations.model.offsetLabel
import com.tuvin.pixelautomations.triggers.ArriveLeaveTrigger
import com.tuvin.pixelautomations.triggers.SolarCalc
import com.tuvin.pixelautomations.triggers.TimeOfDayTrigger
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StageThreeTriggersTest {

    private fun assertNear(expected: ZonedDateTime, actual: ZonedDateTime?, minutes: Long = 3) {
        requireNotNull(actual) { "expected $expected, got null" }
        val diff = abs(Duration.between(expected, actual).toMinutes())
        assertTrue(diff <= minutes, "expected about $expected, got $actual ($diff min off)")
    }

    // Sunrise / sunset against published almanac times
    @Test fun londonMidsummer() {
        val london = ZoneId.of("Europe/London")
        val day = LocalDate.of(2024, 6, 21)
        assertNear(ZonedDateTime.of(2024, 6, 21, 4, 43, 0, 0, london), SolarCalc.time(day, 51.5074, -0.1278, london, sunrise = true))
        assertNear(ZonedDateTime.of(2024, 6, 21, 21, 21, 0, 0, london), SolarCalc.time(day, 51.5074, -0.1278, london, sunrise = false))
    }

    @Test fun newYorkMidwinter() {
        val ny = ZoneId.of("America/New_York")
        val day = LocalDate.of(2024, 12, 21)
        assertNear(ZonedDateTime.of(2024, 12, 21, 7, 16, 0, 0, ny), SolarCalc.time(day, 40.7128, -74.0060, ny, sunrise = true))
        assertNear(ZonedDateTime.of(2024, 12, 21, 16, 32, 0, 0, ny), SolarCalc.time(day, 40.7128, -74.0060, ny, sunrise = false))
    }

    @Test fun polarNightHasNoSunrise() {
        assertNull(SolarCalc.time(LocalDate.of(2024, 12, 21), 69.6492, 18.9553, ZoneId.of("Europe/Oslo"), sunrise = true))
    }

    private val tlv = ZoneId.of("Asia/Jerusalem")
    private val tlvSunset = Trigger.TimeOfDay(0, 0, solar = SolarEvent.SUNSET, offsetMinutes = -30, lat = 32.0853, lng = 34.7818)

    @Test fun solarOffsetAppliesAndRollsToTomorrow() {
        val now = ZonedDateTime.of(2026, 10, 27, 12, 0, 0, 0, tlv)
        val sunset = SolarCalc.time(now.toLocalDate(), 32.0853, 34.7818, tlv, sunrise = false)!!
        assertEquals(sunset.minusMinutes(30), TimeOfDayTrigger.nextFire(now, tlvSunset))
        val late = ZonedDateTime.of(2026, 10, 27, 23, 0, 0, 0, tlv)
        assertEquals(28, TimeOfDayTrigger.nextFire(late, tlvSunset)!!.dayOfMonth)
    }

    @Test fun solarWithoutPlaceNeverSchedules() {
        assertNull(TimeOfDayTrigger.nextFire(ZonedDateTime.now(tlv), tlvSunset.copy(lat = null, lng = null)))
    }

    // Before I Commute
    private val home = Place("Home", 32.08, 34.78, 150)
    private val work = Place("Work", 32.07, 34.79, 200)

    @Test fun commuteRunsBeforeDepartureOnDepartureDays() {
        // Leaves 08:00 Sunday–Thursday (Israeli work week), 15 min before. Tuesday 27 Oct 2026, 07:00.
        val t = Trigger.BeforeCommute(home, work, 8, 0, 15, days = setOf(7, 1, 2, 3, 4))
        val now = ZonedDateTime.of(2026, 10, 27, 7, 0, 0, 0, tlv)
        assertEquals(ZonedDateTime.of(2026, 10, 27, 7, 45, 0, 0, tlv), TimeOfDayTrigger.nextFire(now, t))
        // Thursday 08:00 has passed: next is Sunday 1 Nov 07:45 (Friday and Saturday skipped).
        val thu = ZonedDateTime.of(2026, 10, 29, 9, 0, 0, 0, tlv)
        assertEquals(ZonedDateTime.of(2026, 11, 1, 7, 45, 0, 0, tlv), TimeOfDayTrigger.nextFire(thu, t))
    }

    @Test fun commuteJustAfterMidnightRunsTheEveningBefore() {
        val t = Trigger.BeforeCommute(home, work, 0, 10, 30, days = setOf(3)) // Wednesday 00:10, 30 min before
        val now = ZonedDateTime.of(2026, 10, 27, 12, 0, 0, 0, tlv) // Tuesday noon
        assertEquals(ZonedDateTime.of(2026, 10, 27, 23, 40, 0, 0, tlv), TimeOfDayTrigger.nextFire(now, t))
    }

    // Arrive / Leave
    @Test fun windowsIncludingOvernight() {
        assertTrue(TimeWindow(8 * 60, 20 * 60).contains(12 * 60))
        assertFalse(TimeWindow(8 * 60, 20 * 60).contains(21 * 60))
        val night = TimeWindow(22 * 60, 6 * 60)
        assertTrue(night.contains(23 * 60))
        assertTrue(night.contains(5 * 60))
        assertFalse(night.contains(12 * 60))
    }

    @Test fun arriveAndLeaveMatchTheirDirectionAndWindow() {
        val arrive = Trigger.Arrive(home)
        val leaveDaytime = Trigger.Leave(home, TimeWindow(8 * 60, 20 * 60))
        assertTrue(ArriveLeaveTrigger.matches(arrive, entered = true, minuteOfDay = 3))
        assertFalse(ArriveLeaveTrigger.matches(arrive, entered = false, minuteOfDay = 3))
        assertTrue(ArriveLeaveTrigger.matches(leaveDaytime, entered = false, minuteOfDay = 9 * 60))
        assertFalse(ArriveLeaveTrigger.matches(leaveDaytime, entered = false, minuteOfDay = 23 * 60))
        assertEquals(42L, ArriveLeaveTrigger.automationId(ArriveLeaveTrigger.geofenceId(42)))
    }

    // JSON and labels
    @Test fun roundTripAndLabels() {
        val commute = Trigger.BeforeCommute(home, work, 8, 0, 15)
        listOf(tlvSunset, Trigger.Arrive(home, TimeWindow(60, 120)), Trigger.Leave(work), commute).forEach { t ->
            val a = Automation(id = 1, trigger = t)
            assertEquals(a, AutomationJson.decode(AutomationJson.encode(a)))
        }
        // Stage 1 automations saved without the new fields still load.
        val old = """{"id":1,"trigger":{"type":"time_of_day","hour":7,"minute":0,"days":[]}}"""
        assertEquals(Trigger.TimeOfDay(7, 0), AutomationJson.decode(old).trigger)

        assertEquals("30 min before sunset, daily", tlvSunset.label())
        assertEquals("When I arrive at Home, 01:00–02:00", Trigger.Arrive(home, TimeWindow(60, 120)).label())
        assertEquals("15 min before my 08:00 commute to Work", commute.label())
        assertEquals("1 h after", offsetLabel(60))
    }
}
