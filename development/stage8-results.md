# Stage 8 results (2026-10-28)

Phone: Pixel 10 Pro XL (Android 17 beta). Watch: Galaxy Watch7 SM-L310 (Android 16 / Wear OS 6), paired to the PC
over wireless debugging. Debug builds of both apps (same application ID and debug key, as the Data Layer requires).

| Check | Result |
|---|---|
| Watch app "Send test" → phone Workout (Starts) automation | Fired (00:05:19, type "Other workout") |
| Real Samsung Health **Running** workout started on the watch | Phone "workout started" fired (00:06:26), type Running |
| Same workout ended | Phone "workout ended" fired (00:08:12), type Running |
| Unit tests | 69 phone + 3 watch, pass |

## How it works
- Watch module wear/: Health Services passive monitoring with user-activity info (ACTIVITY_RECOGNITION).
  passive → exercise = start, exercise → anything else = end; last state kept in preferences. Re-registers on boot.
- Sends start|TYPE / nd|TYPE on /automations/workout via the Wear OS MessageClient to connected phones.
- Phone WatchListener (WearableListenerService) fires Trigger.Workout (Starts/Ends, any or chosen types).
- Samsung Health workouts are reported through Health Services, so the Health Connect fallback wasn't needed.

## Notes
- New dependencies (named in the stage 8 plan): play-services-wearable (both apps), health-services-client and
  Guava (for ListenableFuture) on the watch. The debug watch APK is ~12 MB because of Guava; a release build with
  minification would shrink it.
- The watch's wireless debugging drops when the watch sleeps; reconnect with db mdns services + db connect.
