# My Android AI — migration from my_ai

## Source of truth

The original Python assistant is https://github.com/Michael-aal/my_ai. Its source modules are preserved under `python-reference/` as a migration reference. The original repository is not modified.

## Migration approach

This is a native Android app, not a desktop Python process copied onto a phone. Android APIs replace Windows-only behavior, while reusable intent logic is being ported into Kotlin.

| Original Python area | Android counterpart/status |
|---|---|
| `intent_engine.py` | `AssistantCommandRouter.kt` — first native intent port |
| `action_engine.py`, `app_controller.py` | `MainActivity.kt` and `AssistantAccessibilityService.kt` — Android app launch, URLs, navigation, text and visible-label actions |
| `listener.py` | Android speech-recognition intent from the app screen; continuous wake-word listening is not implemented yet |
| `voice.py`, `TTS.py` | Android speech output is not connected yet |
| `ai_engine.py` | Preserved as reference; `AiProviderClient.kt` supports Gemini, Groq, and OpenRouter with per-provider device-encrypted key storage |
| `permissions.py` | Android runtime microphone permission and explicit manual Accessibility enablement |
| `database.py`, memory modules | Not migrated to persistent memory yet |

## Current Android capabilities

- Text commands and speech-to-text from the main app screen
- Launch common apps, search installed launcher apps by label, and open URLs
- A floating command panel after the user manually enables the Accessibility service
- User-initiated visible-label tap, focused-field text entry, screen text reading, Back and Home actions
- Time, date and battery queries
- Android Text-to-Speech responses
- AI fallback through the selected Gemini, Groq, or OpenRouter provider after the user configures a provider-specific API key in the app
- App assistance can be disabled in Android Accessibility settings

## Security and portability

- The original `.env`, local database, memory data, recorded audio, executables and binary archives are intentionally not copied.
- API keys must never be committed to this repository. The Android port stores a user-supplied Gemini key encrypted with Android Keystore; keys are not embedded in the APK. For production multi-user distribution, prefer a backend that keeps provider keys server-side.
- The Python source in `python-reference/` is a reference copy, not an Android runtime module. It contains desktop-specific code and must not be executed on Android as-is.
- Accessibility is powerful. It is opt-in, visibly controlled by the user, and should only perform actions requested through the assistant interface. Never collect screen contents in the background.
- App-specific flows can vary by Android version, language, app layout, and permissions. Sending a message or submitting a form should remain a deliberate user action.

## Build and verification

Open the repository in Android Studio, allow Gradle sync, then build and run on an emulator or Android device. This repository has not yet had a successful Android build verified in this environment.
