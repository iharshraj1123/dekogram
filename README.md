# Dekogram

<p align="center">
  <img src="TMessagesProj/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" height="128" alt="Dekogram Logo" />
</p>

<p align="center">
  <strong>A high-performance Telegram Android client specialized in high-speed downloads, zero-redundancy storage, and unrestricted media saving.</strong>
</p>

<p align="center">
  <a href="https://github.com/iharshraj1123/dekogram/releases/latest"><img src="https://img.shields.io/github/v/release/iharshraj1123/dekogram?label=Latest%20Release&color=blue" alt="Latest Release"></a>
  <a href="https://github.com/iharshraj1123/dekogram/actions/workflows/build-apk.yml"><img src="https://img.shields.io/github/actions/workflow/status/iharshraj1123/dekogram/build-apk.yml?label=CI%20Build&color=green" alt="CI Status"></a>
  <img src="https://img.shields.io/badge/Architecture-arm64--v8a-orange" alt="Architecture">
  <img src="https://img.shields.io/badge/License-GPL%20v2%20or%20later-lightgrey" alt="License">
</p>

---

## Overview

**Dekogram** is an independent, power-user fork of the official Telegram Android app (`DrKLO/Telegram`). It re-engineers Telegram's download engine, media pipeline, and storage architecture to eliminate artificial speed throttling, double-storage caching waste, forced cloud auto-downloads, and content-saving restrictions—all while preserving full official MTProto compatibility, instant push notifications, and secret chat encryption.

---

## Key Features

### High-Speed Download Boost
* **Parallel Chunk Streaming**: Requests multiple file parts concurrently across dual MTProto download sockets, bypassing single-stream server throttling.
* **512 KB Chunk Sizing**: 4x larger chunk blocks compared to stock Telegram's 128 KB, significantly minimizing network round-trip request overhead on high-speed connections.
* **Smooth Throughput**: Configured out of the box with **Fast (8 Streams)** mode for steady, reliable speed, with user controls in *Settings > Data and Storage > Download Speed Boost* (Disabled / Fast / Maximum).

### Zero-Waste Single-File Storage
* **50% Storage Savings**: Stock Telegram downloads files twice—first to the hidden private app sandbox (`Android/data/...`) and again when saving to gallery. Dekogram immediately relinks the database to your public file and purges the internal cached duplicate.
* **Direct Public Export**: Completed downloads route directly to public storage upon finishing:
  * **Videos**: `Downloads/Dekogram/Videos/`
  * **Documents & Files**: `Downloads/Dekogram/`
  * **Photos**: `Pictures/Dekogram/`

### Configurable File Naming & Post Timestamps
* **Custom Video Naming**: Choose how videos are named in *Data and Storage* settings:
  * *Original File Name* (preserves creator's exact title)
  * *File ID* (clean, unique identifier)
  * *File ID + File Name*
* **Post Timestamp Synchronization**: Synchronizes filesystem `lastModified` and Android MediaStore metadata (`DATE_TAKEN`, `DATE_MODIFIED`, `DATE_ADDED`) with the **original Telegram post date**. Your gallery and media players display media in true chronological order.
* **Smart Conflict Resolution**: Replaces clunky `VID_` prefixes with compact, clean 5-digit random suffixes (`_XXXXX`) only when a filename collision occurs.

### Unrestricted Content & Screen Capture
* **Bypass Forward & Save Restrictions**: Channels and groups with `noforwards` enabled can no longer block you from saving. "Save to Gallery", "Save to Downloads", and sharing options are permanently unlocked.
* **Save Disappearing & TTL Media**: The save button remains fully accessible on expiring self-destructing photos and videos.
* **Global Screen Capture**: Cleared `FLAG_SECURE` app-wide. Screenshots and screen recording work everywhere, including restricted channels and secret chats.

### Persistent Background Downloads
* **Dedicated Foreground Service**: Equipped with an Android foreground service, `PARTIAL_WAKE_LOCK`, and high-performance Wi-Fi lock.
* **Zero Timeout Pauses**: Downloads continue uninterrupted with an ongoing notification progress bar when your screen turns off or the app is minimized.

### Multi-Account Limit (10 Accounts)
* **Native C++ & Java Limit Expansion**: Native MTProto connection management expanded from the official 3-account limit to support **10 accounts simultaneously**.

### Privacy & Clean UI Defaults
* **Hidden Stories Bar**: Completely removes the top stories bar with 0px layout shift.
* **Zero Ads & Sponsored Messages**: Sponsored messages and video ad network requests are completely blocked at the source.
* **Stripped Trackers**: Completely purged Microsoft AppCenter telemetry and Google Firebase UserActions background indexing.
* **Accidental Action Guards**: Confirmation dialogs before initiating voice/video calls or sending stickers.
* **Review Voice/Video Notes**: Releasing touch on voice or video note recording enters review draft mode instead of firing instantly.

### External Player Integration
* Added an instant **"Open in..."** option to all video menus, streaming or opening clips directly into VLC, MX Player, MPV, or your external player of choice.

---

## Dekogram vs. Stock Telegram

| Feature | Dekogram | Stock Telegram |
| :--- | :---: | :---: |
| **Download Speed Engine** | **Boosted (8–12 Parallel Streams, 512KB chunks)** | 4 Streams, 128KB chunks |
| **Download Storage** | **Single copy (Direct to public Downloads)** | Duplicated (Cache + Public copy) |
| **Restricted Content Saving** | **Permanently Unlocked** | Blocked ("Saving is restricted") |
| **Expiring / TTL Media Saving** | **Saved with 1 tap** | Blocked |
| **Screenshots & Recording** | **Allowed everywhere** | Blocked (`FLAG_SECURE`) |
| **Media File Naming** | **Configurable (Original / ID / ID+Name)** | Forced generic `VID_` prefix |
| **Post Date Sync** | **Files match original post date** | Files dated when downloaded |
| **Multi-Accounts** | **10 Accounts** | 3 Accounts (5 with Premium) |
| **Stories Bar** | **Hidden (Clean UI)** | Always visible |
| **Sponsored Messages & Ads** | **Blocked** | Displayed in channels |
| **Background Downloads** | **WakeLock foreground service** | Frequently paused in background |
| **External Video Player** | **"Open in..." menu for any clip** | Restricted / Internal player only |

---

## Installation & Updates

### Option 1: Direct Download (Recommended)
1. Download `Dekogram.apk` directly from [Latest Release](https://github.com/iharshraj1123/dekogram/releases/latest).
2. Tap the downloaded APK to install.
3. Login using your phone number or QR code.

### Option 2: Automatic Updates via Obtainium
1. Install [Obtainium](https://github.com/ImranRXZ/Obtainium).
2. Tap **Add App** and paste: `https://github.com/iharshraj1123/dekogram`
3. Obtainium will automatically track new releases and notify you when updates are available.

### Option 3: Built-In In-App Updater
* Dekogram automatically checks for GitHub releases.
* Tap the version text at the bottom of **Settings** at any time to instantly check for updates and self-install directly inside the app.

---

## License

Dekogram is licensed under the **GNU General Public License v2.0 or later** (GPLv2+). See [LICENSE](LICENSE) for details.
Based on the official [Telegram for Android](https://github.com/DrKLO/Telegram) source code by Nikolai Kudashov.
