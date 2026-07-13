# Nihaw — On-Screen Mandarin Translation Overlay

**Version:** 1.0  
**Date:** 2026-07-13  
**Author:** Joshua Yeboah  
**Status:** Draft Design  

---

## 1. Product Summary

Nihaw is an Android floating overlay app that reads Chinese text from any app on screen, translates it instantly via on-device ML Kit, and displays English translations in a transparent overlay panel. Built for users who don't understand Mandarin but regularly use Chinese apps (1688, Alipay, WeChat, etc.) for shopping and communication.

---

## 2. User Story

A user in Ghana buys from 1688, pays via Alipay, and negotiates with suppliers on WeChat — all in Mandarin. They don't understand a single character. Currently they copy-paste into Google Translate, which is slow and breaks the flow. Nihaw eliminates copy-paste: the user opens any Chinese app, taps a floating bubble, and sees all Chinese text translated in real-time on a transparent overlay.

---

## 3. Core Interaction Flow

```
[Chinese app open]
       │
       ▼
Floating bubble visible (always on top)
       │ Tap bubble
       ▼
Full transparent overlay opens (~50% dark)
  ├── Shows scrollable list of all text on screen
  │   └── Each row: Chinese (gray, smaller) + English (white, larger)
  ├── Tap any row → inline popup at original text position
  ├── [Refresh] button to re-capture screen
  └── [Translate Online] button → opens Google Translate intent
       │ Tap outside overlay / swipe down
       ▼
Overlay closes, bubble remains
```

---

## 4. Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Nihaw App Process                        │
│                                                              │
│  ┌─────────────────────┐    ┌────────────────────────────┐  │
│  │ AccessibilityService │    │  FloatingBubbleService    │  │
│  │                      │    │  (Foreground Service)     │  │
│  │  onAccessibilityEvent│    │                            │  │
│  │  → extract all text  │────▶  WindowManager overlay    │  │
│  │  → filter Chinese    │    │  TYPE_APPLICATION_OVERLAY │  │
│  │  → deduplicate       │    │                            │  │
│  └──────────┬───────────┘    └────────────┬───────────────┘  │
│             │                              │                  │
│             ▼                              ▼                  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                Translation Engine                       │  │
│  │                                                         │  │
│  │  ┌─────────────────────────┐  ┌──────────────────────┐  │  │
│  │  │ ML Kit On-Device        │  │ "Translate Online"   │  │  │
│  │  │ Translation (zh↔en)     │  │ → ACTION_VIEW intent │  │  │
│  │  │ Free, instant, offline  │  │   to Google Translate│  │  │
│  │  └─────────────────────────┘  └──────────────────────┘  │  │
│  └────────────────────────────────────────────────────────┘  │
│                              │                                │
│                              ▼                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │              Overlay Panel (ViewPager/RecyclerView)     │  │
│  │  - Scrollable translation list                          │  │
│  │  - Source (gray) + translation (white) per row          │  │
│  │  - Tap row → inline popup at original screen position   │  │
│  │  - ~50% transparent dark background                     │  │
│  └────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

**No Go backend.** This is a pure Android (Kotlin) application. The bridge_apk build pipeline (Gradle, gomobile) is not needed for this app. Instead we use standard Android Studio / Gradle build tooling.

---

## 5. Components

### 5.1 Accessibility Service (`NihawAccessibilityService.kt`)

| Property | Value |
|----------|-------|
| Package | `com.nihaw.translate` |
| Config | `android:accessibilityEventTypes="typeAllMask"` |
| | `android:accessibilityFeedbackType="feedbackGeneric"` |
| | `android:canRetrieveWindowContent="true"` |

