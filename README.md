# My Android AI

A native Android assistant built by migrating the reusable intent and assistant logic from the original Python project, [my_ai](https://github.com/Michael-aal/my_ai).

## Current features

- Text commands and speech-to-text from the app screen
- Open common Android apps and URLs
- Optional Gemini AI using a key entered by the user and encrypted with Android Keystore
- Spoken responses through Android Text-to-Speech
- A separate full-control build with an opt-in Accessibility service for user-requested cross-app actions

## Distribution builds

The project now has two explicit variants:

- **Safe sideload (safeDebug)** — the default GitHub Actions artifact. It does not declare or package the Accessibility service, so it cannot read other apps' screen text, tap their controls, type into other apps, or invoke global Back/Home actions. It can still open apps and websites, handle voice input, and use configured AI.
- **Full control (fullControlDebug)** — includes the Accessibility service. Accessibility can expose sensitive screen content and interact with other apps. Android/Google may block an internet-sideloaded APK with this capability. This variant is built for development/review but is not uploaded as the standard workflow artifact.

The GitHub Actions workflow compiles both variants and uploads only the safe sideload APK. This is a risk-reduction change, not a guarantee that Google Play Protect will approve the APK. Only Google can confirm or correct a Play Protect classification.

## Project structure

- app/ — native Android app (Kotlin)
- AssistantCommandRouter.kt — Kotlin command parser
- AssistantAccessibilityService.kt — cross-app assistance implementation, registered only by the full-control variant
- app/src/fullControl/AndroidManifest.xml — Accessibility service declaration for full-control builds
- python-reference/ — source modules copied from the original Python project for migration and comparison
- ANDROID_MIGRATION.md — porting status, boundaries, and build notes

## Build

Open the project in Android Studio, sync Gradle, and run it on an Android device or emulator.

- Safe variant: gradle assembleSafeDebug
- Full-control variant: gradle assembleFullControlDebug

Microphone access is requested only when voice input is used. Cross-app assistance is available only in the full-control variant and requires the device owner to manually enable it in Android Accessibility settings.

## Privacy and security notes

- The service does not log accessibility events in the background. Full-control commands are user-initiated through the visible assistant interface.
- When you ask the configured Gemini provider a question, that prompt is sent to Google over HTTPS for processing. Do not send passwords, financial details, or other sensitive information.
- The Gemini API key is entered by the user, encrypted with Android Keystore, and is not committed to GitHub or embedded as a repository constant.
- No API keys, local databases, private memory files, recorded audio, or executable archives are intentionally included.
- A successful build confirms compilation only; it is not a malware verdict or a Play Protect approval.
