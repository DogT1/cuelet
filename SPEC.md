# Spec: Pixel Automations

Status: APPROVED 2026-10-27 (with the decisions at the end). Modules and build order: see `CAPABILITY-MAP.md` (approved).

## Objective

The iPhone Shortcuts **Automation** tab on the user's Pixel 10 Pro XL, without root. The user creates a personal
automation the iOS way: **New Automation → pick a trigger → set its options → Run Immediately / Run After Confirmation
(+ Notify When Run) → add actions → Done**, and it fires reliably in the background. Phase 1 covers every iOS trigger
type; custom triggers and more actions come later.

### Research: what the iOS Automation tab offers

Sources: Apple's current Shortcuts User Guide ("Add automations", which lists the 20 trigger types for iOS 26/27) and
MacStories' full trigger list. Apple has since taken down its per-trigger detail pages, so options marked ⚠ come
from knowledge of iOS 17/18. Check them against an iPhone if you have one.

| # | iOS trigger | Options to reproduce |
|---|---|---|
| 1 | Time of Day | Time of Day / Sunrise / Sunset; offset ⚠ (at, 15/30/45 min, 1 h before or after); Repeat: Daily / Weekly (days) / Monthly (day) ⚠ |
| 2 | Alarm | Is Stopped / Is Snoozed (iOS 13 also had "goes off"); Any alarm / a specific one |
| 3 | Sleep | Wind Down Begins / Bedtime Begins / Waking Up ⚠ |
| 4 | Arrive | Location + radius; Any Time / Time Range |
| 5 | Leave | Location + radius; Any Time / Time Range |
| 6 | Before I Commute | Home or Work; at predicted time or 5/10/15/30/60 min before (MacStories: "Before I Leave") |
| 7 | CarPlay → Android Auto | Connects / Disconnects |
| 8 | Email | Sender(s), Subject Contains, Account, Recipient ⚠ |
| 9 | Message | Sender(s), Message Contains ⚠ |
| 10 | Transaction | Card(s), Merchant, Category ⚠ |
| 11 | Wi-Fi | Network(s) or Any; Joins / Leaves ⚠ |
| 12 | Bluetooth | Device(s); Is Connected / Is Disconnected ⚠ |
| 13 | Apple Watch Workout → **Galaxy Watch 7 workout** | Workout type(s); Starts / Ends ⚠. Small Wear OS companion app using Health Services passive monitoring (user activity state = exercise) sends start/end to the phone; Health Connect exercise sessions as a fallback for "Ends" (delayed until Samsung Health syncs) |
| 14 | NFC | A tag scanned and named in the app |
| 15 | App | App(s); Is Opened / Is Closed (Apple guide: opened = "open or switch to"; closed = "close or switch away") |
| 16 | Airplane Mode | Is Turned On / Is Turned Off |
| 17 | Do Not Disturb / Focus → Android Modes | Mode; When Turning On / When Turning Off ⚠ |
| 18 | Low Power Mode → Battery Saver | Is Turned On / Is Turned Off |
| 19 | Battery Level | Equals / Rises Above / Falls Below + percent slider ⚠ |
| 20 | Charger | Is Connected / Is Disconnected |
| 21 | Sound Recognition | Sound type (smoke/fire alarm, siren, doorbell, knock, baby crying, dog, cat, water running, appliance…) |

Automation-wide options: **Run After Confirmation** (default, a notification you tap to run) or **Run Immediately**;
**Notify When Run** (only with Run Immediately). Each automation can be switched on or off in the list.
How the Android triggers work: see the mapping table in `CAPABILITY-MAP.md`.

### Acceptance criteria (phase 1)

