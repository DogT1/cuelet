package com.tuvin.pixelautomations.engine

import android.util.Log
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.model.workoutName
import com.tuvin.pixelautomations.setup.t
import com.tuvin.pixelautomations.triggers.WorkoutTrigger
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/** Receives workout start/end from the companion app on the Galaxy Watch (Wear OS Data Layer). */
class WatchListener : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WorkoutTrigger.PATH) return
        val e = WorkoutTrigger.decode(event.data) ?: return
        Log.i(TAG, "watch workout ${if (e.started) "start" else "end"} ${e.type}")
        val ctx = applicationContext
        val why = t(if (e.started) "%s started on the watch" else "%s ended on the watch", t(workoutName(e.type)))
        Engine.scope.launchCatching {
            Engine.onEvent(ctx, why) { a -> (a.trigger as? Trigger.Workout)?.let { WorkoutTrigger.matches(it, e) } == true }
        }
    }
}
