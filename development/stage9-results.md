# Stage 9 results (2026-10-28)

Pixel 10 Pro XL, Android 17 beta. New actions, built after the user asked for a lost/stolen-phone automation
("text from a trusted contact with a code word → send location + front/back photos, no unlock, no confirmation").

## New actions
| Action | Delivery | Tested |
|---|---|---|
| Send Location | GPS + Maps link to the NAS upload URL (multipart POST) | Location fix taken, POST sent (HTTP response received) |
| Capture Photo (front/back) | JPEGs to the NAS, silently, from a camera foreground service | **2 images captured** with screen on, app in background; POST sent |
| Record Audio (3–60 s) | m4a to the NAS, microphone foreground service | Built; not run on device yet |
| Lock Screen | accessibility GLOBAL_ACTION_LOCK_SCREEN | Built (needs App-trigger accessibility on) |
| Take Screenshot | accessibility GLOBAL_ACTION_TAKE_SCREENSHOT | Built |
| Set Alarm | AlarmClock intent | Built |
| Start Timer | AlarmClock intent | Built |

73 phone unit tests + 3 watch, all pass.

## How the anti-theft example maps
Message trigger (sender = trusted contact, contains = code word) + Run Immediately (no confirmation) + Send Location
+ Capture Photo. The Message trigger fires from notification access even when locked. Verified in StageNineTest.

## Verified pipeline
With a test URL on the LAN, `capturePhotos` logged "photo upload HTTP 404 (2 image(s))" and `sendLocation` logged
"location upload HTTP 404" — i.e. both cameras captured, the GPS fix was taken, multipart was built and POSTed, and a
response came back. The 404 is only because the test URL (the Pi-hole web server) has no upload handler; a real
endpoint returning 2xx logs success.

## Open / caveats
- **Camera while the screen is OFF/locked was not conclusively verified** this session: the phone wouldn't stay
  asleep over the wireless-debug link, and one locked-ish attempt failed to open the camera. Android OEM policy can
  block camera access from the lock screen. The user needs to test for real: lock the phone, trigger it, see if
  photos arrive. If blocked, a fallback is to capture on the next unlock.
- **No NAS upload endpoint yet.** The actions POST multipart/form-data (fields lat/lng/maps/note/device/at; file
  parts "photo"/"audio") to the Connections → "NAS uploads" URL. The user must stand up a receiver on the NAS
  (any small HTTP endpoint that saves the parts). A tiny receiver couldn't be hosted from the dev PC (blocked by the
  session's safety classifier), so end-to-end file storage is untested.
- Camera/mic/background-location permissions were granted over adb for testing; in normal use the editor's "Needs
  setup" + Allow flow requests them.
- Shutter sound may be forced on in some regions by law.
