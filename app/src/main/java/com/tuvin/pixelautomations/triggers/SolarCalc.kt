package com.tuvin.pixelautomations.triggers

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Sunrise and sunset for a date and place, using the standard almanac method (official zenith 90.833°,
 * which includes refraction and the sun's radius). Accurate to about a minute; no network needed.
 */
object SolarCalc {
    private const val ZENITH = 90.833

    /** The event's local time on [date], or null if the sun doesn't rise/set that day (polar regions). */
    fun time(date: LocalDate, lat: Double, lng: Double, zone: ZoneId, sunrise: Boolean): ZonedDateTime? {
        val ut = utcHours(date, lat, lng, sunrise) ?: return null
        val utcMidnight = date.atStartOfDay(ZoneOffset.UTC)
        var result = utcMidnight.plusSeconds((ut * 3600).toLong()).withZoneSameInstant(zone)
        // The formula yields a time of day; keep it on the requested local date.
        if (result.toLocalDate().isAfter(date)) result = result.minusDays(1)
        if (result.toLocalDate().isBefore(date)) result = result.plusDays(1)
        return result
    }

    private fun utcHours(date: LocalDate, lat: Double, lng: Double, sunrise: Boolean): Double? {
        val n = date.dayOfYear
        val lngHour = lng / 15.0
        val t = n + ((if (sunrise) 6.0 else 18.0) - lngHour) / 24.0
        val m = 0.9856 * t - 3.289
        val l = norm360(m + 1.916 * sinD(m) + 0.020 * sinD(2 * m) + 282.634)

        var ra = norm360(atanD(0.91764 * tanD(l)))
        ra += floor(l / 90.0) * 90.0 - floor(ra / 90.0) * 90.0
        ra /= 15.0

        val sinDec = 0.39782 * sinD(l)
        val cosDec = cosD(asinD(sinDec))
        val cosH = (cosD(ZENITH) - sinDec * sinD(lat)) / (cosDec * cosD(lat))
        if (cosH > 1 || cosH < -1) return null

        val h = (if (sunrise) 360.0 - acosD(cosH) else acosD(cosH)) / 15.0
        val localT = h + ra - 0.06571 * t - 6.622
        return ((localT - lngHour) % 24 + 24) % 24
    }

    private fun norm360(x: Double) = ((x % 360) + 360) % 360
    private fun sinD(d: Double) = sin(Math.toRadians(d))
    private fun cosD(d: Double) = cos(Math.toRadians(d))
    private fun tanD(d: Double) = tan(Math.toRadians(d))
    private fun asinD(x: Double) = Math.toDegrees(asin(x))
    private fun acosD(x: Double) = Math.toDegrees(acos(x))
    private fun atanD(x: Double) = Math.toDegrees(atan(x))
}
