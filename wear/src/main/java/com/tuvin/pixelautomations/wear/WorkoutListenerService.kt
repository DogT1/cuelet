package com.tuvin.pixelautomations.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.UserActivityInfo
import androidx.health.services.client.data.UserActivityState

/**
 * Health Services calls this when the watch's user activity changes (passive / exercise / asleep).
 * The last state is kept in preferences because the service doesn't stay alive between calls.
 */
class WorkoutListenerService : PassiveListenerService() {
    override fun onUserActivityInfoReceived(info: UserActivityInfo) {
        val prefs = getSharedPreferences("workout", MODE_PRIVATE)
        val wasExercising = prefs.getBoolean("exercising", false)
        val lastType = prefs.getString("type", null)
        val nowExercising = info.userActivityState == UserActivityState.USER_ACTIVITY_EXERCISE
        val nowType = info.exerciseInfo?.exerciseType?.name
        Log.i(TAG, "user activity ${info.userActivityState.name} type=$nowType")

        WorkoutTracker.change(wasExercising, lastType, nowExercising, nowType)?.let { c ->
            Watch.send(applicationContext, c.started, c.type)
        }
        prefs.edit().putBoolean("exercising", nowExercising).putString("type", nowType ?: lastType).apply()
    }
}

/** Health Services forgets passive registrations on reboot; register again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Watch.register(ctx)
    }
}
