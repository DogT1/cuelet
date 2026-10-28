# Stage 2 results (2026-10-27)

Pixel 10 Pro XL, Android 17 beta. Debug build.

| Trigger | How it was tested | Result |
|---|---|---|
| Battery Level (Falls Below) | Simulated 61 → 60 → 59% | Fired once at 60%, not again at 59% (one fire per crossing) |
| Battery Saver on / off | User, quick settings | Both fired |
| Airplane Mode on / off | User, quick settings | Both fired |
| Bluetooth connects / disconnects (any device) | User, soundcore Liberty 5 Pro earbuds | Both fired after the fix below |
| Wi-Fi joins "kolbasa" / leaves any | User, Wi-Fi off and on | Both fired |
| Android Auto connects | No car available | Unit-tested only; uses AndroidX CarConnection |
| 23 unit tests | `gradlew testDebugUnitTest` | Pass |

Permission flow: "Needs setup" showed on the Bluetooth and named-Wi-Fi automations; the user granted Nearby devices
and Location (Allow all the time) from the editor's Allow button.

## Fixed during the stage
- Bluetooth never fired: ACL broadcasts come from the Bluetooth process, not system_server, and a
  RECEIVER_NOT_EXPORTED receiver silently drops them. That receiver is now EXPORTED (the actions are protected
  broadcasts, so other apps can't fake them).

## Notes
- Turning Wi-Fi or Airplane Mode off drops wireless debugging; reconnect with `adb mdns services` (new port).
- `cmd power set-mode` does not toggle Battery Saver; test it from quick settings.
- Added dependency: `androidx.car.app:app:1.4.0` (approved by the user) for Android Auto detection.
