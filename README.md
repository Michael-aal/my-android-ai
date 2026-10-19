# My Android AI

A native Android assistant built by migrating the reusable intent and assistant logic from the original Python project, [my_ai](https://github.com/Michael-aal/my_ai).

## Current features

- Text commands and speech-to-text from the app screen and floating assistant
- Open common Android apps and URLs
- Optional floating command panel through Android Accessibility (must be manually enabled by the device owner)
- User-requested visible-label tap, focused-field text entry, screen-text reading, Back and Home actions
- Time, date, and battery queries
- Spoken responses through Android Text-to-Speech
- Gemini AI fallback using a key entered by the user and encrypted with Android Keystore
- Dark interface with explicit permission setup

## Project structure

- `app/` — native Android app (Kotlin)
- `app/src/main/java/com/michael/myandroidai/AssistantCommandRouter.kt` — Kotlin port of the original intent parser
- `app/src/main/java/com/michael/myandroidai/AssistantAccessibilityService.kt` — opt-in cross-app UI assistance
- `python-reference/` — source modules copied from the original Python project for migration and comparison
- `ANDROID_MIGRATION.md` — porting status, boundaries, and build notes

## Build

Open the project in Android Studio, sync Gradle, and run it on an Android device or emulator. Microphone access is requested only when voice input is used. Cross-app assistance requires manually enabling **My Android AI** in Android Accessibility settings.

## Important status notes

The Python files in `python-reference/` are preserved source references, not code that Android executes directly. Windows-only controls and desktop audio dependencies need native Android equivalents. Continuous wake-word listening, spoken responses, persistent assistant memory, and secure AI-provider integration are not finished yet.

No API keys, local databases, private memory files, recorded audio, or executable archives are intentionally included. Never commit API keys to this repository or package them in an APK.

See [Android migration notes](ANDROID_MIGRATION.md) for the detailed status.
