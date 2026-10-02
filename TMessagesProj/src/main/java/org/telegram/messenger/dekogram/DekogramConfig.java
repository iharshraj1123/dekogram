package org.telegram.messenger.dekogram;

public class DekogramConfig {

    public static final String APP_NAME = "Dekogram";
    public static final String DIR_NAME = "Dekogram";
    public static final String GITHUB_REPO = "iharshraj1123/dekogram";
    public static final String GITHUB_API_LATEST_RELEASE = "https://api.github.com/repos/iharshraj1123/dekogram/releases/latest";

    // Restriction & Security Bypasses
    public static final boolean BYPASS_RESTRICTED_CONTENT = true;
    public static final boolean ALLOW_SCREEN_CAPTURE = true;

    // Ads and Telemetry
    public static final boolean DISABLE_SPONSORED_MESSAGES = true;

    // Accounts & Limits
    public static final int MAX_ACCOUNTS = 10;

    // UI Adjustments
    public static final boolean HIDE_STORIES = true;
    public static final boolean CONFIRM_SEND_STICKER = true;
    public static final boolean CONFIRM_START_CALL = true;

    // Media & Storage
    public static final boolean AUTO_EXPORT_MEDIA = true;
    public static final boolean SINGLE_FILE_STORAGE = true;
    public static final boolean PRESERVE_ORIGINAL_FILENAMES = true;

    // In-App Updater
    public static final boolean GITHUB_UPDATES_ENABLED = true;

    // Video Download Naming Formats
    public static final int VIDEO_NAMING_FILE_NAME = 0; // File name (default)
    public static final int VIDEO_NAMING_FILE_ID = 1;   // File ID
    public static final int VIDEO_NAMING_FILE_ID_AND_NAME = 2; // File ID + File name

    public static int getVideoNamingMode() {
        return org.telegram.messenger.MessagesController.getGlobalMainSettings().getInt("dekogram_video_naming", VIDEO_NAMING_FILE_NAME);
    }

    public static void setVideoNamingMode(int mode) {
        org.telegram.messenger.MessagesController.getGlobalMainSettings().edit().putInt("dekogram_video_naming", mode).apply();
    }

    // Download Speed Boost
    public static final int DOWNLOAD_BOOST_OFF = 0;      // Disabled (4 streams, 128KB chunks)
    public static final int DOWNLOAD_BOOST_FAST = 1;     // Fast (8 parallel streams, 512KB chunks)
    public static final int DOWNLOAD_BOOST_MAXIMUM = 2;  // Maximum (12 parallel streams, 512KB chunks) [Default]

    public static int getDownloadBoostMode() {
        return org.telegram.messenger.MessagesController.getGlobalMainSettings().getInt("dekogram_download_boost", DOWNLOAD_BOOST_MAXIMUM);
    }

    public static void setDownloadBoostMode(int mode) {
        org.telegram.messenger.MessagesController.getGlobalMainSettings().edit().putInt("dekogram_download_boost", mode).apply();
    }
}
