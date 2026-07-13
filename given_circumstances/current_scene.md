# Nihaw — Current Scene

## Opening Scene — 2026-07-13

**Project:** Nihaw
**Stack:** Kotlin/Android (ML Kit, Accessibility Service, WindowManager overlay)
**Agents:** 1 (OpenCode)

**Product:** Android floating overlay app that reads Mandarin text from 1688/Alipay/WeChat and translates it on-screen via ML Kit offline translation.

### Current Phase: Environment Setup + Prototype

**Done:**
- Design spec written: docs/superpowers/specs/2026-07-13-nihaw-design.md
- LLM Council vetted architecture — 4/5 flagged Accessibility Service risk on WeChat
- Research confirmed: WeChat 8.0.52+ node obfuscation exists but workaroundable; ML Kit quality underestimated by council; Google Play policy non-issue; Tap to Translate still requires copy-paste
- Agent-reach research completed — architecture is sound
- GitHub CLI authenticated (nanayawjoshua)
- Product Kit bootstrapped (hooks, state, lessons-learned)

**Decision:** Build prototype first to validate ML Kit zh→en on real screenshots before finalizing architecture. WeChat gets OCR fallback.

### Next Steps
1. Initialize git repo + create GitHub repo
2. Create AGENTS.md + CLAUDE.md
3. Set up Android Gradle build with ML Kit dependency
4. Build prototype: screenshot capture → OCR → ML Kit translate → display result

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
