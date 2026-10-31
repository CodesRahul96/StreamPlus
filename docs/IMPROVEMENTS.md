# StreamPlus Deep Codebase Audit & Improvement Roadmap

This document outlines key technical opportunities, optimizations, and future enhancements identified during a comprehensive codebase review of StreamPlus.

---

## 1. High-Priority Architectural Opportunities

### 1.1 Decompose `PlayerActivity.kt` into Modular Components
* **Current State**: `PlayerActivity.kt` currently exceeds 3,400 lines of code. It coordinates ExoPlayer instance creation, track selection, sleep timers, lyrics parsing, touch gestures, picture-in-picture, and multiple bottom sheets in one class.
* **Recommendation**: Decompose into dedicated controllers and composables:
  * `PlayerGestureHandler`: Encapsulates touch listener, double-tap seek, and volume/brightness adjustments.
  * `PlayerTrackManager`: Manages audio, video, and subtitle track selector dialogs.
  * `PlayerControlsOverlay`: Standalone composable for center playback controls, scrubbers, and live badges.
  * `PlayerPipController`: Manages PiP parameters, aspect ratios, and broadcast receivers.
* **Benefit**: Dramatically reduces cyclomatic complexity, accelerates Compose preview rendering, and makes automated testing feasible.

### 1.2 Google Cast Manifest Configuration (Resolved)
* **Finding**: `CastOptionsProvider` existed in `com.codesrahul.streamplus.cast`, but its required `<meta-data>` entry was missing from `AndroidManifest.xml`, triggering runtime `IllegalStateException` on launch.
* **Status**: Resolved by adding the `OPTIONS_PROVIDER_CLASS_NAME` metadata tag into `AndroidManifest.xml`.

---

## 2. Media Playback & Engine Optimizations

### 2.1 ExoPlayer Instance Pooling & Seamless Re-use
* **Current State**: Navigating from a channel back to the playlist and opening another channel destroys and recreates the ExoPlayer instance from scratch.
* **Recommendation**: Introduce a lightweight `PlaybackService` or `PlayerPool` that holds a pre-warmed ExoPlayer instance, recycling renderers and buffer memory between rapid channel changes.
* **Benefit**: Reduces channel switching latency from ~1.2s to <400ms.

### 2.2 EPG Incremental GZIP Stream Parsing
* **Current State**: Large EPG XMLTV files are parsed and mapped in memory.
* **Recommendation**: Implement an incremental SAX/Pull parser with GZIP stream reading directly into Room using temporary tables, discarding expired programs past 48 hours.
* **Benefit**: Decreases memory footprint during large multi-day TV guide synchronizations.

---

## 3. UI, Performance & Accessibility Improvements

### 3.1 Dialog Theming Unification
* **Current State**: Historical settings dialogs used a mix of `Card` with custom glassmorphic modifiers and manual alpha overlays.
* **Recommendation**: Standardize all app dialogs on a single `StreamPlusDialog` component that guarantees solid container elevation, high-contrast borders, and accessibility labels.
* **Benefit**: Ensures visual consistency and prevents future contrast or text bleed issues.

### 3.2 Accessibility (TalkBack & Semantic Traversal)
* **Recommendation**:
  * Add explicit `contentDescription` attributes to all icon buttons in player controls (e.g., aspect ratio, resize mode, lock button).
  * Group player control icons with `Modifier.semantics { isTraversalGroup = true }` so screen readers navigate logically from left to right.

---

## 4. Stability, Toolchain & CI/CD Enhancements

### 4.1 Automated 16 KB Page-Size Verification in CI
* **Recommendation**: Add an automated step in GitHub Actions / CI pipeline using `llvm-readelf -Wl` or Google's `check_elf_alignment.sh` on built APK artifacts to guarantee that all native libraries remain 16 KB page-aligned before release builds are generated.

### 4.2 Room Database Schema Migration Versioning
* **Recommendation**: Set `exportSchema = true` in `AppDatabase` and commit Room JSON schemas to git to enable automated migration tests (`MigrationTestHelper`).
