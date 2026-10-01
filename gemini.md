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
* **`.github/workflows/build-apk.yml`**: Added GitHub Actions workflow to build the standalone debug APK (`:TMessagesProj_AppStandalone:assembleAfatDebug`) with JDK 17 and Android SDK/NDK, uploading the APK as a downloadable artifact. Added runner disk cleanup step.
* **`build.gradle` (TMessagesProj & TMessagesProj_AppStandalone)**: Filtered ABIs to `arm64-v8a` and `armeabi-v7a` to avoid runner disk exhaustion and speed up compilation.

### 6. Multi-Account Limit (Increased to 10)
* **`Defines.h`**: Increased `MAX_ACCOUNT_COUNT` from 5 to 10.
* **`ConnectionsManager.cpp`**: Added instance switch cases 4 through 9 in `getInstance(instanceNum)`.
* **`UserConfig.java`**: Set `MAX_ACCOUNT_DEFAULT_COUNT = 10`, `MAX_ACCOUNT_COUNT = 10`, and forced `getMaxAccountCount()` to return 10.

### 7. Hide Stories Bar
* **`DialogsActivity.java`**: Set `dialogStoriesCell` visibility to `View.GONE` and suppressed stories bar rendering in `updateStoriesVisibility()` to prevent layout shifts.

### 8. Block Sponsored Messages & Video Ads
* **`MessagesController.java`**: Forced `getSponsoredMessages()` to return `null` immediately and `isSponsoredDisabled()` to return `true`.
* **`VideoAds.java`**: Emptied `load()` to block sponsored video ad network fetch.
* **`ChatActivity.java`**: Forced `getSponsoredMessagesCount()` to return `0`.

### 9. Strip Trackers & Telemetry
* **`LaunchActivity.java`**: Removed `ApplicationLoader.startAppCenter()` initialization and `FirebaseUserActions` indexing calls.

### 10. Accidental Action Confirmations & Voice/Video Note Preview
* **`VoIPHelper.java`**: Added confirmation dialog in `startCall` before initiating voice/video calls.
* **`ChatActivityEnterView.java`**: Added confirmation dialog before sending stickers. Switched voice and video note touch release from immediate send (`stopRecording(1)`) to review draft state (`stopRecording(2)`).

### 11. Preserve Original Filenames on Save
* **`MediaController.java`**: Forwarded custom `name` parameter to `saveFileInternal` on Android 10+ scoped storage and legacy storage, appending extension if absent.
* **`ChatActivity.java`**: Passed `FileLoader.getDocumentFileName` into `MediaController.saveFile()` when saving media.
* **`PhotoViewer.java`**: Extracted and passed `FileLoader.getDocumentFileName` into `MediaController.saveFile()` when saving media.

### 12. Open Video in External Player
* **`ChatActivity.java`**: Added `OPTION_OPEN_EXTERNAL_PLAYER` ("Open in...") to video message action menus, launching `AndroidUtilities.openForView` or starting download if uncached.
* **`PhotoViewer.java`**: Passed `restrict = false` in `gallery_menu_openin` to allow external video player playback.
* **`AndroidUtilities.java`**: Resolved message MIME types and checked attach path fallbacks in `openForView()`.

### 13. Login Verification Stability
* **`LoginActivity.java`**: Disabled `allow_flashcall` and `allow_missed_call` in `TL_codeSettings` to bypass carrier flash call interception and deliver codes directly via in-app notification or SMS. Kept `codeFieldContainer` visible on all screens and prevented automatic backward navigation on `PHONE_CODE_EXPIRED`. Removed blocking phone/call-log permission checks (`checkPermissions = false`) so submitting phone number immediately sends verification code.
* **`CallReceiver.java`**: Reduced incoming call match window from 15 hours to 2 minutes, preventing stale incoming numbers from submitting as verification codes.

### 14. Accelerated CI Build Pipeline
* **`build.gradle` (TMessagesProj & TMessagesProj_AppStandalone)**: Filtered ABIs strictly to `arm64-v8a`, reduced `ndk.debugSymbolLevel` to `'NONE'`, and configured `ccache` compiler launcher for CMake.
* **`gradle.properties`**: Enabled `org.gradle.caching=true`.
* **`.github/workflows/build-apk.yml`**: Installed `ccache`, configured 5GB GitHub Actions cache for compiled C++ objects, enabled Gradle `--build-cache`, and configured 10GB NVMe swapfile.
* **`gradle.properties`**: Tuned Gradle JVM args (`-Xmx4g -XX:MaxMetaspaceSize=512m`) to prevent Linux OOM killer termination during parallel Clang/CMake compilation on 7GB GitHub runners.

