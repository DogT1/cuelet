# Cuelet

An Android app that recreates the iPhone's **Shortcuts → Automation** tab, with **no root**. "When this happens,
do that" on your own phone: when you arrive home, when a charger is connected, at sunset, when an app opens, and so on.

Everything runs and stays on the phone. There is no account, no cloud, and no analytics. Automations can be
exported and imported as a JSON file; the only things that leave the phone are requests **you** configure (a
webhook, a Home Assistant call, or a message you send).

**[⬇️ Download the latest APK](https://github.com/DogT1/cuelet/releases/latest)**  ·  **[🌐 Website](https://dogt1.github.io/cuelet/)**  ·  Android 14+

## Features

- **Every iOS personal-automation trigger type** (20+), mapped to no-root Android equivalents:
  Time of Day (and sunrise/sunset), Alarm, Sleep, Arrive, Leave, Before I Commute, Android Auto (iOS CarPlay),
  Email, Message, Transaction (Wallet), Wi-Fi, Bluetooth, NFC, App opened/closed, Airplane Mode, Do Not Disturb
  (iOS Focus), Battery Saver (iOS Low Power), Battery Level, Charger, Sound Recognition, and **Galaxy Watch /
  Wear OS workout** (iOS Apple Watch Workout, via the companion app in `wear/`).
- **Run After Confirmation** or **Run Immediately**, with **Notify When Run**, like iOS.
- **Actions:** show notification, open app / URL, webhook, Home Assistant service, set volume / ringer / Do Not
  Disturb, flashlight, brightness, speak text, clipboard, send SMS, wait, media keys, vibrate, run another
  automation, **If / Otherwise**, **Choose from Menu**, lock screen, take screenshot, set alarm, start timer, plus
  **Send Location** (text or share a Maps link to a contact over SMS / WhatsApp / Telegram), **Capture Photo** and
  **Record Audio** (saved to the phone). Wi-Fi / Bluetooth / mobile data / Airplane toggles use
  [Shizuku](https://shizuku.rikka.app/) when available, and otherwise open the switch for you to tap.
- **iOS-style editor**, a **Run Now** button, **Light / Dark / AMOLED** themes, and seven languages — English,
  Hebrew (right-to-left), Russian, German, French, Spanish and Italian.
- Clear **"Needs setup"** prompts for the permission each automation needs, requested only when used.

## No root, and what that costs

Android reserves some things for the Play Store or for root. This app uses the public, documented ways around that:
a foreground service, a notification listener, an accessibility service (window changes only — it can't read screen
content), geofences, and optional Shizuku for a few system toggles. A sideloaded app also needs Android's
"Allow restricted settings" step before Accessibility / Notification access can be switched on; the app explains
this where it matters.

## Build

Android Studio, or the command line:

```
./gradlew :app:assembleDebug            # the phone app
./gradlew :wear:assembleDebug           # the Galaxy Watch / Wear OS companion
./gradlew :app:testDebugUnitTest :wear:testDebugUnitTest
```

JDK 17, Android SDK platform 36. Install your build with `adb install app/build/outputs/apk/debug/app-debug.apk`,
or just grab a ready-made signed APK from the [Releases page](https://github.com/DogT1/cuelet/releases/latest).
The watch companion is optional and only needed for the Workout trigger; install it on the watch and pair it from
its own app screen.

## Privacy

- Automations, settings, and any passwords/tokens you enter live in the app's private storage, never in this repo
  and never in an export.
- The notification listener only reads notifications from the apps your automations use, matches them in memory,
  and drops the text; the run log records a generic reason ("Message (WhatsApp)").
- The accessibility service receives only "which app came to the front" events.

## Device support

Cuelet needs **Android 14+** and does not run on every device. Google Play Services is required for the
location triggers and the watch Workout trigger, and background reliability depends on your phone maker's battery
rules (Samsung, Xiaomi, Oppo, etc. each need the app allow-listed). The full matrix — minimum version, per-feature
hardware/service needs, per-brand background steps, and what's tested — is in **[COMPATIBILITY.md](COMPATIBILITY.md)**.

## Status / caveats

- Developed and tested on a Pixel 10 Pro XL (Android 17 beta), a Galaxy S23 FE (One UI) and a Galaxy Watch7.
- **Shizuku** doesn't run on the Android 17 beta yet ([upstream issue](https://github.com/RikkaApps/Shizuku/issues/2180));
  until it's fixed, the radio toggles open the relevant settings switch instead of flipping it.
- **Capture Photo** and **Record Audio** save to the phone's gallery; capturing while the screen is locked depends
  on your phone maker's lock-screen camera policy and should be tested on your device.

## License

MIT — see [LICENSE](LICENSE). Fork and rename freely; contributions welcome.
