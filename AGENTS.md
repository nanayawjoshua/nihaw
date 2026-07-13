# Nihaw — Agent Instructions

**Product Kit v1.0.0**

Before starting any task, read:
- `given_circumstances/current_scene.md`
- `.agents/state.json`

Current phase: **Environment Setup + ML Kit Prototype**

## Hooks (Session Lifecycle)

| Hook | When | What |
|------|------|------|
| `.agents/hooks/session-start.sh` | Session start | Reads state.json + scene, injects context |
| `.agents/hooks/session-stop.sh` | Session end | Captures decisions, updates state.json |

**Rules:**
- Read `.agents/state.json` on startup
- Check `given_circumstances/lessons-learned.md` before debugging
- Append to `given_circumstances/current_scene.md` after every session

## Required Skills

- **brainstorming** — Always before building anything new
- **subagent-driven-development** — For parallel independent tasks
- **lessons-learned** — After every bug fix or debugging session
- **requesting-code-review** — Before any code merge to main
- **verification-before-completion** — Verify work before claiming done
- **systematic-debugging** — For any bugs/issues

## Project Structure

```
├── AGENTS.md
├── CLAUDE.md
├── .agents/           # Session hooks + state
├── given_circumstances/  # Scene + lessons learned
├── docs/
│   └── superpowers/
│       └── specs/     # Design documents
├── android/           # Production Android app
│   └── app/
│       └── src/main/java/com/nihaw/translate/
├── prototype/         # Quick prototype for ML Kit validation
│   └── app/
│       └── src/main/java/com/nihaw/prototype/
└── scripts/           # Build + device debugging scripts
```

## Language Stack

**Kotlin / Android (no Go/gomobile)**
- compileSdk: 34, minSdk: 26
- ML Kit Translate: `com.google.mlkit:translate:17.0.+`
- Jetpack Compose optional (XML layouts for overlay)
- Standard Gradle build (no gomobile bridge)

## Build & Debug

```bash
# Build debug APK
./gradlew assembleDebug

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat -s NihawTranslate

# Run unit tests
./gradlew test
```

## Security Guidelines

- **NEVER commit `.env` or secrets to git**
- Hardcoded keys in APK cannot be rotated without rebuild; keep only what's necessary
- ML Kit translate models ~30MB; downloaded on first run, cached locally
- All processing on-device — no server-side translation
- Rotate secrets immediately if committed to git history

## Phase Completion Protocol

1. Update AGENTS.md with phase status
2. Document in `given_circumstances/current_scene.md`
3. Commit + push to GitHub
4. Create release tag
