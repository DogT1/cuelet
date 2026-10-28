package com.tuvin.pixelautomations.wear

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.PassiveListenerConfig
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.Executors

const val TAG = "AutomationsWatch"

/** Same message format as the phone's WorkoutTrigger: "start|RUNNING" / "end|RUNNING" on [PATH]. */
object WorkoutMessages {
    const val PATH = "/automations/workout"
    fun encode(started: Boolean, type: String): ByteArray = ((if (started) "start" else "end") + "|" + type).toByteArray()
}

/**
 * Turns Health Services user-activity updates into workout start/end. Only the change matters:
 * passive → exercise is a start, exercise → anything else is an end (with the type that was running).
 */
object WorkoutTracker {
    data class Change(val started: Boolean, val type: String)

    fun change(wasExercising: Boolean, lastType: String?, nowExercising: Boolean, nowType: String?): Change? = when {
        nowExercising && !wasExercising -> Change(true, nowType ?: "WORKOUT")
        !nowExercising && wasExercising -> Change(false, lastType ?: "WORKOUT")
        else -> null
    }
}

object Watch {
    private val io = Executors.newSingleThreadExecutor()

    /** Ask Health Services to call [WorkoutListenerService] with user-activity changes (survives until reboot). */
    fun register(ctx: Context, done: (Boolean, String) -> Unit = { _, _ -> }) {
        val config = PassiveListenerConfig.builder().setShouldUserActivityInfoBeRequested(true).build()
        val future = HealthServices.getClient(ctx).passiveMonitoringClient
            .setPassiveListenerServiceAsync(WorkoutListenerService::class.java, config)
        future.addListener({
            val result = runCatching { future.get() }
            Log.i(TAG, "passive listener registered: ${result.isSuccess} ${result.exceptionOrNull()?.message.orEmpty()}")
            done(result.isSuccess, result.exceptionOrNull()?.message.orEmpty())
        }, io)
    }

    /** Send a workout change to every connected phone running the app. */
    fun send(ctx: Context, started: Boolean, type: String, done: (Int) -> Unit = {}) = io.execute {
        val sent = runCatching {
            val nodes = Tasks.await(Wearable.getNodeClient(ctx).connectedNodes)
            nodes.forEach { node ->
                Tasks.await(Wearable.getMessageClient(ctx).sendMessage(node.id, WorkoutMessages.PATH, WorkoutMessages.encode(started, type)))
            }
            nodes.size
        }.onFailure { Log.w(TAG, "send failed", it) }.getOrDefault(0)
        Log.i(TAG, "sent ${if (started) "start" else "end"} $type to $sent phone(s)")
        done(sent)
    }
}