- Every trigger above (13 is optional) can be created in the editor with the options listed, saved, and fires on the Pixel.
- Run After Confirmation shows a notification with a Run button; Run Immediately runs without asking and honours Notify When Run.
- Automations keep firing after a reboot, after the app is swiped away, and with the screen off (within Android's
  limits; known delays are written in the app's help per trigger).
- A trigger whose permission is missing shows as "Needs setup" in the list and links straight to the grant screen.
- Each run is logged (time, trigger, result, error) and visible in the app.
- Export and import of all automations as one JSON file.

## Tech Stack

- Kotlin 2.x, Jetpack Compose (Material 3), Android Gradle Plugin 8.x, JDK 17 (already on the PC)
- compileSdk/targetSdk 36 (Android 16), minSdk 34
- Room (storage), kotlinx.serialization (JSON), WorkManager (deferred work), Play Services Location (geofences)
- Shizuku API (`dev.rikka.shizuku:api`) for the optional privileged actions
- Tests: JUnit 5 / kotlin.test, Robolectric for Android-dependent units, Compose UI tests for the editor

## Commands

Run from the project folder on a local copy (Gradle must not build on the NAS share):

```
Build:     .\gradlew.bat assembleDebug
Unit test: .\gradlew.bat testDebugUnitTest
Lint:      .\gradlew.bat lintDebug
Install:   adb install -r app\build\outputs\apk\debug\app-debug.apk
UI test:   .\gradlew.bat connectedDebugAndroidTest   (phone attached)
Logs:      adb logcat -s PixelAutomations
```

## Project Structure

```
app/src/main/java/.../model/      Automation, Trigger*, Action*, RunMode; Room DAO; JSON codec
app/src/main/java/.../engine/     AutomationService (foreground), TriggerRegistry, Runner, RunLog, confirmation notifications
app/src/main/java/.../triggers/   one file per trigger provider (TimeOfDayTrigger.kt, ChargerTrigger.kt, …)
app/src/main/java/.../actions/    one file per action; shizuku/ for privileged ones
app/src/main/java/.../setup/      permission checks and grant screens
app/src/main/java/.../editor/     Compose screens: list, trigger picker, trigger options, actions, log
app/src/test/                     unit tests (model, matching, codecs, scheduling math)
app/src/androidTest/              editor UI tests, on-device trigger smoke tests
docs/                             per-trigger notes: Android technique, permission, known limits
```

Source of truth lives on the NAS (`Z:\Claude Code\Projects\pixel-automations`) under git; builds run from a local
clone (`C:\Users\danie\pixel-automations`) and are pushed back.

## Code Style

```kotlin
/** iOS "Battery Level": fires once each time the level crosses the threshold in the chosen direction. */
class BatteryLevelTrigger(
    private val condition: Condition,   // EQUALS, RISES_ABOVE, FALLS_BELOW
    private val percent: Int,
) : TriggerProvider {
    override val needs = emptySet<Permission>()

    override fun matches(previous: Int?, current: Int): Boolean = when (condition) {
        Condition.EQUALS -> current == percent && previous != percent
        Condition.RISES_ABOVE -> previous != null && previous <= percent && current > percent
        Condition.FALLS_BELOW -> previous != null && previous >= percent && current < percent
    }
}
```

One trigger or action per file, named after the iOS label. Pure matching logic kept apart from Android callbacks
so it can be unit-tested. User-facing labels match iOS wording where Android has the same concept.

## Testing Strategy

- Unit tests for each trigger's matching logic, time and sunrise/sunset maths, run-mode rules and JSON round-trips.
  Every trigger file has a test file.
- Robolectric for broadcast parsing (battery, power, airplane, Bluetooth).
- On-device smoke test per trigger: fire it for real (or with `adb shell` broadcasts and `cmd` commands where Android
  allows) and check the run log.
- Editor: Compose UI tests for create, edit, delete and enable/disable.

## Boundaries

- **Always:** keep everything on the phone; ask for a permission only when an automation needs it; log every run;
  run unit tests before each commit; build from the local clone.
- **Ask first:** adding a dependency not listed above; any network call other than a user-made webhook action;
  anything that needs Shizuku for a *trigger* (not just an action); publishing anywhere.
- **Never:** root, analytics or telemetry, copying code or assets from Apple or other apps, storing secrets in the
  repo, keeping Accessibility events or notification text beyond what a trigger needs to match.

## Success Criteria

1. Slice 1 (first build on the phone): Time of Day + Charger triggers, Show Notification action, both run modes,
   list/editor, run log, survives a reboot.
2. Phase 1 done: all 20 triggers (Workout optional) pass their on-device smoke test, with fidelity as in `CAPABILITY-MAP.md`.
3. The user can rebuild any of their iPhone automations from the iOS trigger list without workarounds, except the
   triggers marked Partial.
4. Battery: idle drain from the background service under 1% per day, measured with Battery Usage over 24 h.

## Decisions (2026-10-27)

1. ⚠ options stay as researched. The user has no iPhone to compare against, so they can be adjusted later in use.
2. Workout trigger: Galaxy Watch 7 (Wear OS) through a companion watch module, built after the phone triggers.
3. Webhook action ships with presets for the NAS, Pi-hole (192.168.1.231:20720) and Home Assistant. Tokens are
   entered on the phone and never stored in the repo.
4. Pixel: wireless debugging on 192.168.1.204, paired with this PC. The phone runs the **Android 17 beta** (SDK 37); the app targets 36 and is tested on 17.

