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
}
