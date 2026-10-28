package com.tuvin.pixelautomations

import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.triggers.TimeOfDayTrigger
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeOfDayTriggerTest {
    private val tz = ZoneId.of("Asia/Jerusalem")

    // 2026-10-27 is a Tuesday.
    private fun at(day: Int, h: Int, m: Int, month: Int = 10) = ZonedDateTime.of(2026, month, day, h, m, 0, 0, tz)

    @Test fun laterTodayWhenTimeNotPassed() {
        assertEquals(at(27, 21, 30), TimeOfDayTrigger.nextOccurrence(at(27, 20, 0), Trigger.TimeOfDay(21, 30)))
    }

    @Test fun tomorrowWhenTimePassed() {
        assertEquals(at(28, 7, 0), TimeOfDayTrigger.nextOccurrence(at(27, 20, 0), Trigger.TimeOfDay(7, 0)))
    }

    @Test fun sameMinuteMeansNextDay() {
        assertEquals(at(28, 20, 0), TimeOfDayTrigger.nextOccurrence(at(27, 20, 0), Trigger.TimeOfDay(20, 0)))
    }

    @Test fun skipsToNextAllowedDay() {
        // Weekends only, asked on Tuesday evening: Saturday 31 Oct.
        val t = Trigger.TimeOfDay(9, 0, days = setOf(6, 7))
        assertEquals(at(31, 9, 0), TimeOfDayTrigger.nextOccurrence(at(27, 20, 0), t))
    }

    @Test fun sameWeekdayNextWeekWhenTodayPassed() {
        val t = Trigger.TimeOfDay(8, 0, days = setOf(2)) // Tuesday
        assertEquals(at(3, 8, 0, month = 11), TimeOfDayTrigger.nextOccurrence(at(27, 9, 0), t))
    }

    @Test fun keepsWallClockTimeAcrossDstChange() {
        // Israel leaves summer time on Sunday 25 Oct 2026; an 08:00 alarm stays at 08:00 local.
        val now = ZonedDateTime.of(2026, 10, 24, 9, 0, 0, 0, tz)
        val next = TimeOfDayTrigger.nextOccurrence(now, Trigger.TimeOfDay(8, 0))
        assertEquals(8, next.hour)
        assertEquals(25, next.dayOfMonth)
    }
}
