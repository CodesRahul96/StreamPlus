# StreamPlus Technical Documentation

Welcome to the comprehensive technical documentation for **StreamPlus**, an enterprise-grade IPTV and media streaming application for Android.

---

## Documentation Index

1. [**System Architecture (`ARCHITECTURE.md`)**](ARCHITECTURE.md)
   * High-level layered architecture (MVVM, Clean Architecture, Unidirectional Data Flow).
   * Core architectural pillars: Presentation, Domain, Data, Media Engine.
   * Dagger Hilt dependency injection graph.
   * Single-pass pre-warmed playback initialization (eliminating asynchronous double-load).
   * Dynamic Theme & Color Accent system (7 vibrant palettes, Ocean Blue default).
   * AndroidX Media3 (ExoPlayer) playback architecture & Widevine DRM pipeline.
   * Persistence strategy (Room SQLite, DataStore Preferences).
   * Android 15 16 KB page-size compatibility and native memory alignment.

2. [**System Workflows (`WORKFLOW.md`)**](WORKFLOW.md)
   * End-to-end Playlist Ingestion & Chunked Room Persistence workflow.
   * Video & Live Stream Playback Lifecycle with DRM resolution sequence.
   * Touch Gesture & Opposite-Side HUD Controls state machine (volume, brightness, touch-slop thresholds).
   * Navigation & Back-Button Hierarchy unwinding sequence (zero accidental app exits).
   * Local media filtering workflow (video frame extraction, audio files > 300 KB management).
   * Return-from-player orientation locking sequence.

3. [**Codebase Audit & Improvement Roadmap (`IMPROVEMENTS.md`)**](IMPROVEMENTS.md)
   * Recently resolved and verified technical improvements.
   * **Tomorrow's Working Agenda**: Prioritized action plan for decomposing `PlayerActivity.kt`, ExoPlayer pooling, streaming EPG parsers, and CI/CD checks.