**Behavior:**
- Listens for `TYPE_WINDOW_STATE_CHANGED`, `TYPE_VIEW_SCROLLED`, `TYPE_VIEW_TEXT_CHANGED`
- On event: walks the `AccessibilityNodeInfo` tree, extracts all text nodes
- Filters for Chinese-language content (Unicode range: `\u4E00-\u9FFF`, `\u3400-\u4DBF`)
- Groups text by screen bounds (x, y, width, height) for inline positioning
- Deduplicates: hashes all visible text per screen; skips if identical to last capture
- Only processes events when the overlay panel is actively shown (flag from service)

**Battery optimization:**
- Pause processing entirely when overlay panel is dismissed
- Debounce rapid events (300ms window)
- Skip events from Nihaw's own UI

### 5.2 Floating Bubble (`FloatingBubbleService.kt`)

| Property | Value |
|----------|-------|
| Type | `Foreground Service` + `WindowManager` |
| Notification | Low-priority persistent notification ("Nihaw is running") |
| View | Small circle/pill (~48dp), draggable |

**Interaction:**
- **Drag:** Move anywhere on screen (reposition on `ACTION_MOVE`)
- **Tap:** Toggle overlay visibility (show/hide)
- **Double-tap:** Quick-capture current screen → show mini floating translation window
- **Auto-snap:** On drag end, snap to nearest screen edge with margin

**Visual:**
- Icon: "N" character or translate symbol (🌐), white on translucent dark bg
- Small scale: ~48dp × 48dp
- Shadow/elevation for depth

### 5.3 Translation Engine (`Translator.kt`)

Uses ML Kit's on-device translation (`com.google.mlkit:translate`).

**Language model:**
- Source: Chinese (zh)
- Target: English (en)
- Model size: ~30MB downloaded on first run
- Fully offline after initial download

**API:**
```kotlin
class Translator(private val context: Context) {
    private val translator: Translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.CHINESE)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
    )

    suspend fun translate(text: String): String
    fun downloadModelIfNeeded(): Task<Void>
}
```

**Batching:**
- Translate all captured text blocks in parallel (coroutines)
- Cache translations keyed by text hash (avoids re-translating duplicates)
- Max 500 chars per block (ML Kit limit); split longer text

**"Translate Online" fallback:**
- Opens an Android `ACTION_VIEW` intent to Google Translate:
  ```kotlin
  val intent = Intent(Intent.ACTION_VIEW).apply {
      data = Uri.parse("https://translate.google.com/?sl=zh&tl=en&text=${Uri.encode(text)}&op=translate")
  }
  ```
- User is prompted when translation seems low-confidence (optional, configurable)

### 5.4 Overlay Panel (`OverlayPanel.kt`)

| Property | Value |
|----------|-------|
| Type | `WindowManager` `TYPE_APPLICATION_OVERLAY` |
| Permission | `SYSTEM_ALERT_WINDOW` (requested on first launch) |
| Background | ~50% transparent black (`#80000000`) |
| Layout | `RecyclerView` with vertical list of translation rows |
| Dismiss | Tap outside content area / system back / swipe down |

**Each row layout:**
```
┌──────────────────────────────────────┐
│ 欢迎光临 1688 批发平台     [Tap↗]   │ ← Chinese (gray, ~14sp)
│ Welcome to 1688...                   │ ← English (white, ~16sp)
│ ───────────────────────────────────  │ ← separator (thin line)
└──────────────────────────────────────┘
```

**Tap row → inline popup:**
- Small overlay view (~200dp × 100dp) positioned at the text's original screen bounds
- Shows only the English translation
- Auto-dismisses after 5s or on tap
- Arrow pointing to the original text position

**Header bar:**
- "← Nihaw" label (tappable to dismiss)
- [Refresh] — re-captures current screen
- [Translate Online] — sends all currently visible text to Google Translate

### 5.5 `MainActivity.kt`

| Purpose | First-launch setup |
|---------|-------------------|
| Request `SYSTEM_ALERT_WINDOW` permission | Open settings intent if not granted |
| Request Accessibility Service permission | Open accessibility settings intent |
| Download ML Kit zh→en model | Show progress bar, handle failure |
| Start `FloatingBubbleService` | After all permissions granted |

