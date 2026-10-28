package com.tuvin.pixelautomations

import com.tuvin.pixelautomations.model.AlarmEvent
import com.tuvin.pixelautomations.model.Apps
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.AutomationJson
import com.tuvin.pixelautomations.model.SleepPhase
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.model.label
import com.tuvin.pixelautomations.triggers.AlarmWatcher
import com.tuvin.pixelautomations.triggers.NotifInfo
import com.tuvin.pixelautomations.triggers.NotificationMatch
import com.tuvin.pixelautomations.triggers.SleepRules
import com.tuvin.pixelautomations.triggers.SleepWatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StageFourTriggersTest {

    private fun gmail(from: String, subject: String, summary: Boolean = false) =
        NotifInfo("k", Apps.GMAIL, title = from, text = subject, groupSummary = summary)

    @Test fun emailMatchesSenderAndSubject() {
        val t = Trigger.Email(senders = listOf("Dana", "bank@"), subjectContains = "invoice")
        assertTrue(NotificationMatch.email(t, gmail("Dana Levi", "Your Invoice for October")))
        assertFalse(NotificationMatch.email(t, gmail("Dana Levi", "Lunch?")))
        assertFalse(NotificationMatch.email(t, gmail("Someone", "invoice")))
        assertFalse(NotificationMatch.email(t, gmail("Dana", "invoice", summary = true)))
        assertTrue(NotificationMatch.email(Trigger.Email(), gmail("Anyone", "Anything")))
        assertFalse(NotificationMatch.email(Trigger.Email(apps = listOf(Apps.OUTLOOK)), gmail("A", "B")))
    }

    @Test fun messageUsesNewestSenderInGroupChats() {
        val group = NotifInfo("k", Apps.WHATSAPP, title = "Family", text = "dinner at 8", sender = "Mom")
        assertTrue(NotificationMatch.message(Trigger.Message(senders = listOf("mom")), group))
        assertTrue(NotificationMatch.message(Trigger.Message(contains = "DINNER"), group))
        assertFalse(NotificationMatch.message(Trigger.Message(senders = listOf("Dad")), group))
        assertFalse(NotificationMatch.message(Trigger.Message(apps = listOf(Apps.MESSAGES)), group))
    }

    @Test fun walletAndSoundMatching() {
        val pay = NotifInfo("k", Apps.WALLET, title = "₪42.00 at Aroma Espresso Bar", text = "Visa ••1234")
        assertTrue(NotificationMatch.transaction(Trigger.Transaction("aroma"), pay))
        assertFalse(NotificationMatch.transaction(Trigger.Transaction("shufersal"), pay))
        assertTrue(NotificationMatch.transaction(Trigger.Transaction(), pay))
        val bell = NotifInfo("k", Apps.SOUND_NOTIFICATIONS, title = "Doorbell", text = "Possible doorbell sound")
        assertTrue(NotificationMatch.sound(Trigger.SoundRecognition(listOf("doorbell")), bell))
        assertFalse(NotificationMatch.sound(Trigger.SoundRecognition(listOf("smoke")), bell))
    }

    // Channels and categories as seen on the Pixel 10 Pro XL Clock app (2026-10-27).
    private val firing = NotifInfo("alarm1", Apps.CLOCK, channel = "Firing", category = "alarm")
    private val snoozeNotice = NotifInfo("snooze1", Apps.CLOCK, channel = "Snoozed Alarms v2")

    @Test fun alarmGoesOffOnceThenStops() {
        val w = AlarmWatcher(snoozeWindowMs = 4_000)
        assertEquals(AlarmEvent.GOES_OFF, w.posted(firing, 0))
        assertNull(w.posted(firing, 500)) // same alarm updated
        assertTrue(w.removed(firing, 10_000))
        assertNull(w.settle(12_000)) // still inside the snooze window
        assertEquals(AlarmEvent.STOPPED, w.settle(14_100))
        assertNull(w.settle(20_000))
    }

    @Test fun alarmSnoozeIsRecognisedAndNotReportedAsStopped() {
        val w = AlarmWatcher(snoozeWindowMs = 4_000)
        w.posted(firing, 0)
        w.removed(firing, 10_000)
        assertEquals(AlarmEvent.SNOOZED, w.posted(snoozeNotice, 10_300))
        assertNull(w.settle(15_000))
    }

    @Test fun dismissingASnoozedAlarmIsAStop() {
        val w = AlarmWatcher(snoozeWindowMs = 4_000)
        w.posted(firing, 0)
        w.removed(firing, 4_000)
        assertEquals(AlarmEvent.SNOOZED, w.posted(snoozeNotice, 4_250))
        assertTrue(w.removed(snoozeNotice, 13_000)) // user dismisses it from the shade
        assertEquals(AlarmEvent.STOPPED, w.settle(17_100))
    }

    @Test fun snoozedAlarmRingingAgainIsNotAStop() {
        val w = AlarmWatcher(snoozeWindowMs = 4_000)
        w.posted(firing, 0)
        w.removed(firing, 1_000)
        w.posted(snoozeNotice, 1_250)
        w.removed(snoozeNotice, 600_000) // snooze over: Clock swaps the notice for a ringing one
        assertEquals(AlarmEvent.GOES_OFF, w.posted(firing, 600_100))
        assertNull(w.settle(605_000))
    }

    @Test fun otherClockNotificationsAreIgnored() {
        val w = AlarmWatcher()
        assertNull(w.posted(NotifInfo("u", Apps.CLOCK, channel = "Upcoming Alarms v2"), 0))
        assertNull(w.posted(NotifInfo("s", Apps.CLOCK, channel = "Firing", category = "alarm", groupSummary = true), 0))
        assertFalse(w.removed(NotifInfo("u", Apps.CLOCK, channel = "Upcoming Alarms v2"), 0))
    }

    @Test fun bedtimeBeginsAndEnds() {
        val w = SleepWatcher()
        val bedtime = NotifInfo("b", Apps.WELLBEING, channel = "bedtime_mode", ongoing = true)
        assertEquals(SleepPhase.BEDTIME, w.posted(bedtime))
        assertNull(w.posted(bedtime))
        assertEquals(SleepPhase.WAKING_UP, w.removed(bedtime))
        assertNull(w.removed(bedtime))
        assertNull(w.posted(NotifInfo("o", Apps.WELLBEING, channel = "app_timer")))
    }

    @Test fun sleepFromModeChangesInsideBedtimeHours() {
        val bedtime = Trigger.Sleep(SleepPhase.BEDTIME) // default 21:00–09:00
        val waking = Trigger.Sleep(SleepPhase.WAKING_UP)
        assertTrue(SleepRules.matchesModeChange(bedtime, modeOn = true, minuteOfDay = 22 * 60 + 44))
        assertFalse(SleepRules.matchesModeChange(bedtime, modeOn = true, minuteOfDay = 14 * 60)) // afternoon DND
        assertFalse(SleepRules.matchesModeChange(bedtime, modeOn = false, minuteOfDay = 23 * 60))
        assertTrue(SleepRules.matchesModeChange(waking, modeOn = false, minuteOfDay = 7 * 60))
    }

    @Test fun jsonAndLabels() {
        val triggers = listOf(
            Trigger.Email(listOf("Dana"), "invoice"), Trigger.Message(contains = "hi"), Trigger.Alarm(AlarmEvent.SNOOZED),
            Trigger.Sleep(SleepPhase.WAKING_UP), Trigger.Focus(false), Trigger.Transaction("Aroma"),
            Trigger.SoundRecognition(listOf("doorbell")),
        )
        triggers.forEach { t ->
            val a = Automation(id = 1, trigger = t)
            assertEquals(a, AutomationJson.decode(AutomationJson.encode(a)))
        }
        assertEquals("When I get an email from Dana subject has “invoice”", triggers[0].label())
        assertEquals("When an alarm is snoozed", triggers[2].label())
        assertEquals("When Do Not Disturb turns off", triggers[4].label())
    }
}
