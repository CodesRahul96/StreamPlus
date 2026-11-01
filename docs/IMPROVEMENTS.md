# StreamPlus Codebase Audit, Improvements & Tomorrow's Agenda

This document tracks completed optimizations, architectural audits, and the strategic roadmap for StreamPlus.

---

## 1. Recently Completed Improvements (Verified & Landed)

### 1.1 Single-Pass Pre-Warmed Playback (Resolved)
* **Issue**: Channels were loading/buffering twice whenever opened on the player.
* **Root Cause**: `PlayerScreen` mounted with default `AppSettings()` while DataStore was still loading preferences asynchronously. When DataStore finished reading 40ms later, preferences like `enableTunneling` or `decoderPriority` emitted, causing Compose to recreate `ExoPlayer`, aborting the first buffer and reloading the channel from scratch.
* **Resolution**: Pre-loaded `initialSettings` synchronously with channel metadata on `Dispatchers.IO` in `PlayerActivity.onCreate` before `setContent` is called. Frame 0 mounts with the user's exact preferences; `ExoPlayer` is created, prepared, and buffered strictly once.

### 1.2 Vertical Opposite-Side Gesture HUDs (Resolved)
* **Requirement**: Replace the central card popup with sleek vertical indicator bars placed along the screen edge, showing the indicator on the opposite side of the gesture touch.
* **Resolution**:
  * Left touch (Brightness) slides in the vertical HUD bar on the **Right edge** (`Alignment.CenterEnd`).
  * Right touch (Volume) slides in the vertical HUD bar on the **Left edge** (`Alignment.CenterStart`).
  * Ergonomic advantage: Completely prevents the user's hand or thumb from obstructing the visual feedback.
  * Added vertical progress gradient tracks, contextual icons (`BrightnessHigh/Medium/Low`, `VolumeUp/Down/Off`), percentage labels, and safe auto-hide.

### 1.3 Player Controls & Lock Button Collision (Resolved)
* **Issue**: In portrait mode, the lock button overlay overlapped directly with the center rewind/play controls.
* **Resolution**: Repositioned lock toggle to `Alignment.TopStart` (`start = 20.dp, top = 84.dp`) below the top bar, reduced custom controller bottom margin to 20dp, and added automatic hiding when gestures are active.

### 1.4 Back-Button Navigation Hierarchy (Resolved)
* **Issue**: Pressing back in `PlaylistDetailScreen` or secondary tabs would occasionally exit the app.
* **Resolution**: Implemented step-by-step unwinding in `PlaylistDetailScreen` (clears search query -> resets group to "All" -> resets stream type to "live" -> pops backstack). In `MainScreen`, back from non-playlist tabs redirects to the Playlists tab before prompting exit.

### 1.5 Theme Accent Colors with Ocean Blue Default (Resolved)
* **Requirement**: Add theme accent color customization with Ocean Blue as default.
* **Resolution**: Created `AccentColor` enum (`BLUE` [Default], `VIOLET`, `EMERALD`, `CRIMSON`, `AMBER`, `CYAN`, `ROSE`), integrated dynamic Material 3 color generation in `Theme.kt`, and added an interactive swatch selector in Settings -> Appearance.

### 1.6 Startup & Playlist Empty Flash Elimination (Resolved)
* **Issue**: Empty playlist or "No channels found" state flashed briefly before data loaded.
* **Resolution**: Hoisted `playlistViewModel` in `MainActivity`/`MainScreen`, added immediate Room cache prefill in `PlaylistDetailViewModel` upon `init`, and added an `isAwaitingFiltering` guard in `PlaylistDetailScreen`.

### 1.7 Google Cast Manifest Configuration (Resolved)
* **Issue**: `CastOptionsProvider` existed in code but missed its `<meta-data>` entry in `AndroidManifest.xml`.
* **Resolution**: Added `com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME` metadata tag into `AndroidManifest.xml`.

### 1.8 Playlist Group Logo Support & Room Migration 20 (Resolved)
* **Requirement**: Support group logos for playlists that specify them, showing them in the UI without breaking any existing playback, search, or database schema.
* **Resolution**:
  * **Parser**: Added parsing of `#EXTINF` attributes (`group-logo`, `group_logo`, `group-icon`, `tvg-group-logo`), `#EXTGRP` tags (`#EXTGRP:Name,logo="..."`, `#EXTGRP-LOGO:`, `#EXTIMG:`), and JSON playlist category logos (`group_logo`, `category_logo`). Added a group logo cache with retroactive backfilling across channels sharing the same group.
  * **Room Schema & Migration**: Added non-destructive `MIGRATION_19_20` with `ALTER TABLE channels ADD COLUMN groupLogo TEXT NOT NULL DEFAULT ''` and updated schema to version 20 (`20.json`).
  * **UI Integration**: Surfaced group logos with rounded clipping and clean fallback to folder icons in the Category Selection Bottom Sheet, Category Chips LazyRow, TopAppBar active category dropdown, Channel list/grid items, and `PlayerActivity` quick channel drawer.

---

## 2. Tomorrow's Working Agenda & Action Items

When resuming work tomorrow, focus on these prioritized initiatives:

### 2.1 Refactor & Decompose `PlayerActivity.kt`
* **Goal**: Break down the 3,500+ line `PlayerActivity.kt` into clean, maintainable modular components:
  1. `PlayerGestureHandler`: Separate touch listener, double-tap seek, and opposite-side gesture HUD state into its own controller.
  2. `PlayerControlsOverlay`: Standalone composable for center media controls, live watch timer, and top app bar.
  3. `PlayerTrackManager`: Audio, subtitle, and video quality dialog handlers.
  4. `PlayerPipController`: Picture-in-picture parameters and aspect ratio calculations.
* **Target File**: `app/src/main/java/com/codesrahul/streamplus/ui/player/PlayerActivity.kt`.

### 2.2 ExoPlayer Instance Pre-Warming & Pool Recycling
* **Goal**: Enable near-instant channel switching (<300ms) by reusing `ExoPlayer` instances when navigating between adjacent channels or returning from detail views.
* **Approach**: Implement a lightweight `PlayerPool` or `MediaSessionService` that recycles renderers and network data source pools.

### 2.3 Incremental EPG SAX/Pull Parser
* **Goal**: Optimize large multi-day EPG XMLTV imports (>50 MB).
* **Approach**: Stream parse directly into Room in temporary batch transactions rather than holding full DOM trees in memory.

### 2.4 CI/CD 16 KB Page Alignment Checks
* **Goal**: Add an automated alignment check (`check_elf_alignment.sh` or `readelf`) in GitHub Actions to guarantee all native libraries stay 16 KB page-aligned before release builds.

---

## 3. Reference Links
* [System Architecture (`docs/ARCHITECTURE.md`)](ARCHITECTURE.md)
* [System Workflows (`docs/WORKFLOW.md`)](WORKFLOW.md)
* [Project Overview (`docs/README.md`)](README.md)
