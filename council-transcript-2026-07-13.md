# LLM Council Transcript — Nihaw Design Spec

**Date:** 2026-07-13  
**Question:** Vet the Nihaw design spec — an Android floating overlay app that translates Mandarin text on-screen using ML Kit offline translation + Accessibility Service for text capture. Is this the right approach? What are the gaps, risks, and improvements needed?

---

## Original Question

"Use council skill to vet the Nihaw design spec."

## Framed Question

DESIGN SPEC TO VET: Nihaw — An Android floating overlay app that translates Mandarin text on-screen.

User story: A user in Ghana buys from 1688 (Chinese wholesale), pays via Alipay, and communicates with suppliers on WeChat — all in Mandarin. They don't understand a single character. Currently they copy-paste into Google Translate. Nihaw eliminates copy-paste.

Core architecture:
- Android Accessibility Service captures all Chinese text from any app's UI tree
- ML Kit on-device translation (zh→en, ~30MB model, offline, free)
- Floating bubble (like Messenger Chat Heads) → tap opens transparent overlay panel
- Overlay shows scrollable list: Chinese (gray) + English (white) per row
- Tap any row → inline popup at the text's original screen position
- "Translate Online" button for tricky sentences (opens Google Translate intent)
- ~50% transparent dark overlay background
- No Go backend, pure Kotlin/Android app
- minSdk 26, compileSdk 34
- Permissions: SYSTEM_ALERT_WINDOW, BIND_ACCESSIBILITY_SERVICE, FOREGROUND_SERVICE

What's at stake: This is a real daily-use tool the user needs for cross-border commerce. Wrong architecture choices waste development time and deliver a frustrating product.

---

## Advisor Responses

### The Contrarian

Here's the hard truth: this app has a fundamental blindness problem that no amount of polish can fix.

**The weakest link is Android's Accessibility Service on Chinese apps.** WeChat chat bubbles use Canvas-based custom rendering. 1688 product pages are WebViews, often with virtual DOMs that expose zero accessibility nodes. Alipay payment confirmations render as images in some flows. The spec admits this with "WeChat-specific WebView support" as a *future consideration* — meaning v1 will simply miss the text the user actually needs. The accessibility tree in these apps is a ghost town.

**ML Kit on-device (zh→en) is NOT Google Translate.** The user is comparing their current workflow (full Google Translate) against ML Kit's stripped 30MB model, which is optimized for basic phrases, not commerce vocabulary. "MOQ 500, FOB Shanghai, T/T payment" — ML Kit will hallucinate or return gibberish. Trusting on-device translation for cross-border commerce negotiations is a category error. The only escape hatch ("Translate Online") requires a browser tab, which means the user still copy-pastes — defeating the entire premise.

**The 50% dark overlay will be unusable** over dense Chinese UIs where users need to see both the original layout context *and* the translation simultaneously.

**The real fix:** Make OCR-based capture (screenshot → ML Kit Text Recognition → translate) the *primary* path instead of accessibility, since it works on any app regardless of rendering engine. Push the accessibility listener to "nice bonus." Without this, you're shipping a product that works beautifully on Chrome Settings but silently fails on the three apps the user actually needs.

---

### The First Principles Thinker

You're building a battleship to cross a stream.

**The real problem**: Chinese text is rendered on screen and the user can't read it. Copy-paste is the pain point, not "I need a beautiful overlay."

**Assumption that breaks first**: Accessibility Service + Chinese app UI trees. WeChat, Taobao, and 1688 all use heavy custom rendering — WebViews, canvas-drawn text, native rendering surfaces. The Accessibility Service will return garbled or partial text from these apps. ML Kit's on-device translation is also weakest on zh→en compared to cloud APIs.

**Three simpler paths you're ignoring**:

1. **Screenshot + OCR**: A foreground service that watches the screen, snapshots on a trigger (e.g., notification shade toggle), runs ML Kit's own Text Recognition (which *works on images*, not just accessibility trees), overlays translations. Works with every app, no accessibility quirks, same ML Kit model.

2. **Clipboard watcher + system toast**: Far simpler — monitor clipboard, when Chinese text detected, show a persistent notification or toast with the translation. No overlay permissions, no bubble, no accessibility service. Tiny app, ships in days.

3. **Modified keyboard** (InputMethodService): Replace the system keyboard with one that has a "translate last sentence" button. Zero permissions beyond keyboard. Works in every text field. No overlay conflicts.

