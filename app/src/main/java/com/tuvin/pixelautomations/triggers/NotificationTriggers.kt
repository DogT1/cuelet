package com.tuvin.pixelautomations.triggers

import com.tuvin.pixelautomations.model.AlarmEvent
import com.tuvin.pixelautomations.model.Apps
import com.tuvin.pixelautomations.model.SleepPhase
import com.tuvin.pixelautomations.model.Trigger

/**
 * The parts of a notification the triggers look at. Built by the listener and kept only in memory:
 * text is matched against the user's automations and then dropped.
 */
data class NotifInfo(
    val key: String,
    val pkg: String,
    val channel: String? = null,
    val category: String? = null,
    val title: String = "",
    val text: String = "",
    val bigText: String = "",
    /** Sender of the newest message, for chat apps using MessagingStyle. */
    val sender: String? = null,
    val ongoing: Boolean = false,
    val groupSummary: Boolean = false,
) {
    val allText: String get() = listOf(title, text, bigText).joinToString("\n")
}

/**
 * How a phone's own apps mark the notifications we watch, matched by channel ID or category so it works
 * whatever language the phone is in. Google signals calibrated on a Pixel 10 Pro XL (Android 17 beta,
 * see development/stage4-results.md); Samsung One UI signals added per-package alongside them.
 */
object PixelSignals {
    /**
     * A ringing alarm. Google Clock marks it channel "Firing" + category "alarm"; Samsung Clock posts a
     * non-ongoing category "alarm" notification. Both exclude the persistent "upcoming alarm" notice.
     */
    fun isAlarmFiring(n: NotifInfo) = !n.groupSummary && !n.ongoing && (
        (n.pkg == Apps.CLOCK && n.channel == "Firing" && n.category == "alarm") ||
            (n.pkg == Apps.SAMSUNG_CLOCK && n.category == "alarm")
    )

    /** The "snoozed" notice that follows a snooze (Google: channel "Snoozed Alarms v2"; Samsung: a snooze channel). */
    fun isSnoozeNotice(n: NotifInfo) = !n.groupSummary && (
        (n.pkg == Apps.CLOCK && n.channel?.startsWith("Snoozed Alarms") == true) ||
            (n.pkg == Apps.SAMSUNG_CLOCK && n.channel?.contains("snooze", ignoreCase = true) == true)
    )

    /** The ongoing "Bedtime mode is on" notification (Google Digital Wellbeing or Samsung's). */
    fun isBedtime(n: NotifInfo) = n.pkg in Apps.wellbeingApps &&
        (n.channel?.contains("bedtime", ignoreCase = true) == true || n.channel?.contains("sleep", ignoreCase = true) == true)
}

object NotificationMatch {
    private fun containsAny(haystack: String, needles: List<String>) =
        needles.any { it.isNotBlank() && haystack.contains(it.trim(), ignoreCase = true) }

    fun email(t: Trigger.Email, n: NotifInfo): Boolean {
        if (n.pkg !in t.apps || n.groupSummary) return false
        // Mail apps put the sender in the title and the subject in the first text line.
        val senderOk = t.senders.isEmpty() || containsAny(n.title, t.senders)
        val subjectOk = t.subjectContains.isBlank() || containsAny(n.text + "\n" + n.bigText, listOf(t.subjectContains))
        return senderOk && subjectOk
    }

    fun message(t: Trigger.Message, n: NotifInfo): Boolean {
        if (n.pkg !in t.apps || n.groupSummary) return false
        val from = n.sender ?: n.title
        val senderOk = t.senders.isEmpty() || containsAny(from, t.senders)
        val textOk = t.contains.isBlank() || containsAny(n.text + "\n" + n.bigText, listOf(t.contains))
        return senderOk && textOk
    }

    fun transaction(t: Trigger.Transaction, n: NotifInfo): Boolean =
        n.pkg == Apps.WALLET && !n.groupSummary && (t.merchantContains.isBlank() || containsAny(n.allText, listOf(t.merchantContains)))

    fun sound(t: Trigger.SoundRecognition, n: NotifInfo): Boolean =
        n.pkg == Apps.SOUND_NOTIFICATIONS && !n.groupSummary && (t.sounds.isEmpty() || containsAny(n.allText, t.sounds))
}

/**
 * Turns Clock notifications into iOS alarm events.
 * - Ringing notification appears → GOES_OFF.
 * - It disappears and a "snoozed" notice follows within the window → SNOOZED.
 * - A ringing or snoozed alarm goes away with nothing following (no snooze notice, no re-ring) → STOPPED.
 *   That covers stopping a ringing alarm and dismissing a snoozed one.
 */
class AlarmWatcher(private val snoozeWindowMs: Long = 4_000) {
    private val ringing = mutableSetOf<String>()
    private val snoozed = mutableSetOf<String>()
    private var goneAt: Long? = null

    fun posted(n: NotifInfo, now: Long): AlarmEvent? = when {
        PixelSignals.isAlarmFiring(n) -> {
            goneAt = null // a snoozed alarm ringing again isn't a stop
            if (ringing.add(n.key)) AlarmEvent.GOES_OFF else null
        }
        PixelSignals.isSnoozeNotice(n) -> {
            val afterRing = goneAt?.let { now - it <= snoozeWindowMs } == true
            if (snoozed.add(n.key) && afterRing) {
                goneAt = null
                AlarmEvent.SNOOZED
            } else {
                null
            }
        }
        else -> null
    }

    /** True when a ringing or snoozed alarm went away; call [settle] after [snoozeWindowMs] to learn the outcome. */
    fun removed(n: NotifInfo, now: Long): Boolean {
        if (!ringing.remove(n.key) && !snoozed.remove(n.key)) return false
        goneAt = now
        return true
    }

    /** STOPPED if nothing followed within the window; null otherwise (snooze or re-ring already handled). */
    fun settle(now: Long): AlarmEvent? {
        val at = goneAt ?: return null
        if (now - at < snoozeWindowMs) return null
        goneAt = null
        return AlarmEvent.STOPPED
    }
}

object SleepRules {
    /** A Do Not Disturb / Mode change counts as Bedtime begins (on) or Waking up (off) inside the bedtime hours. */
    fun matchesModeChange(t: Trigger.Sleep, modeOn: Boolean, minuteOfDay: Int): Boolean =
        t.phase == (if (modeOn) SleepPhase.BEDTIME else SleepPhase.WAKING_UP) && t.hours.contains(minuteOfDay)
}

/** Bedtime mode notification appearing = Bedtime begins; going away = waking up (phones that still post one). */
class SleepWatcher {
    private val active = mutableSetOf<String>()

    fun posted(n: NotifInfo): SleepPhase? =
        if (PixelSignals.isBedtime(n) && active.add(n.key)) SleepPhase.BEDTIME else null

    fun removed(n: NotifInfo): SleepPhase? =
        if (active.remove(n.key)) SleepPhase.WAKING_UP else null
}
