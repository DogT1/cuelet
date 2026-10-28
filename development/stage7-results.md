# Stage 7 results (2026-10-27)

Pixel 10 Pro XL, Android 17 beta. Debug build.

| Feature | Test | Result |
|---|---|---|
| iOS-style New Automation list (Event / Travel / Communication / Transaction / Setting, with subtitles) | Opened on the phone | Shown correctly |
| If / Otherwise (charging, battery, Wi-Fi network, time window, Do Not Disturb, weekday) | "Is charging?" on the phone (not charging) | Took the Otherwise branch |
| Choose from Menu (notification buttons, max 3) | User tapped "Yes" | Ran the Yes branch, then the actions after the menu |
| Run Now (runs the draft, saved or not) | Unit/behaviour shared with the engine | Built; for the user to try |
| Clock-dial time picker (replaces typed time) | Build | Built; for the user to try |
| Export / Import (JSON via Android's file picker; Connections secrets excluded; Run Automation IDs remapped) | Unit test of the remapping | Built; for the user to try |
| 60 unit tests | gradlew testDebugUnitTest | Pass |

## Design notes
- Menus are paused runs kept in memory (token → automation snapshot + menu index), so they also work for unsaved
  drafts started with Run Now. If Android kills the app before the tap, the tap is logged as expired.
- Choose from Menu is offered only at the top level; If can nest anything else.
- Hebrew UI not done yet (all text is English, layout forced left-to-right).

## Language (added after stage 7)
- Settings screen (gear icon): Phone language / English / עברית, switching instantly; Connections and battery status moved here.
- Whole UI translated to Hebrew (~420 strings, setup/HebrewStrings.kt) with right-to-left layout and a Sunday-first week.
- StringsCoverageTest fails on any untranslated string or mismatched placeholder.
- Checked on the phone: list and Settings in Hebrew (phone language) and English; switching works both ways.