The floating overlay + accessibility service combo introduces maximum complexity for maximum fragility. You'll spend 80% of your time handling edge cases where Chinese apps don't expose text properly, when a screenshot-based trigger would give you 95% reliability with 20% of the code. Strip the architecture to screenshot capture → OCR → overlay. Drop the accessibility service.

---

### The Expansionist

You're thinking too small. This isn't a Mandarin→English tool for one Ghanaian user — it's a universal on-screen translation platform for the entire Global South.

**Platform play, not a feature.** Same architecture, zero code changes: ML Kit supports 59 languages. Launch v2 with French, Portuguese, Arabic — suddenly Nihaw serves all of West Africa (300M+ people) reading Chinese commerce apps. The *reverse* is equally huge: English-speaking traders across Africa reading 1688 listings. That's the wedge.

**The "Translate Online" button is where you leave money on the table.** Google intent = zero revenue. Build a premium tier: cloud translation (Google Cloud Translation API, DeepL, or your own model) for $4.99/mo. Better quality, longer text, no URL redirect. The free tier proves value; the paid tier captures it.

**OCR is your unlock, not a footnote.** Product photos, labels, screenshots, scanned contracts — that's where the Chinese text actually lives in commerce. Add ML Kit's on-device OCR and you triple your surface area. The "medium priority" label is wrong — this should be v1.5.

**Adjacent markets are massive.** Japanese Amazon/Rakuten shoppers. Korean Coupang/Naver. Thai Shopee/Lazada. Vietnamese. Hindi. Same app, different language pack download. You're sitting on a horizontal platform that every non-English speaker with a smartphone needs.

**WeChat WebView support?** Ship it v1.1 — that's where the actual WeChat commerce happens. Don't wait.

---

### The Outsider

Three things jump out immediately:

**"This app can read everything on my screen."** That's the pitch, but also the problem. You're asking someone in Ghana to install an app that requires them to literally flip a switch in system settings labeled "allow this app to see all your screen content" — and the only promise is "trust us, we're not exfiltrating anything." There's zero mention of a privacy model, data handling policy, or audit mechanism. That's a trust wall, not a technical one.

**Four setup steps before a single translation.** Enable overlay permission. Enable accessibility. Download a 30MB model. Then start the service. In Ghana, where mobile data is expensive and slow, that 30MB download is a real pain point. And the only fallback mentioned is one open question: "retry logic?" — not even answered. What happens when download fails? App is useless until it succeeds.

**"Inline popup at original text position" sounds magical, and probably is.** The spec says it uses screen bounds from `AccessibilityNodeInfo`. But most Chinese apps (especially WeChat and 1688) render text inside custom Views or WebViews — the accessibility tree doesn't always provide reliable bounding boxes. I'd bet this feature frequently shows the popup at the wrong screen location or not at all. There's no mention of what the user sees when positioning fails.

Also: what happens to touch events on the overlay? Can I still scroll the app behind it, or is the whole screen frozen? The spec doesn't say.

---

### The Executor

**Risk #1 is Accessibility Service capture.** Many Chinese apps use custom views (WebView, SurfaceView, native rendering) that don't expose text through `AccessibilityNodeInfo`. WeChat, 1688, and Alipay all do this. **Build the accessibility logger first** — a headless service that dumps `getRootInActiveWindow()` text to logcat. Install it, open each target app, and check what comes through. If the answer is "nothing useful," the whole spec is dead.

**Risk #2 is ML Kit download.** ~30MB on Ghanaian mobile networks is painful but workable. ML Kit downloads models lazily on first translation call, not at install time. Test this: bundle a minimal activity that calls `Translator.downloadModelIfNeeded()` and time it on a real device on an MTN Ghana / Vodafone connection. If it times out repeatedly, you need a fallback (on-device model bundled in APK, or edge-case Google Translate intent for everything).

**Build order:**

1. Accessibility logger app (validate text capture) — 1 day
2. If pass → single-activity app that translates a hardcoded Chinese string via ML Kit and prints result — 1 day
3. Accessibility Service → Translation pipeline → write to a file/log — 2 days
4. Notification shortcut (skip floating bubble in v1 entirely) — 1 day
5. Floating bubble + overlay — only if notification UX proves insufficient

