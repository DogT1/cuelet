# Plan: Pixel Automations

Follows `SPEC.md` and `CAPABILITY-MAP.md` (both approved). Status: DRAFT, waiting for review.

## Approach

Build thin vertical slices that run on the Pixel, not module by module in isolation. Slice 1 touches every layer
(model → engine → one trigger → one action → editor), so the hard parts (background reliability, permissions,
reboot) are proven on day one. Then triggers and actions are added in batches grouped by how Android delivers them.

## Phases and checkpoints

| Phase | What | Checkpoint (on the phone) |
|---|---|---|
| 0. Setup | Git repo on the NAS + local clone, Gradle/Compose skeleton, pair the Pixel, CI-free local test run | Empty app installs and opens |
| 1. Slice 1 | Model + Room, foreground engine, Time of Day + Charger triggers, Show Notification action, Run Immediately / After Confirmation / Notify, bare list + editor, run log, boot restart | A 2-minute-ahead Time of Day automation and a Charger automation both fire, also after a reboot and with the app swiped away |
| 2. System broadcasts | Battery Level, Low Power, Airplane Mode, Bluetooth, Wi-Fi, Android Auto | Each fires from a real toggle; logged |
| 3. Location | Arrive, Leave (geofences), Before I Commute, sunrise/sunset | Geofence fires on a short walk; commute fires in its window |
| 4. Notification-based | Email, Message, Alarm, Transaction, Sound Recognition, Sleep/Modes, Focus | Each fires from a real notification or mode change |
| 5. Accessibility + NFC | App Is Opened / Is Closed, NFC tags | Opening and closing a chosen app fires; a written tag fires |
| 6. Actions | Starter actions, webhook presets (NAS / Pi-hole / Home Assistant), Shizuku actions (Wi-Fi, Bluetooth, airplane, data, force-stop) | Each action runs from a test automation |
| 7. iOS-style editor + setup | Full trigger picker with iOS labels, option screens, action list editing, "Needs setup" + grant flows, restricted-settings guide, export/import | Rebuild every iOS trigger from the editor alone |
| 8. Watch | Wear OS companion on the Galaxy Watch 7: workout Starts / Ends | Starting and ending a workout on the watch fires on the phone |

## Risks

| Risk | Mitigation |
|---|---|
| Android kills or delays the background engine | Foreground service + battery-optimisation exemption; prove it in slice 1 before adding triggers |
| Sideloaded app blocked from Accessibility / Notification access | Setup screen walks through "Allow restricted settings" |
| Wi-Fi SSID hidden without location | Ask for location only when a Wi-Fi automation names a network |
| Geofence delays (minutes) | Document per trigger; offer a smaller-radius / high-accuracy option |
| Samsung Health workouts not reported on the watch | Fallback: Health Connect exercise sessions for "Ends" |
| Shizuku stops after a phone reboot | Detect, show "Shizuku not running", and fall back by opening the right settings panel |

## Parallel vs. sequential

Phases 0–1 run in order. Phases 2–6 can be done in any order once slice 1 works. 7 needs most triggers in place.
8 needs only the engine.
