package com.tuvin.pixelautomations.triggers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.tuvin.pixelautomations.engine.GeofenceReceiver
import com.tuvin.pixelautomations.engine.TAG
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.Place
import com.tuvin.pixelautomations.model.TimeWindow
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.setup.Permissions
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** iOS "Arrive" / "Leave": one Play Services geofence per enabled automation. */
object ArriveLeaveTrigger {

    fun geofenceId(automationId: Long) = "a$automationId"
    fun automationId(geofenceId: String): Long? = geofenceId.removePrefix("a").toLongOrNull()

    /** Whether an enter/exit event for [t] counts at [minuteOfDay] (the optional iOS time range). */
    fun matches(t: Trigger, entered: Boolean, minuteOfDay: Int): Boolean {
        val window: TimeWindow? = when (t) {
            is Trigger.Arrive -> if (entered) t.window else return false
            is Trigger.Leave -> if (!entered) t.window else return false
            else -> return false
        }
        return window?.contains(minuteOfDay) ?: true
    }

    /** Replace all registered geofences with the enabled Arrive/Leave automations. */
    @SuppressLint("MissingPermission")
    fun sync(ctx: Context, all: List<Automation>) {
        val client = LocationServices.getGeofencingClient(ctx)
        val pi = pending(ctx)
        val fences = all.filter { it.enabled && (it.trigger is Trigger.Arrive || it.trigger is Trigger.Leave) }.map { a ->
            val place = when (val t = a.trigger) {
                is Trigger.Arrive -> t.place
                is Trigger.Leave -> t.place
                else -> error("unreachable")
            }
            Geofence.Builder()
                .setRequestId(geofenceId(a.id))
                .setCircularRegion(place.lat, place.lng, place.radius.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }
        client.removeGeofences(pi).addOnCompleteListener {
            if (fences.isEmpty()) return@addOnCompleteListener
            if (!Permissions.granted(ctx, Permissions.FINE_LOCATION) || !Permissions.granted(ctx, Permissions.BACKGROUND_LOCATION)) {
                Log.w(TAG, "geofences skipped: location permission missing")
                return@addOnCompleteListener
            }
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(0) // don't fire for where the phone already is
                .addGeofences(fences)
                .build()
            client.addGeofences(request, pi)
                .addOnSuccessListener { Log.i(TAG, "geofences registered: ${fences.size}") }
                .addOnFailureListener { Log.w(TAG, "geofences failed", it) }
        }
    }

    private fun pending(ctx: Context) = PendingIntent.getBroadcast(
        ctx, 0, Intent(ctx, GeofenceReceiver::class.java),
        // Play Services fills in the event, so this one must be mutable.
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
}

/** "Before I Commute" runs only if the phone is at the starting place when the alarm goes off. */
object CommuteTrigger {
    fun isAt(place: Place, here: Location?): Boolean {
        if (here == null) return false
        val d = FloatArray(1)
        Location.distanceBetween(here.latitude, here.longitude, place.lat, place.lng, d)
        // Allow for GPS error on top of the place's radius.
        return d[0] <= place.radius + maxOf(here.accuracy, 50f)
    }
}

object Locations {
    /** A fresh location fix (up to [timeoutMs]), falling back to the last known one; null without permission. */
    @SuppressLint("MissingPermission")
    suspend fun current(ctx: Context, timeoutMs: Long = 20_000): Location? {
        if (!Permissions.granted(ctx, Permissions.FINE_LOCATION)) return null
        val client = LocationServices.getFusedLocationProviderClient(ctx)
        val cts = CancellationTokenSource()
        val fresh = withTimeoutOrNull(timeoutMs) {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
        }
        if (fresh == null) cts.cancel()
        return fresh ?: client.lastLocation.await()
    }
}

/** Minimal Task → suspend bridge (avoids pulling in another library). */
suspend fun <T> Task<T>.await(): T? = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { t -> cont.resume(if (t.isSuccessful) t.result else null) }
}
