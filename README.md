# Quick OTP

Android authenticator for TOTP and HOTP (`otpauth://`) secrets. Scan a barcode, paste a secret, label it, and read the current code on the home list. The home screen has two tabs, **OTP** and **QR codes**. A scan that is not an OTP is kept as-is and shown only on the QR codes tab.

Designed for sideloading on recent 64-bit ARM phones. CI builds a signed arm64 release APK and publishes it to GitHub Releases.

## Requirements

| Setting | Value |
|---------|-------|
| **minSdk** | 26 (Android 8.0 Oreo) |
| **targetSdk** | 34 |
| **compileSdk** | 34 |
| **ABI** | arm64-v8a only |
| **Package** | `com.quickotp.app` |
| **JDK** | 17 (required for Gradle/AGP) |

## Features

- **Home tabs** — Under the top bar, **OTP** and **QR codes** split the list. The OTP tab shows only authenticator codes. The QR codes tab shows only stored barcodes. Saving a new OTP switches to the OTP tab; saving a stored barcode switches to the QR codes tab. The chosen tab is kept across rotation.
- **Add from barcode** — Camera button in the top bar. An `otpauth://` URI or a Base32 secret becomes an OTP, and the app asks for a label in a field labeled “Add label”. Any other barcode is stored exactly as scanned. The app asks you to label it, then shows that label and the scanned value on the QR codes tab.
- **Add from image** — Gallery button in the top bar, next to the camera. It opens the system photo picker (no storage permission needed), reads the first barcode in the chosen image, and then behaves exactly like a camera scan. If the image has no readable barcode, a snackbar says so.
- **Stored barcodes** — These rows are only on the QR codes tab. A down chevron next to the label expands a clean barcode rendered from the stored value (the same symbology when it can be encoded, otherwise a QR of that value). The chevron switches to up and hides the barcode, showing the value again. Tap the value to copy it. When the whole value is an `http` or `https` URL, the text is an underlined link: tap opens it in a browser, and a long-press copies it. The chevron, arrows, and pencil share the label line, so the value (often a long link) and the barcode use the full width of the card. Whether the barcode or the value is showing is kept on the device. The pencil menu renames or deletes it.
- **Add from secret** — Plus button in the top bar. Paste a Base32 secret or a full `otpauth://totp/…` / `otpauth://hotp/…` URI, then label it.
- **Home list** — On the OTP tab, each row shows the label with a chevron beside it, the current code (grouped, tap to copy), and a circular spinner for TOTP remaining time. The code and spinner turn **red in the last 10 seconds**. HOTP rows show a refresh button that advances the counter. The up chevron hides the code, the spinner, and the refresh button, and switches to down; down shows them again. That choice is kept on the device across launches.
- **Reorder** — Small up and down arrows beside the pencil on every row move it one place among the rows on the current tab. The top row on that tab cannot move up and the bottom row cannot move down. The order is kept on the device; new entries are added at the top.
- **Pencil menu** — For an OTP: rename, show barcode (QR of the `otpauth` URI), show secret (Base32 plus URI), delete with confirmation. For a stored barcode: rename and delete.
- **Protocols** — TOTP (RFC 6238) and HOTP (RFC 4226), algorithms SHA1 / SHA256 / SHA512, 5–10 digits, custom period. Unknown issuer/account from some barcode apps (`<Unknown>`) is ignored when suggesting a label.
- **App update** — On each cold open the app checks GitHub Releases for a newer signed APK (`diegoboston/quickotp`, stable asset `app.apk`). When one exists, a snackbar offers **Update now**. Tapping it downloads the build (progress sits above the tabs) and hands it to the system installer. If install-from-this-app is off, Android opens that setting and the installer continues after you allow it. Dismiss the snackbar to keep the current version; the prompt is shown once per app session. A failed check stays quiet.

## Screens

```
┌─────────────────────────────────────┐
│ Quick OTP      [barcode] [image] [+]│  ← scan camera · scan image · paste secret
├─────────────────────────────────────┤
│  OTP              QR codes          │  ← only that tab’s rows
├─────────────────────────────────────┤
│  OTP for mysite  ⌃             ↑    │
│  123 456                   ◷   ↓ ✎  │  ← ⌃ hides code, timer, refresh
│                                     │
│  GitHub          ⌄             ↑ ✎  │  ← hidden, kept next launch
│                                ↓    │     ↑ ↓ move within this tab
│                                     │
│  ✎ → rename · show barcode          │
│      show secret · delete           │
│                                     │
│  Version 1.0.12 available [Update]  │  ← once per launch, if newer
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ Quick OTP      [barcode] [image] [+]│
├─────────────────────────────────────┤
│  OTP              QR codes          │
├─────────────────────────────────────┤
│  Lobby door      ⌄             ↑ ✎  │  ← stored barcode (value)
│                                ↓    │
│  https://example.com/door/a-long-   │  ← URL: tap opens, hold copies
│  link-that-wraps                    │
│                                     │
│  Lobby door      ⌃             ↑ ✎  │  ← chevron expands a clean
│                                ↓    │     barcode of that value
│  ┌───────────────────────────────┐  │
│  │           barcode             │  │
│  └───────────────────────────────┘  │
│                                     │
│  ✎ → rename · delete                │
└─────────────────────────────────────┘
```

## Build

Use JDK 17 and Android SDK 34. This repo expects the same userspace toolchain as Stage Manager:

```bash
source ~/tmp/android-build/env.sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Or run the project scripts (they source `env.sh` for you):

```bash
bash .cursor/skills/compile-kotlin/scripts/compile-kotlin.sh
bash .cursor/skills/rebuild-app/scripts/rebuild-app.sh
```

CI (`.github/workflows/android.yml`) runs unit tests and `assembleRelease`, then publishes `app-<version>.apk` and a stable `app.apk` alias on push to `main` / `master`. The in-app updater downloads that alias.

## Tests

JVM unit tests cover Base32, RFC 4226 HOTP vectors, RFC 6238 TOTP vectors (SHA1/SHA256/SHA512), `otpauth://` parse/encode (including `<Unknown>` labels), period/urgency math, input parsing, scan classification (OTP versus stored barcode), http(s) link detection for stored barcode text, GitHub release tag and APK asset parsing, barcode encode/decode round-trips, gallery image scans, and list reordering.

```bash
./gradlew :app:testDebugUnitTest
```

## Project layout

```
app/src/main/java/com/quickotp/app/
  otp/           TOTP/HOTP, Base32, otpauth URI
  barcode/       QR and other barcode encode (ZXing), ML Kit scan setup and image scan, scan classification, Android bitmaps
  update/        GitHub Releases check, APK download, package installer
  data/          Room accounts
  ui/            Compose home list, camera scan, dialogs
.github/workflows/android.yml
```

## Updating

Debug and release builds share the keystore in `app/keystore/`, so a downloaded release installs on top of a previous sideload and keeps the accounts database. `versionCode` comes from `GITHUB_RUN_NUMBER` (`versionName` `1.0.<run>`), and the release tag is `v1.0.<run>`. The updater treats the last number in that tag as the version code and only offers a build when it is higher than the installed one.

## Cursor agent skills

| Skill | When |
|-------|------|
| **rebuild-app** | After any `app/` or Gradle change — compile, unit tests, debug APK |
| **compile-kotlin** | Faster Kotlin + unit tests only (does not replace rebuild-app) |
| **update-readme** | After user-facing behaviour or build/layout changes |
| **local-workspace** | This tree is the source; do not rsync to “recover” files |
