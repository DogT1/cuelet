package com.tuvin.pixelautomations.triggers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tuvin.pixelautomations.engine.AlarmReceiver
import com.tuvin.pixelautomations.engine.TAG
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.SolarEvent
import com.tuvin.pixelautomations.model.Trigger
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Exact alarms for the time-based triggers: iOS "Time of Day" (fixed time or sunrise/sunset) and
 * "Before I Commute". One alarm per automation, re-armed after each run.
 */
object TimeOfDayTrigger {

    fun usesAlarm(t: Trigger) = t is Trigger.TimeOfDay || t is Trigger.BeforeCommute

    /** The next fire time strictly after [now], or null if none in the next 400 days (e.g. polar night). */
    fun nextFire(now: ZonedDateTime, t: Trigger): ZonedDateTime? = when (t) {
        is Trigger.TimeOfDay -> if (t.solar == null) nextOccurrence(now, t) else nextSolar(now, t)
        is Trigger.BeforeCommute -> nextCommute(now, t)
        else -> null
    }

    /** Fixed-time occurrence (kept for the stage 1 tests and callers). */
    fun nextOccurrence(now: ZonedDateTime, t: Trigger.TimeOfDay): ZonedDateTime =
        nextDaily(now, t.days) { day -> ZonedDateTime.of(day, LocalTime.of(t.hour, t.minute), now.zone) }
            ?: error("no occurrence for $t")

    private fun nextSolar(now: ZonedDateTime, t: Trigger.TimeOfDay): ZonedDateTime? {
        val lat = t.lat ?: return null
        val lng = t.lng ?: return null
        return nextDaily(now, t.days, searchDays = 400) { day ->
            SolarCalc.time(day, lat, lng, now.zone, sunrise = t.solar == SolarEvent.SUNRISE)
                ?.plusMinutes(t.offsetMinutes.toLong())
        }
    }

    private fun nextCommute(now: ZonedDateTime, t: Trigger.BeforeCommute): ZonedDateTime? =
        // [days] are departure days; the alarm runs minutesBefore earlier (possibly the evening before).
        nextDaily(now.minusDays(1), t.days, searchDays = 9) { day ->
            ZonedDateTime.of(day, LocalTime.of(t.departHour, t.departMinute), now.zone)
                .minusMinutes(t.minutesBefore.toLong())
                .takeIf { it.isAfter(now) }
        }

    /** First candidate after [from] on an allowed day, checking day by day. */
    private fun nextDaily(
        from: ZonedDateTime,
        days: Set<Int>,
        searchDays: Int = 8,
        candidate: (java.time.LocalDate) -> ZonedDateTime?,
    ): ZonedDateTime? {
        var day = from.toLocalDate()
        repeat(searchDays) {
            if (days.isEmpty() || day.dayOfWeek.value in days) {
                val c = candidate(day)
                if (c != null && c.isAfter(from)) return c
            }
            day = day.plusDays(1)
        }
        return null
    }

    fun schedule(ctx: Context, a: Automation, now: ZonedDateTime = ZonedDateTime.now()) {
        val at = nextFire(now, a.trigger) ?: return cancel(ctx, a.id)
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), pending(ctx, a.id))
        Log.i(TAG, "alarm for ${a.title} at $at")
    }

    fun cancel(ctx: Context, id: Long) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx, id))
    }

    fun syncAlarms(ctx: Context, all: List<Automation>) {
        all.forEach { a ->
            if (a.enabled && usesAlarm(a.trigger)) schedule(ctx, a) else cancel(ctx, a.id)
        }
    }

    private fun pending(ctx: Context, id: Long) = PendingIntent.getBroadcast(
        ctx, id.toInt(),
        Intent(ctx, AlarmReceiver::class.java).putExtra(AlarmReceiver.EXTRA_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