**UI:** Minimal setup screen with:
1. "Enable Overlay" → SYSTEM_ALERT_WINDOW permission
2. "Enable Accessibility" → open accessibility settings
3. "Download Chinese→English Model" → progress bar
4. "Start Nihaw" → launch foreground service

All permissions persist. Only shown on first launch or when permissions revoked.

---

## 6. Data Flow

### 6.1 Auto-Capture Flow

```
Accessibility event (scroll/new screen)
  → NihawAccessibilityService.onAccessibilityEvent()
    → extractTextFromNode(root) → List<TextBlock>
    → filterChinese(texts) → List<TextBlock>
    → deduplicate(blocks, lastHash)
    → sendToService(blocks)
      → FloatingBubbleService
        → Translator.translate(blocks) → List<Translation>
          → cacheTranslations(translations)
          → updateOverlay(translations)
```

### 6.2 Tap Row → Inline Overlay

```
User taps row in overlay panel
  → onRowClick(Translation)
    → extract original screen bounds from TextBlock
    → FloatingBubbleService.showInlinePopup(
        text = translation.english,
        x = block.bounds.left,
        y = block.bounds.top,
        width = block.bounds.width()
      )
    → auto-dismiss after 5s
```

---

## 7. Android Permissions

| Permission | Why | When Requested |
|-----------|-----|----------------|
| `SYSTEM_ALERT_WINDOW` | Draw overlay on top of other apps | First launch (settings intent) |
| `BIND_ACCESSIBILITY_SERVICE` | Read screen text from other apps | First launch (accessibility settings) |
| `FOREGROUND_SERVICE` | Keep bubble alive | Manifest-declared |
| `POST_NOTIFICATIONS` (Android 13+) | Persistent service notification | First launch |
| `INTERNET` | Google Translate intent / ML Kit model download | Manifest-declared |

---

## 8. Build Pipeline

Nihaw is a **standard Android app** — it does NOT need the gomobile/Go bridge from bridge_apk.

| Tool | Version |
|------|---------|
| Android Studio / Gradle | Latest stable |
| Kotlin | 1.9+ |
| compileSdk | 34 |
| minSdk | 26 |
| ML Kit Translate | `com.google.mlkit:translate:17.0.+` |

The bridge_apk build pipeline (WSL → gomobile → Windows NDK) is NOT needed. But the same **device debugging tools** (`adb-bridge.sh`, `adb install`, logcat) from bridge_apk's `scripts/` and AGENTS.md can be adapted.

**APK output:** `android/app/build/outputs/apk/debug/nihaw-debug.apk`

---

## 9. Testing Strategy

| Test Type | Method |
|-----------|--------|
| Unit tests | JUnit for `Translator` (mock ML Kit), text filtering |
| UI tests | Compose/Espresso for overlay rendering, row interactions |
| Accessibility | Manual: launch Chinese apps, verify text capture |
| Translation quality | Manual: compare against Google Translate for accuracy |
| Battery | `dumpsys batterystats` with overlay active for 30 min |
| Edge cases | No Chinese text on screen, very long text, special characters |

---

## 10. Future Considerations (Post-v1)

| Feature | Priority | Notes |
|---------|----------|-------|
| Screenshot OCR fallback | Medium | For Chinese text in images (product photos, scanned docs) |
| WeChat-specific WebView support | Medium | Accessibility may miss text in embedded WebViews |
| Translation history | Low | Save past translations for reference |
| Customizable overlay opacity | Low | Slider to adjust from 20%–80% |
| Dark/light mode | Low | Follow system theme |

---

## 11. Open Questions

- ML Kit zh→en model download reliability on slow networks (retry logic?)

---

## 12. Revision History

| Date | Version | Changes |
|------|---------|---------|
| 2026-07-13 | 1.0 | Initial design spec |
