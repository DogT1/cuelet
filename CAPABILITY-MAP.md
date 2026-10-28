# Capability Map: Pixel Automations

An Android app for the user's Pixel 10 Pro XL (no root) that recreates the iPhone Shortcuts **Automation** tab:
every iOS personal-automation trigger type, an iOS-style "When → Do" editor, a starter set of actions, and room
for custom triggers later.

Status: APPROVED 2026-10-27. Decisions: Shizuku included (installed on the phone), working name "Pixel Automations",
no automation apps on the phone, so the "reverse engineering" research uses Apple's published docs and reviews (see SPEC.md).

## Assumptions (correct any of these)

1. Kotlin + Jetpack Compose, one app, sideloaded from the PC (not on the Play Store). Target Android 16, minSdk 34.
2. Personal use on one phone. No accounts, no cloud, no analytics. Data stays on the phone (export/import as a JSON file).
3. Look and flow follow iOS: automation list → "New Automation" → pick a trigger → set its options →
   "Run Immediately" / "Run After Confirmation" + "Notify When Run" → add actions → done. Material-styled, not a pixel copy of Apple's UI.
4. "No root" includes **Shizuku** (a free app you start once over wireless debugging) as an *optional* extra for
   actions Android blocks for normal apps (Wi-Fi/airplane/mobile-data toggles, force-stop). Everything else works without it.
5. A sideloaded app needs Android's "Allow restricted settings" step before Accessibility / Notification access
   can be turned on. The app's setup screen will walk through it.
6. Reverse engineering is used for **research only**: inspecting the manifests/permissions of automation apps on the
   phone (e.g. MacroDroid, Tasker, tvQuickActions) to see which no-root technique each uses. No code is copied.

## Modules

| Module id | Responsibility | Depends on |
|---|---|---|
| `model` | Automation, Trigger, Action and settings data types; Room storage; JSON export/import | — |
| `engine` | Always-on foreground service, trigger registry, matching, "run immediately vs. ask", run log, notifications | `model` |
| `triggers` | One provider per iOS trigger type (table below), each with its permission needs | `engine` |
| `actions` | Action runner and the starter actions (list below); Shizuku bridge for the optional ones | `engine` |
| `setup` | Permission onboarding: per-trigger "what's needed + grant" screens, restricted-settings guide, Shizuku check | `triggers`, `actions` |
| `editor` | iOS-style UI: automation list, trigger picker, trigger options, action list editor, run log | `model`, `setup` |
| `watch` | Wear OS companion for the Galaxy Watch 7: workout start/end → phone (Data Layer) | `engine` |

Build order: `model` → `engine` → `triggers` + `actions` (in parallel, a few at a time) → `setup` → `editor`.
First usable slice: model + engine + **Time of Day** and **Charger** triggers + **Notification** action + a bare editor,
running on the phone, before widening to every trigger.

## iOS triggers → Android (no root)

Fidelity: **Full** = same behaviour as iOS. **Close** = works with a small difference. **Partial** = best effort through notifications.

| iOS trigger | Android technique | Permission / setup | Fidelity |
|---|---|---|---|
| Time of Day (time, sunrise, sunset, days) | AlarmManager exact alarms; sunrise/sunset computed from location | Alarms & reminders | Full |
| Alarm (stopped / snoozed) | Clock app's alarm notification appearing / removed | Notification access | Partial |
| Sleep (bedtime / wind down / waking) | Bedtime mode via DND/Modes changes + optional Sleep API | Notification policy access | Close |
| Email (from / subject / to) | Gmail (or other mail app) notifications | Notification access | Close |
| Message (from / contains) | Messages / WhatsApp notifications; SMS broadcast for SMS | Notification access (+ SMS optional) | Close |
| Arrive | Geofencing API | Location "Allow all the time" | Full |
| Leave | Geofencing API | Location "Allow all the time" | Full |
| Before I Commute | Time window + home/work geofence + travel time estimate | Location | Partial |
| CarPlay → **Android Auto** | Car mode (UiModeManager) / car Bluetooth connect | Bluetooth | Close |
| Airplane Mode | Airplane-mode change broadcast | — | Full |
| Wi-Fi (join / leave, SSID) | ConnectivityManager network callback | Location (needed to read SSID) | Full |
| Bluetooth (device connects / disconnects) | Bluetooth ACL broadcasts | Nearby devices | Full |
| NFC (scan a tag) | App-registered NDEF tag intent | NFC on | Full |
| App (is opened / is closed) | Accessibility window events (fallback: usage stats) | Accessibility | Full |
| Focus (on / off, which mode) | DND / Modes (Android 15+) change broadcasts | Notification policy access | Close |
| Low Power Mode | Battery Saver change broadcast | — | Full |
| Battery Level (equals / rises above / falls below) | Battery change broadcast | — | Full |
| Charger (connected / disconnected) | Power connected / disconnected broadcast | — | Full |
| Sound Recognition | Android "Sound Notifications" alerts (via notification access) | Notification access + Sound Notifications on | Partial |
| Transaction (Wallet) | Google Wallet payment notifications | Notification access | Partial |
| Stage Manager / Display | iPad only, skipped | — | n/a |

Every automation also gets the iOS options: Run Immediately / Run After Confirmation, Notify When Run.

## Starter actions (more later)

Show notification · Ask / choose from menu · Open app · Open URL · Call webhook (NAS, Pi-hole, Home Assistant) ·
Set volume / ringer · Set DND / Mode · Flashlight · Brightness · Speak text · Copy to clipboard · Wait ·
If / otherwise · Play/pause media · Launch Shortcut-style "run another automation".
Optional with Shizuku: Wi-Fi / Bluetooth / airplane / mobile data on-off, force-stop app.

## Open questions

1. App name? (Working name: "Pixel Automations".)
2. Which automation apps are on the phone for the research step (MacroDroid, Tasker, others)?
   That step needs USB or wireless debugging on the Pixel.
3. OK to use Shizuku for the optional actions, or keep the app strictly standalone?
