package com.tuvin.pixelautomations

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.SystemClock
import android.util.Log
import com.tuvin.pixelautomations.engine.TAG
import com.google.android.gms.location.LocationServices

/**
 * Debug builds only: fake the phone's location so Arrive/Leave can be tested from the PC.
 * Needs: adb shell appops set com.tuvin.pixelautomations android:mock_location allow
 *   adb shell am broadcast -n com.tuvin.pixelautomations/.DebugLocationReceiver --ef lat 32.08 --ef lng 34.78
 *   adb shell am broadcast -n com.tuvin.pixelautomations/.DebugLocationReceiver --ez off true
 */
class DebugLocationReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(ctx: Context, intent: Intent) {
        val client = LocationServices.getFusedLocationProviderClient(ctx)
        if (intent.getBooleanExtra("off", false)) {
            client.setMockMode(false)
            Log.i(TAG, "mock location off")
            return
        }
        val loc = Location("fused").apply {
            latitude = intent.getFloatExtra("lat", 0f).toDouble()
            longitude = intent.getFloatExtra("lng", 0f).toDouble()
            accuracy = 5f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        client.setMockMode(true)
            .continueWithTask { client.setMockLocation(loc) }
            .addOnSuccessListener { Log.i(TAG, "mock location ${loc.latitude}, ${loc.longitude}") }
            .addOnFailureListener { Log.w(TAG, "mock location failed", it) }
    }
}
