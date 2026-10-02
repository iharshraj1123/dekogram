# Dekogram

<p align="center">
  <img src="TMessagesProj/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" height="128" alt="Dekogram Logo" />
</p>

<p align="center">
  <strong>A high-performance Telegram Android client specialized in unrestricted downloads, zero-redundancy storage, and privacy freedom.</strong>
</p>

<p align="center">
  <a href="https://github.com/iharshraj1123/dekogram/releases/latest"><img src="https://img.shields.io/github/v/release/iharshraj1123/dekogram?label=Latest%20Release&color=blue" alt="Latest Release"></a>
  <a href="https://github.com/iharshraj1123/dekogram/actions/workflows/build-apk.yml"><img src="https://img.shields.io/github/actions/workflow/status/iharshraj1123/dekogram/build-apk.yml?label=CI%20Build&color=green" alt="CI Status"></a>
  <img src="https://img.shields.io/badge/Architecture-arm64--v8a-orange" alt="Architecture">
  <img src="https://img.shields.io/badge/License-GPL%20v2%20or%20later-lightgrey" alt="License">
</p>

---

## 🌟 Overview

**Dekogram** is an independent, clean fork of the official Telegram Android app (`DrKLO/Telegram`). It re-engineers Telegram's media pipeline from the ground up to eliminate artificial download restrictions, double-storage caching waste, and forced cloud auto-downloads—all while preserving official MTProto protocol compatibility, full end-to-end encryption, and a zero-warning Google Play Protect footprint.

---

## 📥 The Specialized Download Engine

Unlike stock Telegram, Dekogram is specifically built for power downloaders and archival enthusiasts:

### 🚀 Direct-to-Storage Auto Export
Completed video and document downloads automatically bypass hidden app sandbox folders and route straight to your public storage:
* **Videos**: `Downloads/Dekogram/Videos/`
* **Documents & Files**: `Downloads/Dekogram/`
* **Images**: `Pictures/Dekogram/`

### 💾 Single-File Zero Waste Storage
Stock Telegram downloads files twice: once in the private internal sandbox (`Android/data/...`) and once again when you tap "Save to gallery". 
* **Dekogram eliminates duplicate storage consumption**: When a download completes, the app relinks Telegram's internal file database directly to the public copy in `Downloads/Dekogram/Videos/` and purges the internal cached duplicate. You get **50% storage savings**.

### 🏷️ Clean Original Filenames
* Eliminates Telegram’s forced `VID_2026xxxx` and generic hashes.
* Preserves original document and clip names exactly as uploaded by channel authors.
* Uses clean collision handling: appends a compact random 5-digit suffix (`_XXXXX`) **only** when a duplicate filename already exists.

### 📅 Post Timestamp Synchronization
* Synchronizes filesystem `lastModified` and Android MediaStore metadata (`DATE_TAKEN`, `DATE_MODIFIED`, `DATE_ADDED`) with the **original Telegram post date**.
* Your gallery, VLC, and MX Player sort media chronologically by **when it was originally posted**, not when you downloaded it.

### 🔋 Uninterrupted Background Downloads
* Features an Android foreground service with a `PARTIAL_WAKE_LOCK` and high-performance Wi-Fi lock.
* Downloads run continuously with an ongoing notification progress bar—**no timeouts or pausing** when your screen turns off or the app is minimized.

### 🎬 External Player Integration
* Added an instant **"Open in..."** action to all video menus, streaming or opening clips directly into VLC, MX Player, MPV, or your external player of choice.

---

## 🔓 Unrestricted Freedom & Privacy

