# My Android AI

A native Android assistant starter designed to keep the normal Android system intact. It starts with a voice/text assistant screen and adds device-control capabilities only through Android's explicit, user-granted permissions.

## Current scope

- Native Kotlin + Jetpack Compose app scaffold
- Text command input and microphone permission/voice input entry point
- Accessibility service foundation for user-authorized UI assistance
- Clear privacy and permission boundaries
- No root access, stealth control, credential collection, or background surveillance

## Requirements

- Android Studio (current stable)
- JDK 17 or the version required by the selected Android Gradle Plugin
- Android SDK installed through Android Studio

## Build

Open this repository in Android Studio and sync Gradle. Build and run on an Android device or emulator. The accessibility service must be enabled manually by the device owner in Android Settings before it can interact with other apps.

## Status

Starter implementation. Voice recognition depends on Android's available speech recognition service; broad app control is not automatic and is constrained by Android permissions and each app's UI.
