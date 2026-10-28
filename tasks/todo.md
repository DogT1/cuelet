# Tasks: Phases 0–1 (first build on the phone)

Later phases get their task lists when we reach them.

- [x] T0.1 Repo and skeleton
  - Acceptance: git repo in `Z:\Claude Code\Projects\pixel-automations`, local clone `C:\Users\danie\pixel-automations`,
    Compose app `com.tuvin.pixelautomations` builds; `.gitignore` covers build output, keystores, local.properties
  - Verify: `.\gradlew.bat assembleDebug testDebugUnitTest` passes
  - Files: settings.gradle.kts, build.gradle.kts, app/build.gradle.kts, MainActivity.kt, .gitignore

- [x] T0.2 Pair and install
  - Acceptance: Pixel paired over wireless debugging, debug APK installed and opens
  - Verify: `adb devices` lists the Pixel; app visible on the phone

- [x] T1.1 Data model + storage
  - Acceptance: Automation(id, name, enabled, trigger, actions, runMode, notifyWhenRun), sealed Trigger/Action types,
    Room DAO, JSON codec with round-trip
  - Verify: unit tests for DAO (Robolectric) and JSON round-trip
  - Files: model/*.kt, model tests

- [x] T1.2 Engine
  - Acceptance: foreground service starts on app open and on boot; TriggerRegistry registers providers for enabled
    automations; Runner executes actions; Run After Confirmation posts a notification with Run; Notify When Run honoured;
    run log stored
  - Verify: unit tests for run-mode rules; on device: service notification visible after reboot
  - Files: engine/*.kt, AndroidManifest.xml

- [x] T1.3 Time of Day trigger (fixed time + days; sunrise/sunset later in phase 3)
  - Acceptance: exact alarm scheduled for the next occurrence, rescheduled after firing, reboot and time-zone change
  - Verify: unit tests for next-occurrence maths; on device: automation 2 minutes ahead fires
  - Files: triggers/TimeOfDayTrigger.kt + test

- [x] T1.4 Charger trigger
  - Acceptance: Is Connected / Is Disconnected fire once per change
  - Verify: unit test; on device: plug and unplug
  - Files: triggers/ChargerTrigger.kt + test

- [x] T1.5 Show Notification action
  - Acceptance: title and body, posted on its own channel
  - Verify: on device via both triggers
  - Files: actions/ShowNotificationAction.kt

- [x] T1.6 Bare editor + run log
  - Acceptance: list with on/off switches, New Automation → pick Time of Day or Charger → options → run mode → one
    notification action → Done; run log screen
  - Verify: Compose UI test for create and toggle; manual on the phone
  - Files: editor/*.kt

- [x] T1.7 Slice 1 checkpoint
  - Verify on the phone: both automations fire with the app swiped away, after a reboot, with the screen off;
    battery-optimisation exemption requested; results written to `docs/slice1-results.md`


# Stage 2 (done 2026-10-27)

- [x] Battery Level, Battery Saver, Airplane Mode, Bluetooth, Wi-Fi, Android Auto triggers + editor options
- [x] Permission checks with Needs setup and Allow flow
- [x] Device tests: see docs/stage2-results.md (Android Auto unit-tested only)

# Stage 3 (done 2026-10-27)

- [x] Arrive / Leave (geofences, time range), Before I Commute, sunrise/sunset with iOS offsets
- [x] Place picker (current location, address search, radius)
- [x] Fake-location tests on the phone: see docs/stage3-results.md
- [ ] Real-world Arrive/Leave and sunset check (user, when out)

# Stage 4 (done 2026-10-27)

- [x] Notification listener; Email, Message, Alarm, Transaction, Sound Recognition; Focus via DND; Sleep via Modes + bedtime hours
- [x] Calibrated Clock channel IDs on the phone; see docs/stage4-results.md
- [ ] Real Email, Wallet payment and Sound Notifications checks (user)

# Stage 5 (done 2026-10-27)

- [x] App Is Opened / Is Closed (Accessibility, window changes only), NFC tags (scan, optional write)
- [x] Device tests: see docs/stage5-results.md
- [x] Real NFC tag test (Rav-Kav, 2026-10-27)

# Stage 6 (done 2026-10-27)

- [x] 19 actions incl. webhook, Pi-hole, Home Assistant, Shizuku switches (with settings-panel fallback)
- [x] Connections screen; action list editor; device tests in docs/stage6-results.md
- [ ] Shizuku on Android 17 (blocked upstream, issue #2180)
- [ ] If / Otherwise, Choose from Menu, Run Now (stage 7)

# Stage 7 (done 2026-10-27)

- [x] iOS trigger list, Run Now, If/Otherwise, Choose from Menu, dial time picker, export/import
- [x] Hebrew UI + Settings language switch
- [ ] User to try Run Now, time picker, export/import

# Stage 8 (done 2026-10-28)

- [x] Galaxy Watch companion (Health Services) + phone Workout trigger; tested with a real Samsung Health run

# Stage 9 (done 2026-10-28)

- [x] Anti-theft actions: Send Location, Capture Photo (front/back), Record Audio -> NAS upload URL
- [x] Added iOS-style actions: Lock Screen, Take Screenshot, Set Alarm, Start Timer
- [ ] User: test camera capture while the phone is locked
- [ ] User: stand up a NAS upload endpoint and set it in Connections