**Verdict:** Kill the floating bubble in v1. A notification with "Translate this screen" action is faster to build, doesn't need `SYSTEM_ALERT_WINDOW`, and avoids the overlay compositing jank on Android 10+. Ship notification-first, add bubble overlay as v1.1.

---

## Peer Reviews

### Reviewer 1

1. **E is strongest.** It's the only response that says "test before building" — validate accessibility capture on real Chinese apps and ML Kit download on Ghanaian networks. Its iterative build order (notification-first, skip overlay) is the most capital-efficient path to learning.

2. **B has the biggest blind spot.** It assumes the architecture works and jumps to "59 languages, Global South, premium tier" — never questioning whether the Accessibility Service can capture text from WeChat/1688 at all. It's scaling a foundation that might be sand.

3. **All five missed** Android's regulatory and distribution risk. Since Android 10+, `SYSTEM_ALERT_WINDOW` (overlay) is heavily restricted by Play Store policy. Accessibility Services not used *for* accessibility — i.e., helping disabled users — face immediate removal from Play. This app's core mechanism (reading all screen content for translation) is *functionally identical to spyware* in Google's policy framework, regardless of intent. Getting this app listed is harder than building it.

### Reviewer 2

1. **Strongest: E.** The only response that prescribes a concrete validation gate (build the accessibility logger first). Correctly identifies the existential risk (AS failing on Chinese apps) and gives a test before committing. Practical.

2. **Biggest blind spot: B.** Assumes the core architecture works and wants to scale globally. Misses that the Accessibility Service might yield zero text from the target apps — the prerequisite doesn't hold.

3. **What all five missed: Google Play policy.** Granting overlays and Accessibility Service to scrape screen text for translation violates the "Deceptive Behavior" clause. This architecture is effectively unpublishable on Google Play without major redesign — an existential distribution risk.

### Reviewer 3

1. **E is strongest.** It identifies the kill-risk (accessibility tree failure) and prescribes the exact falsification test (accessibility logger → logcat dump) before writing a line of overlay code. Every other response speculates; E says "go prove it works or pivot."

2. **A's biggest blind spot:** Screenshot+OCR as a drop-in replacement for accessibility service. A ignores that OCR also fails on Chinese apps — WebView text, custom fonts, small UI labels. A swaps one fragile path for another without proving OCR works either.

3. **All five missed: business model viability.** Nobody asked: "Will a Ghanaian trader pay for this when Google Translate already exists as a free overlay (Tap to Translate)?" The value prop vs. the free incumbent is entirely assumed, never challenged.

### Reviewer 4

1. **A is strongest.** It names the core failure mode — Accessibility Service is brittle on Chinese apps (WebViews, Canvas, custom rendering) — and offers three concrete fallback paths. "Battleship to cross a stream" is the right call.

2. **B has the biggest blind spot.** Every other response flags the Accessibility Service as a likely showstopper. B completely ignores this and blithely speculates about 59-language expansion and premium tiers on an architecture that may not work for one language.

3. **All five missed**: the user already has this — Google Lens and Google Translate's "Tap to Translate" overlay ship on most Android phones, work with OCR, and require zero install/download. The council should have addressed "why does this need to be a separate app at all?" before debating architecture.

### Reviewer 5

1. **E** — It's the only response that prescribes a concrete validation step (accessibility logger → test on real devices) before committing to architecture. Every other response either critiques without a path forward (A, D) or assumes the architecture works (B, C).

2. **B** — Premature scaling. B assumes "same architecture, zero code changes" for a platform expansion without validating that the architecture works on any single Chinese app. The global South strategy is moot if the service silently returns garbage from WeChat's Canvas rendering.

3. **The interaction bind**: none of the five resolved how the overlay works during use. When a translation covers half the WeChat chat view, how does the user tap the send button? Scroll the conversation? Dismiss the overlay without losing the translation? Every response treats the overlay as a display surface without addressing that it's also an interaction blocker on the very apps the user needs to operate.

---

## Chairman Synthesis

### Where the Council Agrees

**Accessibility Service will likely fail on WeChat, 1688, and Alipay.** Four of five advisors independently converged: these apps use Canvas-based custom rendering, WebViews, and SurfaceViews that expose little-to-no text through `AccessibilityNodeInfo`. The spec's architecture has an existential dependency on a component that hasn't been validated against the actual target apps.

