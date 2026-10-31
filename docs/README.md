# StreamPlus Technical Documentation

Welcome to the comprehensive technical documentation for **StreamPlus**, an enterprise-grade IPTV and media streaming application for Android.

---

## Documentation Index

1. [**System Architecture (`ARCHITECTURE.md`)**](ARCHITECTURE.md)
   * High-level layered architecture (MVVM, Clean Architecture, Unidirectional Data Flow).
   * Core architectural pillars: Presentation, Domain, Data, Media Engine.
   * Dagger Hilt dependency injection graph.
   * AndroidX Media3 (ExoPlayer) playback architecture & Widevine DRM pipeline.
   * Persistence strategy (Room SQLite, DataStore Preferences).
   * Android 15 16 KB page-size compatibility and native memory alignment.

2. [**System Workflows (`WORKFLOW.md`)**](WORKFLOW.md)
   * End-to-end Playlist Ingestion & Chunked Room Persistence workflow.
   * Video & Live Stream Playback Lifecycle with DRM resolution sequence.
   * Touch Gesture & HUD Controls state machine (volume, brightness, touch-slop thresholds).
   * Local media filtering workflow (video frame extraction, audio files > 300 KB management).
   * Return-from-player orientation locking sequence.

3. [**Codebase Audit & Improvement Roadmap (`IMPROVEMENTS.md`)**](IMPROVEMENTS.md)
   * Deep technical analysis of the codebase.
   * Recommendations for decomposing `PlayerActivity.kt` into dedicated controllers.
   * Playback pre-warming and player instance pooling opportunities.
   * EPG XMLTV streaming parsing optimizations.
   * UI accessibility and CI/CD verification recommendations.
