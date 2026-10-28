# Device support & compatibility

Cuelet is a no-root background-automation app, so it runs into the limits Android (and each phone maker) puts on
exactly that kind of app. It does **not** work on every Android device. Here is the honest picture.

## Hard requirements

- **Android 14 (API 34) or newer.** This is the biggest limit — Cuelet will not install on Android 13 or older.
  It relies on Android 14's foreground-service-type permission model for the Capture Photo / Record Audio / Send
  Location actions. Lowering the minimum is possible but needs per-API guards and testing on older versions, so it
  is the top item for reaching more devices (tracked as a known gap, not done yet).
- **Google Play Services** for anything location-based: the **Arrive**, **Leave** and **Before I Commute** triggers,
  the **Send Location** action, and the **Workout** trigger. On devices without Play Services (de-Googled / custom
  ROMs, some Huawei, Amazon Fire) those specific items don't work — everything else still does.

## Background reliability depends on the phone maker

Cuelet reacts to events while it isn't open, which OEMs restrict aggressively and *differently*. The in-app
"Allow background" button covers the standard Android battery exemption, but most skins add their own layer:

- **Samsung (One UI):** Settings → Battery → Background usage limits → **Never sleeping apps** → add Cuelet.
- **Xiaomi / Redmi / POCO (MIUI / HyperOS):** enable **Autostart**, set battery to **No restrictions**, lock the app
  in recents.
- **Oppo / Realme / OnePlus (ColorOS / OxygenOS)** and **Vivo / iQOO (Funtouch):** allow **Auto-launch** and
  background activity, disable battery optimisation.
- **Huawei / Honor (EMUI / Magic OS):** App launch → **Manage manually** (auto-launch, secondary launch, run in
  background). Also no Play Services — see above.

Without these, time / geofence / notification triggers may fire late or not at all. Per-brand steps:
[dontkillmyapp.com](https://dontkillmyapp.com).

## Per-feature needs (and what happens without them)

| Feature | Needs | Without it |
|---|---|---|
| Arrive / Leave / Before I Commute, Send Location | GPS + Google Play Services | trigger / action unavailable |
| Send SMS | a SIM / telephony (phones only) | action fails on tablets & Wi-Fi-only devices |
| Capture Photo | a camera | action fails (saved photos go to the gallery) |
| Record Audio | a microphone | action fails |
| NFC trigger | NFC hardware | trigger unavailable |
| Bluetooth trigger | Bluetooth | trigger unavailable |
| Wi-Fi / Bluetooth / Data / Airplane toggle, Force-stop App | [Shizuku](https://shizuku.rikka.app/) running + allowed | opens the settings switch for a manual tap instead |
| Workout trigger | Wear OS 3+ watch with Health Services + the `wear/` companion | trigger unavailable |
| Transaction trigger | Google Wallet | trigger unavailable |
| Alarm trigger | Google Clock or Samsung Clock | other clock apps may not be detected |
| Lock Screen / Take Screenshot, auto-send | the App-trigger accessibility service enabled | action fails with a prompt |

The app installs on tablets and non-cellular devices (all hardware features are declared optional); the features
that need missing hardware are simply unavailable or fail gracefully — they don't crash the app.

## Tested on

- Pixel 10 Pro XL (Android 17 beta)
- Galaxy S23 FE (Android 16, One UI)
- Galaxy Watch7 (Wear OS)

Other brands and versions (on Android 14+) are expected to work but are **not** verified. The background-reliability
notes above are the most likely rough edges.

## Known gaps

- **Android 13 and older:** not supported (minSdk 34).
- **Non-Google-Play devices:** location triggers and the watch Workout trigger don't work.
- **Shizuku on the Android 17 beta:** blocked upstream
  ([issue #2180](https://github.com/RikkaApps/Shizuku/issues/2180)); radio toggles fall back to opening the settings
  panel.
- **Locked-screen camera capture** depends on the phone maker's lock-screen camera policy — verify on your device.
- OEM background-killing (above) can delay or drop triggers until the app is allow-listed.
