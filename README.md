# Quick OTP

Android authenticator for TOTP and HOTP (`otpauth://`) secrets. Scan a barcode, paste a secret, label it, and read the current code on the home list.

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

- **Add from barcode** — Camera button in the top bar. Points the camera at a QR (or other) barcode containing an `otpauth://` URI or a Base32 secret. After a successful scan, the app asks for a label (for example `OTP for mysite`).
- **Add from secret** — Plus button in the top bar. Paste a Base32 secret or a full `otpauth://totp/…` / `otpauth://hotp/…` URI, then label it.
- **Home list** — Each row shows the label, the current code (grouped, tap to copy), and a circular spinner for TOTP remaining time. The code and spinner turn **red in the last 10 seconds**. HOTP rows show a refresh button that advances the counter.
- **Pencil menu** — Rename, show barcode (QR of the `otpauth` URI), show secret (Base32 plus URI), delete with confirmation.
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
│  ✎ → rename · show barcode          │
│      show secret · delete           │
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

JVM unit tests cover Base32, RFC 4226 HOTP vectors, RFC 6238 TOTP vectors (SHA1/SHA256/SHA512), `otpauth://` parse/encode (including `<Unknown>` labels), period/urgency math, input parsing, and QR encode/decode round-trips.

```bash
./gradlew :app:testDebugUnitTest
```

## Project layout

```
app/src/main/java/com/quickotp/app/
  otp/           TOTP/HOTP, Base32, otpauth URI
  barcode/       QR encode (ZXing) and Android bitmaps
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