**Screenshot + OCR is the more reliable capture path.** Three advisors independently suggested ML Kit Text Recognition on screenshots as the primary mechanism. It works regardless of rendering engine and sidesteps the accessibility tree problem entirely.

**ML Kit zh→en quality for commerce vocabulary is unproven.** Multiple advisors flagged that the 30MB on-device model handles basic phrases — not "MOQ 500, FOB Shanghai, T/T payment." The user would be trusting life-altering cross-border transactions to a stripped model.

**The overlay has serious UX unknowns.** How touch events propagate, how scrolling works, and how the user dismisses the panel without losing state were all raised independently.

### Where the Council Clashes

**Screenshot+OCR vs. Accessibility Service as primary path.** The Contrarian argues OCR should be *the* path with accessibility as a bonus. The Executor and others treat it as a backup. This matters for architecture: one triggers on screen change events, the other on explicit user action. The right answer depends on how often the user needs translations — occasional screenshots or continuous reading.

**Notification-first vs. floating bubble.** The Executor makes a strong case for shipping a notification action in v1 (no overlay permission, no compositing jank, simpler code). The spec assumes the bubble is essential. This is genuinely unresolved because nobody tested whether "pull down notification, tap translate" adds enough friction to break the workflow.

**Global platform vs. single-user tool.** The Expansionist alone argues this is a play for 59 languages and the Global South. Every other advisor and every peer reviewer calls this premature. The right call depends on capital — if this is a solo dev, build for one user. If funded, the platform play changes ROI calculations.

### Blind Spots the Council Caught

**Google Play policy is a probable kill switch.** Zero of five advisors mentioned it. Peer reviewers flagged it unanimously: overlay + accessibility service scraping screen content for non-accessibility purposes matches Google's "Deceptive Behavior" policy verbatim. This app may be unpublishable on Play without a redesign. Side-loading in Ghana changes the distribution calculus, but removes automatic updates and trust signals.

**Google Translate already ships on most Android phones with "Tap to Translate" overlay.** Peer reviewers punctured the premise: the user's current workflow is a UX gap, not a technical one. Why install a 30MB model when Google already has the overlay + cloud translation + OCR with zero setup? The answer can't be "on-device privacy" — the spec doesn't address privacy at all.

**Business model against free alternatives.** Nobody in the first round asked whether a Ghanaian trader pays $4.99/mo when Google Translate is already free. Peer review exposed this.

**Touch event handling is undefined.** The Outsider asked: can the user scroll the app behind the overlay? What happens to taps? The spec is silent.

**30MB download on Ghanaian mobile networks.** The Executor raised the concrete pain point. ML Kit downloads lazily on first call. If that call times out, the app is bricks. This was an open question in the spec.

### The Recommendation

**Kill the Accessibility Service as primary capture. Replace with screenshot-trigger → ML Kit Text Recognition → overlay panel.** This single change addresses the existential risk, works on all three target apps, and side-steps Play Store policy (no accessibility permission needed). It also simplifies the permission ask from four steps (overlay + accessibility + model download + service start) to two (notification + storage/reading).

**Ship notification-first, not bubble-first.** A notification with "Translate Screen" that triggers screenshot → OCR → overlay. No `SYSTEM_ALERT_WINDOW` permission in v1. No overlay compositing bugs. You can add the bubble in v1.1 if the notification action proves too slow.

**Validate before building.** The Executor's two-day validation sprint is non-negotiable:
1. Test ML Kit Text Recognition (`TextRecognition.getClient`) on real screenshots of 1688 listings, WeChat messages, and Alipay confirmations. Count how many fields return usable Chinese text.
2. Time the ML Kit zh→en model download on a real device tethered to an MTN Ghana connection. If it exceeds 30 seconds, bundle the model in the APK or switch to a Google Translate intent fallback.

**Don't try to out-Google Google.** This app wins on *integration* — one tap captures, translates, and overlays — not on translation quality. The cloud translation premium tier is a distraction. Users on Ghanaian mobile budgets won't pay for what they already get free via Lens.

### The One Thing to Do First

**Build the OCR logger app: install on a real Android device, screenshot each of the three target apps (WeChat, 1688, Alipay), run ML Kit Text Recognition, and dump recognized text + confidence scores to logcat.** If this doesn't return clean, actionable Chinese text across all three apps, the entire premise is dead and you need to reconsider the problem entirely (maybe a modified keyboard approach or a desktop companion). If it does, you have a validated path forward with zero architecture risk.
