package com.tuvin.pixelautomations.engine

import com.tuvin.pixelautomations.setup.t

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.tuvin.pixelautomations.BuildConfig
import com.tuvin.pixelautomations.model.AlarmEvent
import com.tuvin.pixelautomations.model.Apps
import com.tuvin.pixelautomations.model.SleepPhase
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.triggers.AlarmWatcher
import com.tuvin.pixelautomations.triggers.NotifInfo
import com.tuvin.pixelautomations.triggers.NotificationMatch
import com.tuvin.pixelautomations.triggers.SleepWatcher

/**
 * Reads notifications for the notification-based iOS triggers (Email, Message, Alarm, Sleep, Transaction,
 * Sound Recognition). Only the apps those triggers use are looked at; text is matched in memory and never stored.
 */
class NotifListener : NotificationListenerService() {

    private val alarms = AlarmWatcher()
    private val sleep = SleepWatcher()
    private val handler = Handler(Looper.getMainLooper())

    override fun onListenerConnected() {
        // Prime with what's already showing so a restart doesn't re-fire "goes off" or "bedtime".
        runCatching { activeNotifications }.getOrNull()?.forEach { sbn ->
            val n = info(sbn)
            alarms.posted(n, System.currentTimeMillis())
            sleep.posted(n)
        }
        Log.i(TAG, "notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in watched) return
        val n = info(sbn)
        spy("posted", n)
        val now = System.currentTimeMillis()

        alarms.posted(n, now)?.let(::fireAlarm)
        sleep.posted(n)?.let(::fireSleep)
        if (n.pkg in mailApps) {
            fire(t("Email (%s)", Apps.names[n.pkg] ?: n.pkg)) { t -> t is Trigger.Email && NotificationMatch.email(t, n) }
        }
        if (n.pkg in chatApps) {
            fire(t("Message (%s)", Apps.names[n.pkg] ?: n.pkg)) { t -> t is Trigger.Message && NotificationMatch.message(t, n) }
        }
        if (n.pkg == Apps.WALLET) fire(t("Wallet payment")) { t -> t is Trigger.Transaction && NotificationMatch.transaction(t, n) }
        if (n.pkg == Apps.SOUND_NOTIFICATIONS) fire(t("Sound recognised")) { t -> t is Trigger.SoundRecognition && NotificationMatch.sound(t, n) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName !in watched) return
        val n = info(sbn)
        spy("removed", n)
        if (alarms.removed(n, System.currentTimeMillis())) {
            handler.postDelayed({ alarms.settle(System.currentTimeMillis())?.let(::fireAlarm) }, 4_500)
        }
        sleep.removed(n)?.let(::fireSleep)
    }

    private fun fireAlarm(e: AlarmEvent) {
        val why = when (e) {
            AlarmEvent.GOES_OFF -> t("Alarm went off")
            AlarmEvent.SNOOZED -> t("Alarm snoozed")
            AlarmEvent.STOPPED -> t("Alarm stopped")
        }
        fire(why) { t -> t is Trigger.Alarm && t.event == e }
    }

    private fun fireSleep(p: SleepPhase) =
        fire(t(if (p == SleepPhase.BEDTIME) "Bedtime began" else "Woke up")) { t -> t is Trigger.Sleep && t.phase == p }

    private fun fire(why: String, matches: (Trigger) -> Boolean) {
        val ctx = applicationContext
        Engine.scope.launchCatching { Engine.onEvent(ctx, why) { a -> matches(a.trigger) } }
    }

    /** Debug builds: log channel/category/flags (never text) to calibrate [com.tuvin.pixelautomations.triggers.PixelSignals]. */
    private fun spy(what: String, n: NotifInfo) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "notif $what pkg=${n.pkg} channel=${n.channel} category=${n.category} ongoing=${n.ongoing} summary=${n.groupSummary}")
        }
    }

    companion object {
        private val mailApps = setOf(Apps.GMAIL, Apps.OUTLOOK, Apps.SAMSUNG_EMAIL)
        private val chatApps = setOf(Apps.MESSAGES, Apps.SAMSUNG_MESSAGES, Apps.WHATSAPP, Apps.TELEGRAM)
        private val watched = mailApps + chatApps + Apps.clockApps + Apps.wellbeingApps + setOf(Apps.WALLET, Apps.SOUND_NOTIFICATIONS)

        fun info(sbn: StatusBarNotification): NotifInfo {
            val e = sbn.notification.extras
            val messages = e.getParcelableArray(Notification.EXTRA_MESSAGES, android.os.Parcelable::class.java)
            val last = messages?.lastOrNull() as? android.os.Bundle
            return NotifInfo(
                key = sbn.key,
                pkg = sbn.packageName,
                channel = sbn.notification.channelId,
                category = sbn.notification.category,
                title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                text = (last?.getCharSequence("text") ?: e.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty(),
                bigText = e.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty(),
                sender = last?.getCharSequence("sender")?.toString(),
                ongoing = sbn.isOngoing,
                groupSummary = sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            )
        }
    }
}