* **Restriction Bypass (`noforwards`)**: Bypasses channel and group forward/save blocks. *"Saving content is restricted"* prompts are completely eliminated—Save to Gallery, Save to Downloads, and Sharing are permanently unlocked.
* **Self-Destructing / TTL Media Saving**: The save button remains fully available even on disappearing or timed photos/videos.
* **Global Screenshot & Screen Capture Access**: Stripped `FLAG_SECURE` app-wide. Screenshots and screen recording work everywhere, including restricted channels and secret chats.
* **Tracker & Telemetry Cleaned**: Completely stripped Microsoft AppCenter crash telemetry and Google Firebase UserActions background indexing.
* **Block Sponsored Messages & Video Ads**: Sponsored channel ads and video network ad units are blocked at the source.
* **Auto-Download Cloud Killswitch**: Default auto-download presets are initialized to disabled, and Telegram cloud server configs are blocked from turning background media auto-downloads back on.

---

## ⚡ Unique Quirks & Quality-of-Life

| Feature | Dekogram Behavior | Stock Telegram Behavior |
| :--- | :--- | :--- |
| **Max Accounts** | **10 Accounts** (Engineered in native C++ & Java) | 3 accounts (5 with Premium) |
| **Stories Bar** | **Hidden** cleanly with 0px layout shift | Dominates top of chat list |
| **Accidental Calls** | **Confirmation alert** before initiating calls | Taps call immediately |
| **Sticker Sends** | **Confirmation preview** before sending stickers | Single tap sends immediately |
| **Voice / Video Notes** | Touch release enters **review draft** mode | Releases send instantly |
| **Clean Package ID** | `org.dekogram.messenger` (No Play Protect blocks) | Conflicts with Play Store |

---

## 🔄 Autonomous Updates & Upstream Engine

Dekogram remains synchronized with upstream official Telegram while ensuring custom features never break:

1. **Modular Hook Architecture**:
   All Dekogram logic resides in an isolated package (`org.telegram.messenger.dekogram`). Patches to official Telegram classes are lightweight 1-line delegates, allowing upstream merges with 90%+ conflict reduction.
2. **Autonomous Cloud Pipeline**:
   A scheduled GitHub Actions workflow queries `DrKLO/Telegram` tags every 6 hours. When Telegram releases a new tag, GitHub cloud automatically merges, compiles with ccache, and publishes the new release asset.
3. **Native In-App Updater**:
   * Dekogram checks [GitHub Releases](https://github.com/iharshraj1123/dekogram/releases/latest) in the background.
   * Single-tap on the version text at the bottom of **Settings** checks for updates on demand.
   * Downloads and installs directly via Android's secure `FileProvider` without needing ADB or a computer.

---

## 📲 Installation

### Option 1: Direct APK Download (Recommended)
1. Download `Dekogram.apk` directly from the [Latest Release](https://github.com/iharshraj1123/dekogram/releases/latest).
2. Tap the downloaded APK to install.
3. Login using your phone number or QR code.

### Option 2: Automatic Updates via Obtainium
1. Install [Obtainium](https://github.com/ImranRXZ/Obtainium).
2. Add App -> Paste: `https://github.com/iharshraj1123/dekogram`
3. Obtainium will automatically track releases and notify you whenever a new APK is ready.

### Option 3: Install via ADB
```bash
adb push Dekogram.apk /data/local/tmp/app.apk
adb shell pm install -r -t -d /data/local/tmp/app.apk
adb shell rm /data/local/tmp/app.apk
```

---

## 🛠️ Building from Source

Dekogram builds the standalone variant target (`:TMessagesProj_AppStandalone:assembleAfatDebug`) optimized for ARM64 (`arm64-v8a`):

### Requirements
* JDK 17
* Android SDK 36 (Build Tools 35.0.0)
* Android NDK `27.2.12479018`
* CMake `3.22.1`

### Build Command
```bash
./gradlew :TMessagesProj_AppStandalone:assembleAfatDebug --no-daemon
```
The compiled APK will be located at:
`TMessagesProj_AppStandalone/build/outputs/apk/afat/debug/app.apk`

---

## ⚖️ License
Dekogram is licensed under the **GNU General Public License v2.0 or later** (GPLv2+). See [LICENSE](LICENSE) for details.
Based on [Telegram for Android](https://github.com/DrKLO/Telegram) by Nikolai Kudashov.
