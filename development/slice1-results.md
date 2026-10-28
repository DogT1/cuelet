# Stage 1 results (2026-10-27)

Phone: Pixel 10 Pro XL, Android 17 beta (SDK 37), Hebrew system language. Debug build over wireless debugging.

| Check | Result |
|---|---|
| 13 unit tests (time maths incl. DST, charger matching, run rules, JSON, Room) | Pass |
| Time of Day, app in background, Run Immediately + Notify When Run | Fired at 21:42:00.05; action notification + "Ran" notification; re-armed for the next day |
| Charger disconnected, Run Immediately, no Notify | Fired (simulated and real unplug) |
| Charger connected, Run After Confirmation | "Run …?" notification; tapping Run ran it (confirmed by the user) |
| Reboot | Service and alarms came back after the first unlock; automation fired at 21:48 |
| Screen off + forced deep Doze | Fired at 21:50:00.03 |
| Exact alarms | Granted at install via USE_EXACT_ALARM (no settings trip) |
| Battery optimisation | Exempted; the list shows a prompt when it is not |

## Fixed during the stage
- Radio rows in the editor only reacted on the small circle; the whole row is the tap target now.
- English UI on a Hebrew phone was mirrored right-to-left; the app is kept left-to-right until it has Hebrew text.
- Deleting through the debug loader left alarms behind; it now cancels them like the Delete button does.

## Follow-ups for stage 7 (editor)
- Replace the typed time field with an iOS-style picker (tap the time, wheel or clock dialog).
- Expose the New Automation button to accessibility tools (it is missing from the UI tree).
- Hebrew strings and right-to-left layout.

## Notes
- After a reboot nothing runs until the first unlock (same as iPhone).
- Wireless debugging changes port after a reboot; reconnect with `adb mdns services` + `adb connect`.
- Test automations are loaded with `tools\seed.ps1` (debug builds only).
