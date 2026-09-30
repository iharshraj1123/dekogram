# Dekogram — Project Modifications Log

> **Maintenance Instruction**: Any assistant or developer modifying this repository must keep this file up to date. Log new modifications as concise bullet points with affected files and rationale. Keep entries minimal and avoid verbose additions.

---

## Overview
Dekogram is a private, clean fork of the official Telegram Android client (`DrKLO/Telegram`) with privacy-focused defaults and unrestricted local media saving.

---

## Changes Log

### 1. Restricted Content & Screen Capture Bypass
* **`MessagesController.java`**: Forced `isChatNoForwards()`, `isPeerNoForwards()`, and `isUserNoForwards()` to return `false`.
* **`PhotoViewer.java`**:
  * Set `noforwards = false` across viewer initializers (`lines 14080, 14528, 14846, 15648`).
  * Removed TTL/self-destruct check hiding the save button (`line 14765`).
  * Cleared `FLAG_SECURE` (`line 17435`) to allow screenshots and screen recording.
* **`ChatActivity.java`**:
  * Forced `noforwards = false` and `noforwardsOrPaidMedia = false` in message action popup (`line 45709`) so "Save to Gallery", "Save to Downloads", and "Share" always display.
* **`MessageObject.java`**:
  * Removed `messageOwner.noforwards` checks from `canShareMessage()`, `canForwardMessage()`, and text selectable layout.
* **`FileLoader.java`**:
  * Removed `noforwards` and `isPeerNoForwards` checks from `checkSaveToGallery()`.
* **`FlagSecureReason.java`**:
  * Forced `updateWindowSecure()` to clear `FLAG_SECURE` app-wide; `isSecuredNow()` returns `false`.

### 2. Default Download Directory (`Downloads > Dekogram > Videos`)
* **`MediaController.java`**:
  * Android 10+ (`MediaStore`): Videos saved with relative path `Download/Dekogram/Videos/` via `MediaStore.Downloads`. Documents routed to `Download/Dekogram/`.
  * Legacy filesystem: Direct directory configured as `Environment.DIRECTORY_DOWNLOADS + "/Dekogram/Videos"` for videos and `"/Dekogram"` for documents/pictures.
  * Motion photos and cache exports updated from `Telegram` to `Dekogram`.

### 3. Auto-Download Disabled by Default
* **`DownloadController.java`**:
  * Initialized default presets (`lowPreset`, `mediumPreset`, `highPreset`, `mobilePreset`, `wifiPreset`, `roamingPreset`) with disabled mask `0_0_0_0_...` and `enabled = false`.
  * Patched `loadAutoDownloadConfig()` with an immediate return to block Telegram servers from re-enabling auto-download via cloud config.

### 4. Branding & Update Prevention
* **`strings.xml`**: Renamed `AppName` and `AppNameBeta` to `Dekogram` and `Dekogram Beta`.
* **`BuildVars.java`**: Set `CHECK_UPDATES = false` to prevent in-app prompts downloading official APK updates over this build.

### 5. Automated Cloud CI Build
* **`.github/workflows/build-apk.yml`**: Added GitHub Actions workflow to build the standalone debug APK (`:TMessagesProj_AppStandalone:assembleAfatDebug`) with JDK 17 and Android SDK/NDK, uploading the APK as a downloadable artifact.
