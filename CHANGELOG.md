# Changelog

Notable changes per release. **Before tagging a new version, add a `## vX.Y.Z` section here** — the release
workflow pulls the matching section into the GitHub Release notes, so every version ships with its fixes listed.

## v0.1.2

### Fixed
- NFC: the app no longer reacts to every NFC card near the phone (a transit or bank card in the case made it
  scan nonstop). The tag listener now only runs while an automation with an NFC trigger is switched on.

### Added
- 14 more conditions for **If / Otherwise**, in the spirit of iPhone Shortcuts: battery saver, airplane mode,
  dark mode, internet over Wi-Fi / mobile data / none, Bluetooth device connected (any or a specific one),
  headphones, ringer mode, volume above / below, music playing, on a call, at a place, phone locked,
  app in front, and a random chance.
- A grouped **Pick a condition** sheet, like Add Action, now that the If step has 21 conditions.

## v0.1.1

### Fixed
- Rotating the phone no longer bounces you back to the automations list — the app is locked to portrait.

## v0.1.0

First public release — iPhone-style automations for Android, with no root.

### Added
- 20+ triggers: time & sun, alarm, sleep, arrive / leave, commute, Android Auto, email, message, Wi-Fi, Bluetooth,
  NFC, app open/close, airplane, Do Not Disturb, battery, charger, sound — and a Wear OS workout.
- A When → Do editor with **If / Otherwise** and **Choose from Menu**.
- Rich actions: notify, speak, open app / URL, webhook, Home Assistant, volume / ringer / DND, flashlight,
  brightness, send SMS, send location (to a contact over SMS / WhatsApp / Telegram), capture photo, record audio,
  lock screen, screenshot, set alarm, start timer.
- Seven languages (English, Hebrew RTL, Russian, German, French, Spanish, Italian) and Light / Dark / AMOLED themes.