### 15. Automatic Media Export & Internal Directory Renaming
* **`FileLoader.java`**: Automatically routes completed video, document, and photo downloads to `MediaController.saveFile()`, placing them immediately into public `Download/Dekogram/Videos/` (or `Download/Dekogram/`) with their original filenames upon download completion without requiring manual "Save to gallery" interaction.
* **`ImageLoader.java`, `SharedConfig.java`, `AndroidUtilities.java`, `ChatAttachAlertDocumentLayout.java`**: Renamed all internal app and media directories from `Telegram` (`Telegram Images`, `Telegram Video`, `Telegram Documents`, `Telegram Audio`, `Telegram Files`) to `Dekogram`.
* **`MediaController.java`**: Added `context instanceof Activity` and `showProgress` guard to prevent background auto-save dialog exceptions.

### 16. UI Brand Harmonization & Direct Distribution
* **`strings.xml`**: Updated user-facing strings across storage usage ("Dekogram uses %s...", "Clear Dekogram Cache", "Dekogram Cache"), system permissions dialogs ("Dekogram needs..."), passcode lock screens ("Dekogram Locked", "Unlock Dekogram"), and version branding ("Dekogram for Android %1$s").
* **`FilesMigrationService.java`**: Switched migration target directory from `Telegram` to `Dekogram`.
* **`.github/workflows/build-apk.yml`**: Added GitHub Release automated publishing (`tag: latest`) delivering raw `Dekogram.apk` directly to avoid ZIP packaging errors on mobile, and flattened Actions artifact structure.

### 17. Persistent Background Downloads with Notification Service
* **`DownloadForegroundService.java`**: Created dataSync foreground service with `PARTIAL_WAKE_LOCK` and high-perf Wi-Fi lock. Renders ongoing notification with filename and progress bar. Self-terminates when downloads finish.
* **`ConnectionsManager.java`, `LaunchActivity.java`, `ScreenReceiver.java`**: Prevented native network pausing (`native_pauseNetwork`) when app is minimized or screen turns off while downloads are active.
### 18. Third-Party Client Login Architecture (Nekogram Standard) & Zero-Warning Install
* **Package Identity**: Configured application package as `org.dekogram.messenger` across `gradle.properties`, `build.gradle`, account sync, and `google-services.json`. Completely eliminates Google Play Protect impersonation blocks.
* **Permission Cleanup**: Stripped high-risk `SEND_SMS` and `READ_CALL_LOG` permissions from `AndroidManifest_standalone.xml`. Play Protect scans clean without malware warnings.
* **API Credentials**: Configured third-party Telegram client API credentials in `BuildVars.java` (`APP_ID = 442495`, `APP_HASH = "873ffaceba76e791ff2491224a3cdb49"`, `SUPPORTS_PASSKEYS = false`). Telegram MTProto servers validate third-party package names, enabling verification codes and account login without official package restrictions.
* **`LoginActivity.java`**: Bypassed telephony and call-log permission trap via Nekogram guard (`false && ...`) in `onConfirm` and `onNextPressed`, directly advancing to phone confirmation and verification code dispatch.

---

## Installation & Deployment

### 1. Direct Download on Phone (No ADB Needed)
* Download `Dekogram.apk` directly from [Dekogram Releases (Latest)](https://github.com/iharshraj1123/dekogram/releases/tag/latest).
* Tap the downloaded `.apk` to install cleanly without Play Protect blocks.

### 2. Install on Device via ADB
1. Connect device via USB with **USB Debugging** enabled.
2. Push and install the APK (package runs cleanly as `org.dekogram.messenger`):
   ```bash
   adb push build_artifact/Dekogram.apk /data/local/tmp/app.apk
   adb shell pm install -r -t -d /data/local/tmp/app.apk
   adb shell rm /data/local/tmp/app.apk
   ```
3. Launch Dekogram from launcher or ADB:
   ```bash
   adb shell am start -n org.dekogram.messenger/org.telegram.messenger.DefaultIcon
   ```
