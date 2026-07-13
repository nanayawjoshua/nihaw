# Nihaw — Current Scene

## Opening Scene — 2026-07-13

**Project:** Nihaw
**Stack:** Kotlin/Android (ML Kit, Accessibility Service, WindowManager overlay)
**Agents:** 1 (OpenCode)

**Product:** Android floating overlay app that reads Mandarin text from 1688/Alipay/WeChat and translates it on-screen via ML Kit offline translation.

### Current Phase: Environment Setup + ML Kit Prototype

**Done:**
- Design spec written: docs/superpowers/specs/2026-07-13-nihaw-design.md
- LLM Council vetted architecture — 4/5 flagged Accessibility Service risk on WeChat
- Research confirmed: WeChat 8.0.52+ node obfuscation exists but workaroundable; ML Kit quality underestimated by council; Google Play policy non-issue; Tap to Translate still requires copy-paste
- Agent-reach research completed — architecture is sound
- GitHub CLI authenticated (nanayawjoshua)
- GitHub repo created: github.com/nanayawjoshua/nihaw
- Product Kit bootstrapped (hooks, state, lessons-learned)
- AGENTS.md + CLAUDE.md created
- Git repo initialized, initial commit pushed to main
- Android project skeleton created (build.gradle, settings, manifest, layouts)
- Prototype app for ML Kit zh→en validation (text input → translate → display)
- **Both APKs build successfully** — android/ and prototype/

**Decision:** Build prototype first to validate ML Kit zh→en on real screenshots before finalizing architecture. WeChat gets OCR fallback.

### Next Steps
1. ~~Initialize git repo + create GitHub repo~~ ✅
2. ~~Create AGENTS.md + CLAUDE.md~~ ✅
3. ~~Set up Android Gradle build with ML Kit dependency~~ ✅
4. Install prototype APK on device and test ML Kit zh→en with real Chinese text
5. Add screenshot capture + OCR pipeline to prototype

---

## Session Log

## 2026-07-13 — OpenCode

**Status:** COMPLETED
**Branch:** main

Done:
- Loaded brainstorming skill, interviewed user on product scope
- Wrote design spec with 3 layout options, auto+tap modes, bubble behavior
- Ran LLM Council (5 advisors + peer review + chairman synthesis)
- Generated council-report-2026-07-13.html and council-transcript-2026-07-13.md
- Researched council's claims via agent-reach (Accessibility Service reliability, ML Kit quality, Google Play policy, Tap to Translate comparison)
- Product Kit bootstrapped with hooks, state, lessons-learned

Found:
- WeChat 8.0.52+ node obfuscation confirmed, but workarounds exist
- ML Kit uses same models as Google Translate offline — better than council claimed
- Google Play policy permits non-accessibility Accessibility Service usage with declaration
- Tap to Translate still requires copy-paste — Nihaw's value prop is real

Decision: Build prototype first. Validate ML Kit zh→en on real screenshots.

## 2026-07-13 20:00 — OpenCode

**Status:** COMPLETED
**Branch:** main

Done:
- Authenticated GitHub CLI (nanayawjoshua), created github.com/nanayawjoshua/nihaw
- Initialized git repo, first commit pushed to main
- Created AGENTS.md + CLAUDE.md for project
- Bootstrapped Product Kit: .agents/hooks, state.json, given_circumstances/
- Created Android project: build.gradle, settings, manifest, layouts, icon
- Created prototype app: ML Kit zh→en text translation input
- Fixed build errors: vector icon, Translator async API
- **Both android/ and prototype/ build successfully** (two debug APKs)

Next:
- Install prototype on device, test ML Kit quality with real 1688 screenshots
- Add screenshot capture + OCR to prototype
