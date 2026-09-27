# Quick OTP

Android authenticator for TOTP and HOTP (`otpauth://`) secrets. Scan a barcode, paste a secret, label it, and read the current code on the home list. A scan that is not an OTP is kept as-is and shown in the same list.

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

- **Add from barcode** — Camera button in the top bar. An `otpauth://` URI or a Base32 secret becomes an OTP, and the app asks for a label in a field labeled “Add label”. Any other barcode is stored exactly as scanned. The app asks you to label it, then shows that label and the scanned value in the home list.
- **Stored barcodes** — These rows sit with the OTPs. A down chevron next to the label expands a clean barcode rendered from the stored value (the same symbology when it can be encoded, otherwise a QR of that value). The chevron switches to up and hides the barcode, showing the value again. Tap the value to copy it. The pencil menu renames or deletes it.
- **Add from secret** — Plus button in the top bar. Paste a Base32 secret or a full `otpauth://totp/…` / `otpauth://hotp/…` URI, then label it.
- **Home list** — Each OTP row shows the label, the current code (grouped, tap to copy), and a circular spinner for TOTP remaining time. The code and spinner turn **red in the last 10 seconds**. HOTP rows show a refresh button that advances the counter.
- **Pencil menu** — For an OTP: rename, show barcode (QR of the `otpauth` URI), show secret (Base32 plus URI), delete with confirmation. For a stored barcode: rename and delete.
- **Protocols** — TOTP (RFC 6238) and HOTP (RFC 4226), algorithms SHA1 / SHA256 / SHA512, 5–10 digits, custom period. Unknown issuer/account from some barcode apps (`<Unknown>`) is ignored when suggesting a label.

## Screens

```
┌─────────────────────────────────────┐
│ Quick OTP              [barcode] [+]│  ← scan camera · paste secret
├─────────────────────────────────────┤
│                                     │
│  OTP for mysite                     │
│  123 456                   ◷   ✎    │  ← spinner (red last 10s)
│                                     │
│  GitHub                             │
│  847 291                   ◷   ✎    │
│                                     │
│  Lobby door                  ⌄   ✎  │  ← stored barcode (value)
│  https://example.com/door           │
│                                     │
│  Lobby door                  ⌃   ✎  │  ← chevron expands a clean
│  ┌─────────────────────────┐        │     barcode of that value
│  │        barcode          │        │
│  └─────────────────────────┘        │
│                                     │
│  ✎ → OTP: rename · show barcode     │
│      show secret · delete           │
│      barcode: rename · delete       │
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

CI (`.github/workflows/android.yml`) runs unit tests and `assembleRelease`, then publishes `app-<version>.apk` on push to `main` / `master`.

## Tests

JVM unit tests cover Base32, RFC 4226 HOTP vectors, RFC 6238 TOTP vectors (SHA1/SHA256/SHA512), `otpauth://` parse/encode (including `<Unknown>` labels), period/urgency math, input parsing, scan classification (OTP versus stored barcode), and barcode encode/decode round-trips.

```bash
./gradlew :app:testDebugUnitTest
```

## Project layout

```
app/src/main/java/com/quickotp/app/
  otp/           TOTP/HOTP, Base32, otpauth URI
  barcode/       QR and other barcode encode (ZXing), scan classification, Android bitmaps
  data/          Room accounts
  ui/            Compose home list, camera scan, dialogs
.github/workflows/android.yml
```

## Cursor agent skills

| Skill | When |
|-------|------|
| **rebuild-app** | After any `app/` or Gradle change — compile, unit tests, debug APK |
| **compile-kotlin** | Faster Kotlin + unit tests only (does not replace rebuild-app) |
| **update-readme** | After user-facing behaviour or build/layout changes |
| **local-workspace** | This tree is the source; do not rsync to “recover” files |
