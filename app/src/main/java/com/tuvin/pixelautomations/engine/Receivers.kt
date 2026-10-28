package com.tuvin.pixelautomations.engine

import com.tuvin.pixelautomations.setup.t

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tuvin.pixelautomations.app
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.triggers.ArriveLeaveTrigger
import com.tuvin.pixelautomations.triggers.CommuteTrigger
import com.tuvin.pixelautomations.triggers.Locations
import com.tuvin.pixelautomations.triggers.TimeOfDayTrigger
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.time.LocalTime

/** A time-based alarm went off (Time of Day or Before I Commute): fire, then schedule the next one. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, 0)
        launchAsync {
            val a = ctx.app.repo.get(id) ?: return@launchAsync
            when (val trig = a.trigger) {
                is Trigger.BeforeCommute -> {
                    val here = Locations.current(ctx)
                    if (CommuteTrigger.isAt(trig.origin, here)) {
                        Engine.onTriggered(ctx, id, t("Before commute to %s", trig.destination.name))
                    } else {
                        Log.i(TAG, "commute skipped: not at ${trig.origin.name}")
                        Engine.logSkip(ctx, a, t("Before commute"), t("Skipped: not at %s", trig.origin.name))
                    }
                }
                is Trigger.TimeOfDay -> Engine.onTriggered(ctx, id, t(if (trig.solar == null) "Time of Day" else if (trig.solar == com.tuvin.pixelautomations.model.SolarEvent.SUNRISE) "Sunrise" else "Sunset"))
                else -> {}
            }
            if (a.enabled) TimeOfDayTrigger.schedule(ctx, a)
        }
    }

    companion object {
        const val EXTRA_ID = "id"
    }
}

/** Play Services reports entering or leaving a geofence (iOS Arrive / Leave). */
class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.w(TAG, "geofence error ${event.errorCode}")
            return
        }
        val entered = event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
        val ids = event.triggeringGeofences.orEmpty().mapNotNull { ArriveLeaveTrigger.automationId(it.requestId) }
        val now = LocalTime.now()
        launchAsync {
            ids.forEach { id ->
                val a = ctx.app.repo.get(id) ?: return@forEach
                if (a.enabled && ArriveLeaveTrigger.matches(a.trigger, entered, now.hour * 60 + now.minute)) {
                    val place = (a.trigger as? Trigger.Arrive)?.place ?: (a.trigger as Trigger.Leave).place
                    Engine.onTriggered(ctx, id, t(if (entered) "Arrived at %s" else "Left %s", place.name))
                }
            }
        }
    }
}

/** The user tapped Run on a "Run After Confirmation" notification. */
class ConfirmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, 0)
        val why = intent.getStringExtra(EXTRA_WHY) ?: t("Confirmed")
        Notifications.cancelAsk(ctx, id)
        launchAsync {
            ctx.app.repo.get(id)?.let { Engine.run(ctx, it, t("%s (confirmed)", why), notify = false) }
        }
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_WHY = "why"
    }
}

/** A Choose from Menu button: run the chosen option's actions, then the rest of the automation. */
class MenuReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val token = intent.getIntExtra(EXTRA_TOKEN, -1)
        val option = intent.getIntExtra(EXTRA_OPTION, -1)
        Notifications.cancelMenu(ctx, token)
        val (a, index) = PendingMenus.take(token) ?: run {
            Log.w(TAG, "menu tap expired (app was restarted)")
            return
        }
        val menu = a.actions.getOrNull(index) as? com.tuvin.pixelautomations.model.Action.ChooseFromMenu ?: return
        val chosen = menu.options.getOrNull(option) ?: return
        launchAsync {
            Engine.runFrom(ctx, a, index + 1, t("Chose “%s”", chosen.label), notify = false, prefix = chosen.actions)
        }
    }

    companion object {
        const val EXTRA_TOKEN = "token"
        const val EXTRA_OPTION = "option"
    }
}

/** Reboot, app update or clock change: re-arm alarms, geofences and the service. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Engine.sync(ctx)
    }
}
