# NFC Brick

Tap an NFC tag to lock selected apps (or the whole phone, except your launcher);
tap the same tag again to unlock.

## How it works

1. **First tap ever**: whatever tag you tap gets "enrolled" — its unique ID is
   saved. From then on, only that tag toggles the lock (other tags are ignored).
2. **Every tap after that**: flips a `locked` boolean in SharedPreferences.
3. **The actual blocking**: an `AccessibilityService` watches which app comes
   to the foreground. If `locked == true` and that app is in your blocked
   list (or the list is empty, meaning "block everything"), it immediately
   fires a `HOME` intent, bouncing you back to the launcher.

No cloud, no network calls, everything is local.

## Setup

1. Open this folder in Android Studio (Giraffe or newer), let Gradle sync.
2. Build & run on a real device (NFC doesn't work in the emulator).
3. In the app, tap **"Open Accessibility Settings"** and turn on **"NFC Brick"**
   under Settings > Accessibility > Installed apps. This step is required —
   without it, the lock state changes but nothing actually blocks anything.
4. Tap **"Choose apps to block"** and pick the apps you want locked out
   (e.g. Instagram, TikTok, browser). Leave nothing selected if you want it
   to block everything except the home launcher.
5. Buy any writable NFC sticker (NTAG213/215/216 — a few cents each on
   Amazon/AliExpress). No need to pre-write anything; the app enrolls
   whatever tag you first tap.
6. Tap the tag to the back of your phone (NFC antenna is usually near the
   top-center or top-back). First tap enrolls it. Every tap after that
   toggles lock/unlock.

## Limitations (read before relying on this)

- **This is a soft lock, not a hard brick.** A technically inclined user (i.e.
  you, when you're trying to cheat your own system) can go into Settings and
  disable the Accessibility Service to bypass it instantly. There's no way
  for a normal, non-Device-Owner app to prevent that on stock Android.
- If you want a much harder-to-escape version, the next step up is enrolling
  the app as **Device Owner** (via ADB on an otherwise-unused device/profile)
  and using `startLockTask()` (kiosk mode). That fully disables the recents
  button, notification shade pull-down, and Settings access while locked —
  much closer to an actual "brick." It requires either a fresh device/work
  profile with no Google account attached yet, or provisioning via QR/ADB.
  Ask if you want that variant built out.
- Accessibility-service-based blocking has a small delay (tens of
  milliseconds) — you may see a blocked app flash open before being kicked
  to home.
- On some OEM skins (Xiaomi, Samsung, etc.) you may need to also disable
  battery optimization for the app so Android doesn't kill the accessibility
  service in the background.

## Project structure

```
app/src/main/java/com/example/nfcbrick/
├── MainActivity.kt              # NFC read, enrollment, lock toggle, app picker UI
├── LockAccessibilityService.kt  # Watches foreground app, kicks to home when locked
└── PrefsHelper.kt               # SharedPreferences wrapper (lock state, tag ID, blocked apps)
```
