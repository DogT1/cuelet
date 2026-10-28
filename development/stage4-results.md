# Stage 4 results (2026-10-27)

Pixel 10 Pro XL, Android 17 beta, Hebrew system language. Debug build. Notification access turned on by the user
through the app's Allow button (no "restricted settings" block for this adb-installed build).

| Trigger | Test | Result |
|---|---|---|
| Message (WhatsApp) | User sent a WhatsApp message | Fired |
| Alarm Goes Off / Is Snoozed / Is Stopped | Clock alarm set from the PC; user snoozed, then dismissed the snoozed alarm | All three fired (22:43:00 / :05 / :27) |
| Do Not Disturb on / off (iOS Focus) | cmd notification set_dnd and the user's Bedtime toggle | Fired |
| Sleep: Bedtime begins / Waking up | Do Not Disturb on/off at 22:47 (inside default 21:00–09:00) | Fired after the redesign below |
| Email (Gmail) | Unit tests only | Not tried on the phone yet |
| Transaction (Wallet), Sound Recognition | Unit tests only | Need a real payment / Sound Notifications alert |
| 44 unit tests | gradlew testDebugUnitTest | Pass |

## Calibration (channel IDs, language-independent)
- Clock ringing alarm: channel Firing, category larm, **not** flagged ongoing (first guess required ongoing: missed).
- Clock snooze notice: channel Snoozed Alarms v2, posted ~0.25 s after the ringing notification goes away.
- Clock also posts Upcoming Alarms v2 reminders; ignored.
- WhatsApp: channel individual_chat_defaults_*, plus a group summary (ignored).
- Digital Wellbeing posted **no** notification for Bedtime mode: on Android 15+ Bedtime is a Mode, seen by apps
  only as Do Not Disturb turning on/off.

## Redesign: Sleep
Android doesn't tell apps which Mode turned on. Sleep now fires when Do Not Disturb / a Mode turns on (Bedtime begins)
or off (Waking up) inside user-set bedtime hours (default 21:00–09:00), and still uses Wellbeing's notification on
phones that post one. It needs no Notification access.

## Privacy
Only the watched apps' notifications are read; text is matched in memory and never stored. The run log records
generic reasons ("Message (WhatsApp)"). Debug builds log channel/category only, never text.

## Left over on the phone
Two Clock alarms named "AutomationTest" / "AutomationTest2" (22:40, 22:43; one-off, now off) can be deleted.
