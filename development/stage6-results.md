# Stage 6 results (2026-10-27)

Pixel 10 Pro XL, Android 17 beta. Debug build. Actions were run from the PC with the debug-only
DebugRunReceiver (the app gets a "Run Now" button in stage 7).

| Action(s) | Result |
|---|---|
| Show Notification, Flashlight on/off, Vibrate, Wait, Media volume 30 %, Copy to Clipboard, Speak Text | All 8 ran |
| Webhook GET to the Pi-hole web UI on the NAS | OK |
| Open URL from the background (Chrome) | Opened directly (Accessibility exemption) |
| Do Not Disturb on/off, Ringer vibrate/normal | Ran after the user granted Do Not Disturb access |
| Brightness 15 % → 60 % | Ran after the user granted Modify system settings |
| Pi-hole pause 1 min → resume (v6 API, session login/logout) | Ran; password entered by the user in Connections |
| Mobile data off without Shizuku | Fallback worked: "Tap to open Mobile data" (Accessibility was off at that moment) |
| Home Assistant, Media keys, Run Another Automation, Force-stop | Unit-tested only |
| 55 unit tests | Pass |

## Shizuku
Shizuku 13.6.0 doesn't run on the Android 17 beta: its server crashes with
UnsatisfiedLinkError: librish.so not found (open upstream issue RikkaApps/Shizuku#2180, Pixel 10 Pro XL).
The Shizuku path is built and unit-tested (commands, package-name check); until Shizuku works, Wi-Fi / Bluetooth /
mobile data / Airplane actions open the matching settings panel and force-stop opens the app's info page.
A community fork ("Nightzuku") claims Android 17 support; not installed (needs vetting first).

## Notes
- Force-stopping Automations (e.g. m force-stop) makes Android switch off its Accessibility service; the user has
  to turn "Automations: App trigger" back on. Without it, Open App/URL fall back to a tap-to-open notification.
- Connections (Pi-hole password, Home Assistant token) live in private SharedPreferences, outside automations and
  exports.
- Cleartext http is allowed app-wide for user-entered LAN addresses (network_security.xml).
- Deferred to stage 7: If / Otherwise, Choose from Menu, Run Now button, polished action picker.
