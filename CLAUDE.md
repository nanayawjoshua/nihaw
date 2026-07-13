# Nihaw — Claude/OpenCode Instructions

## Project Overview

Nihaw is an Android floating overlay app that reads Mandarin text from any app (1688, Alipay, WeChat) and translates it on-screen using ML Kit offline translation. Architecture: screenshot/accessibility capture → OCR → ML Kit zh→en → transparent overlay display.

## Key Decisions

1. **Pure Kotlin/Android** — No Go/gomobile bridge. Standard Gradle build.
2. **Accessibility Service** primary capture path, OCR fallback for WeChat Canvas rendering
3. **ML Kit offline translation** (zh→en) as primary engine, Google Translate intent as online fallback
4. **Floating bubble** (draggable) + transparent overlay panel (~50% dark)
5. **User in Ghana** — offline-first for slow/unreliable internet

## Current Focus

Building a prototype to validate ML Kit zh→en translation quality on real 1688/Alipay screenshots before committing to full architecture.

## Session Workflow

1. Read `given_circumstances/current_scene.md` first
2. Check `given_circumstances/lessons-learned.md` before debugging
3. Run session-start hook
4. Do the work
5. Update `current_scene.md`
6. Run session-stop hook

## Important Contexts

- Android SDK at `/mnt/c/Users/user/AppData/Local/Android/Sdk` (Windows side)
- WSL ADB: `/usr/bin/adb` (platform tools on Linux side)
- Java 17, Gradle available on Linux
- GitHub: `nanayawjoshua` (authenticated via `gh`)
- GPG signing: not configured

## Commands

```bash
# Build
./gradlew assembleDebug

# Install + run on device
adb install -r android/app/build/outputs/apk/debug/app-debug.apk

# Filtered logcat
adb logcat -s NihawTranslate

# Tests
./gradlew test
```
