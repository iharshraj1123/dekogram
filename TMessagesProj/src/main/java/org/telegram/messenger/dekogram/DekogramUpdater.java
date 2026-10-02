package org.telegram.messenger.dekogram;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BetaUpdate;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DekogramUpdater {

    private static final String TAG = "DekogramUpdater";

    public static class DekogramBetaUpdate extends BetaUpdate {
        public final String downloadUrl;
        public final long size;
        public final long publishedAt;
        public final String releaseTitle;

        public DekogramBetaUpdate(String version, int versionCode, String changelog, String downloadUrl, long size, long publishedAt, String releaseTitle) {
            super(version, versionCode, changelog);
            this.downloadUrl = downloadUrl;
            this.size = size;
            this.publishedAt = publishedAt;
            this.releaseTitle = releaseTitle;
        }

        @Override
        public boolean higherThan(BetaUpdate other) {
            if (other == null) {
                return true;
            }
            if (other instanceof DekogramBetaUpdate) {
                DekogramBetaUpdate o = (DekogramBetaUpdate) other;
                if (publishedAt > 0 && o.publishedAt > 0) {
                    return publishedAt > o.publishedAt;
                }
            }
            return super.higherThan(other);
        }
    }

    private static volatile DekogramBetaUpdate pendingUpdate;
    private static volatile boolean isDownloading = false;
    private static volatile float downloadProgress = 0.0f;
    private static volatile File downloadedFile = null;
    private static volatile boolean cancelRequested = false;

    public static BetaUpdate getPendingUpdate() {
        return pendingUpdate;
    }

    public static boolean isDownloading() {
        return isDownloading;
    }

    public static float getProgress() {
        return downloadProgress;
    }

    public static File getDownloadedFile() {
        return downloadedFile;
    }

    public static void cancelDownloading() {
        cancelRequested = true;
    }

    public static void checkUpdate(boolean force, Runnable whenDone) {
        if (!DekogramConfig.GITHUB_UPDATES_ENABLED) {
            if (whenDone != null) {
                whenDone.run();
            }
            return;
        }

        Utilities.globalQueue.postRunnable(() -> {
            DekogramBetaUpdate update = checkUpdateSync(force);
            AndroidUtilities.runOnUIThread(() -> {
                pendingUpdate = update;
                if (whenDone != null) {
                    whenDone.run();
                }
            });
        });
    }

    private static DekogramBetaUpdate checkUpdateSync(boolean force) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(DekogramConfig.GITHUB_API_LATEST_RELEASE);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "Dekogram/" + BuildVars.BUILD_VERSION_STRING);
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                FileLog.e(TAG + ": GitHub API returned response code " + responseCode);
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();

            JSONObject json = new JSONObject(sb.toString());
            String tagName = json.optString("tag_name", "");
            String releaseTitle = json.optString("name", "Dekogram Update");
            String changelog = json.optString("body", "");
            String publishedAtStr = json.optString("published_at", "");

            long publishedAt = 0;
            if (!TextUtils.isEmpty(publishedAtStr)) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date d = sdf.parse(publishedAtStr);
                    if (d != null) {
                        publishedAt = d.getTime();
                    }
                } catch (Exception ignore) {
                }
            }

            // Find APK asset
            String downloadUrl = null;
            long assetSize = 0;
            JSONArray assets = json.optJSONArray("assets");
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    String assetName = asset.optString("name", "");
                    if (assetName.toLowerCase().endsWith(".apk")) {
                        downloadUrl = asset.optString("browser_download_url", null);
                        assetSize = asset.optLong("size", 0);
                        break;
                    }
                }
            }

            if (TextUtils.isEmpty(downloadUrl)) {
                FileLog.e(TAG + ": No APK asset found in latest GitHub release");
                return null;
            }

            // Determine if release is newer
            long localInstallTime = 0;
            int localVersionCode = 1;
            try {
                Context context = ApplicationLoader.applicationContext;
                PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                localInstallTime = Math.max(pkgInfo.lastUpdateTime, pkgInfo.firstInstallTime);
                localVersionCode = pkgInfo.versionCode;
            } catch (Exception ignore) {
            }

            String cleanVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
            boolean hasHigherVersion = false;
            if (!"latest".equalsIgnoreCase(tagName) && !TextUtils.isEmpty(cleanVersion)) {
                if (SharedConfig.versionBiggerOrEqual(cleanVersion, BuildVars.BUILD_VERSION_STRING)
                        && !cleanVersion.equals(BuildVars.BUILD_VERSION_STRING)) {
                    hasHigherVersion = true;
                }
            }

            boolean isNewerRelease = (publishedAt > 0 && publishedAt > localInstallTime + 60000L);

            if (hasHigherVersion || isNewerRelease || (force && publishedAt > localInstallTime)) {
                String displayVersion = !TextUtils.isEmpty(cleanVersion) && !"latest".equalsIgnoreCase(cleanVersion)
                        ? cleanVersion
                        : BuildVars.BUILD_VERSION_STRING;
                return new DekogramBetaUpdate(displayVersion, localVersionCode + 1, changelog, downloadUrl, assetSize, publishedAt, releaseTitle);
            }

        } catch (Exception e) {
            FileLog.e(TAG + ": Check update error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    public static boolean showUpdateDialog(Context context, BetaUpdate update) {
        if (!(update instanceof DekogramBetaUpdate) || context == null) {
            return false;
        }

        final DekogramBetaUpdate dekogramUpdate = (DekogramBetaUpdate) update;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(dekogramUpdate.releaseTitle != null ? dekogramUpdate.releaseTitle : LocaleController.getString(R.string.AppUpdate));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        TextView versionText = new TextView(context);
        versionText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        versionText.setTextSize(16);
        versionText.setTypeface(AndroidUtilities.bold());
        versionText.setText("Version " + dekogramUpdate.version);
        container.addView(versionText);

        if (!TextUtils.isEmpty(dekogramUpdate.changelog)) {
            TextView changelogText = new TextView(context);
            changelogText.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
            changelogText.setTextSize(14);
            changelogText.setPadding(0, AndroidUtilities.dp(8), 0, 0);
            changelogText.setText(dekogramUpdate.changelog.trim());
            container.addView(changelogText);
        }

        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.AppUpdateDownloadNow), (dialog, which) -> {
            startDownloadWithProgress(context, dekogramUpdate);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);

        try {
            builder.show();
            return true;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return false;
    }

    public static void startDownloadWithProgress(Context context, DekogramBetaUpdate update) {
        if (isDownloading) {
            Toast.makeText(context, "Download already in progress", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder progressDialogBuilder = new AlertDialog.Builder(context);
        progressDialogBuilder.setTitle(LocaleController.getString(R.string.AppUpdateDownloading));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setIndeterminate(update.size <= 0);
        layout.addView(progressBar);

        TextView progressText = new TextView(context);
        progressText.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        progressText.setTextSize(13);
        progressText.setPadding(0, AndroidUtilities.dp(8), 0, 0);
        progressText.setGravity(Gravity.CENTER_HORIZONTAL);
        progressText.setText("Connecting...");
        layout.addView(progressText);

        progressDialogBuilder.setView(layout);
        progressDialogBuilder.setNegativeButton(LocaleController.getString(R.string.Cancel), (dialog, which) -> {
            cancelDownloading();
        });

        AlertDialog progressDialog = progressDialogBuilder.create();
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        isDownloading = true;
        cancelRequested = false;
        downloadProgress = 0.0f;

        Utilities.globalQueue.postRunnable(() -> {
            File destDir = new File(context.getExternalCacheDir() != null ? context.getExternalCacheDir() : context.getCacheDir(), "updates");
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            File apkFile = new File(destDir, "Dekogram.apk");
            if (apkFile.exists()) {
                apkFile.delete();
            }

            HttpURLConnection conn = null;
            InputStream in = null;
            FileOutputStream out = null;
            boolean success = false;
            try {
                URL url = new URL(update.downloadUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "Dekogram/" + BuildVars.BUILD_VERSION_STRING);

                // Handle redirects manually if needed (GitHub releases redirect to AWS S3)
                int status = conn.getResponseCode();
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                    String redirectUrl = conn.getHeaderField("Location");
                    conn.disconnect();
                    url = new URL(redirectUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.setRequestProperty("User-Agent", "Dekogram/" + BuildVars.BUILD_VERSION_STRING);
                }

                long totalBytes = conn.getContentLengthLong();
                if (totalBytes <= 0) {
                    totalBytes = update.size;
                }

                in = conn.getInputStream();
                out = new FileOutputStream(apkFile);

                byte[] buffer = new byte[8192];
                long downloadedBytes = 0;
                int read;
                long lastUiUpdate = 0;

                while ((read = in.read(buffer)) != -1) {
                    if (cancelRequested) {
                        break;
                    }
                    out.write(buffer, 0, read);
                    downloadedBytes += read;

                    long now = System.currentTimeMillis();
                    if (now - lastUiUpdate > 100 || downloadedBytes == totalBytes) {
                        lastUiUpdate = now;
                        final long curBytes = downloadedBytes;
                        final long total = totalBytes;
                        AndroidUtilities.runOnUIThread(() -> {
                            if (total > 0) {
                                int percent = (int) ((curBytes * 100) / total);
                                progressBar.setIndeterminate(false);
                                progressBar.setProgress(percent);
                                progressText.setText(AndroidUtilities.formatFileSize(curBytes) + " / " + AndroidUtilities.formatFileSize(total) + " (" + percent + "%)");
                            } else {
                                progressText.setText(AndroidUtilities.formatFileSize(curBytes));
                            }
                        });
                    }
                }

                if (!cancelRequested && apkFile.exists() && apkFile.length() > 0) {
                    success = true;
                    downloadedFile = apkFile;
                }
            } catch (Exception e) {
                FileLog.e(TAG + ": Download error: " + e.getMessage());
            } finally {
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                } catch (Exception ignore) {
                }
                if (conn != null) {
                    conn.disconnect();
                }
                isDownloading = false;
            }

            final boolean isSuccess = success;
            final boolean wasCancelled = cancelRequested;
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    progressDialog.dismiss();
                } catch (Exception ignore) {
                }
                if (wasCancelled) {
                    if (apkFile.exists()) {
                        apkFile.delete();
                    }
                    Toast.makeText(context, "Update download cancelled", Toast.LENGTH_SHORT).show();
                } else if (isSuccess) {
                    promptInstallApk(context, apkFile);
                } else {
                    Toast.makeText(context, "Failed to download update", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    public static void promptInstallApk(Context context, File apkFile) {
        if (context == null || apkFile == null || !apkFile.exists()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!ApplicationLoader.applicationContext.getPackageManager().canRequestPackageInstalls()) {
                ApplicationLoader.applicationLoaderInstance.checkApkInstallPermissions(context);
                return;
            }
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri apkUri = FileProvider.getUriForFile(context, ApplicationLoader.getApplicationId() + ".provider", apkFile);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            context.startActivity(intent);
        } catch (Exception e) {
            FileLog.e(TAG + ": Error opening APK install intent: " + e.getMessage());
            Toast.makeText(context, "Cannot open APK installer: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
