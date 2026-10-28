# Stage 5 results (2026-10-27)

Pixel 10 Pro XL, Android 17 beta. Debug build.

| Trigger | Test | Result |
|---|---|---|
| App: WhatsApp Is Opened / Is Closed | User opened WhatsApp and went home, twice | Opened and Closed fired at the right moments (after the fix below) |
| NFC tag | Simulated tap from the PC (debug-only extra), matching and non-matching tag IDs | Matching ID fired, other ignored |
| NFC real tag | User's Rav-Kav card (ISO 14443 Type B: IsoDep, NfcB), tapped on the home screen twice | Android delivered it straight to the app (no chooser); same ID both taps; automation fired |
| 49 unit tests | gradlew testDebugUnitTest | Pass |

## Fixed during the stage
1. **Crash on Allow:** ACCESSIBILITY_DETAILS_SETTINGS needs a system-only permission. The app now opens the
   Accessibility list, and the Allow button falls back to Settings instead of crashing if a page is refused.
2. **False "closed":** overlay apps (here Dynamic Spot, com.jamworks.dynamicspot) and launcher popups send window
   events with View class names. Only windows whose class is a real Activity of that package now count.

## Design notes
- App watcher is an Accessibility service limited to window-change events with canRetrieveWindowContent=false:
  it knows which app is in front, nothing on screen. Only launchable apps and home screens count as apps.
- NFC: tags are matched by hardware ID read from the NFC stack's Tag object, never from the URI in the intent,
  so another app can't fake a tap through the exported activity. "Write tag" (optional) writes
  pixelautomations://tag/<id> plus an Android Application Record so tags that already held a link route to the app.
- adb-installed builds weren't blocked by "restricted settings"; a build installed from a file may be (the app says
  where to allow it).
