# Ultimate KeyMapper

An Android 11+ keyboard and mouse companion app, built with Kotlin and Jetpack Compose.

## Current foundation

- Detects connected Android input devices (USB/Bluetooth keyboards and mice exposed by Android).
- Shows live key and mouse input diagnostics while the app is in the foreground.
- Provides a draggable mapping-layout editor with saved positions and key assignments.
- Saves local profiles and mappings on-device.
- GitHub Actions builds a Debug APK automatically.

## Important Android limitation

Receiving input in this app is not the same as injecting touch events into another app. Android restricts cross-app input injection. This foundation does **not** claim to control third-party games yet. A legitimate activation/injection path must be implemented and tested separately for each supported Android version and must not bypass game anti-cheat systems.

## Build

Open this repository in GitHub Codespaces, then run:

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Device test target

OPPO A3x / Android 15, plus other Android 11+ devices. Actual USB/Bluetooth support depends on the input device and Android vendor implementation.
