# Stage 3 results (2026-10-27)

Pixel 10 Pro XL, Android 17 beta. Debug build. Location faked from the PC with `DebugLocationReceiver`
(Play Services mock mode, debug builds only); real-world walk/drive tests still to do.

| Trigger | Test | Result |
|---|---|---|
| Sunrise / sunset maths | Unit tests vs. published times (London June 2024, New York Dec 2024), polar night | Within 3 min; no sunrise in Tromsø midwinter |
| Time of Day: 15 min before sunset | Scheduled on the phone for Tel Aviv area coordinates | 2026-10-28 16:40 (sunset ≈ 16:55) |
| Before I Commute, at the starting place | Fake location at origin, departure 22:23 | Fired at 22:23:00 |
| Before I Commute, away from the starting place | Fake location 3 km away | Skipped, logged "Skipped: not at Test spot" |
| Arrive (200 m radius) | Fake walk from 3 km away into the place | Fired about 3 s after entering |
| Leave | Fake walk back out | Fired about 12 s after leaving |
| Time windows (incl. overnight), commute day rules (Sun–Thu, evening-before) | Unit tests | Pass |
| 33 unit tests total | `gradlew testDebugUnitTest` | Pass |

## Design notes
- Places are picked with "Use current location" or an address search (Android Geocoder). A map picker would need a
  Google Maps API key; not added.
- Geofences: one per enabled Arrive/Leave automation, re-registered on every sync, boot, app update and when
  Location is switched back on (`PROVIDERS_CHANGED`). Initial trigger off, so creating one at home doesn't fire.
- Sunrise/sunset store the coordinates when the automation is made, so they need no location access later.
- Before I Commute: iOS predicts departure; Android has no public equivalent, so the user sets the usual departure
  time. The alarm runs N minutes before it and checks a fresh location fix against the origin (radius + accuracy).

## To test in real life
- Arrive/Leave at home with the real GPS (Android geofences can take 1–3 minutes in the real world).
- A sunrise or sunset automation on a normal day.
