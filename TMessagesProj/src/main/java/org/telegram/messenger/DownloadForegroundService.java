package org.telegram.messenger;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.telegram.tgnet.TLRPC;
import org.telegram.ui.LaunchActivity;

public class DownloadForegroundService extends Service implements NotificationCenter.NotificationCenterDelegate {

    public static final int ID_NOTIFICATION = 42;
    private static DownloadForegroundService instance;

    private NotificationCompat.Builder builder;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    private long lastProgressUpdate;

    public DownloadForegroundService() {
        super();
    }

    public static boolean isRunning() {
        return instance != null;
    }

    public static boolean hasActiveDownloads() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                DownloadController controller = DownloadController.getInstance(a);
                if (controller != null && controller.downloadingFiles != null && !controller.downloadingFiles.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void start() {
        if (!hasActiveDownloads()) {
            return;
        }
        if (instance != null) {
            instance.updateNotification();
            return;
        }
        try {
            Intent intent = new Intent(ApplicationLoader.applicationContext, DownloadForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ApplicationLoader.applicationContext.startForegroundService(intent);
            } else {
                ApplicationLoader.applicationContext.startService(intent);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static void stop() {
        if (instance != null) {
            instance.stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dekogram:download_lock");
                wakeLock.setReferenceCounted(false);
                wakeLock.acquire(60 * 60 * 1000L);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }

        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "dekogram:download_wifi");
                wifiLock.setReferenceCounted(false);
                wifiLock.acquire();
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }

        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            NotificationCenter.getInstance(i).addObserver(this, NotificationCenter.fileLoadProgressChanged);
            NotificationCenter.getInstance(i).addObserver(this, NotificationCenter.onDownloadingFilesChanged);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!hasActiveDownloads()) {
            stopSelf();
            return Service.START_NOT_STICKY;
        }

        if (builder == null) {
            NotificationsController.checkOtherNotificationsChannel();
            builder = new NotificationCompat.Builder(this, NotificationsController.OTHER_NOTIFICATIONS_CHANNEL);
            builder.setSmallIcon(android.R.drawable.stat_sys_download);
            builder.setWhen(System.currentTimeMillis());
            builder.setOngoing(true);
            builder.setOnlyAlertOnce(true);
            builder.setPriority(NotificationCompat.PRIORITY_LOW);

            Intent launchIntent = new Intent(this, LaunchActivity.class);
            launchIntent.setAction(Intent.ACTION_MAIN);
            launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
            builder.setContentIntent(pendingIntent);
        }

        updateNotificationContent(null, 0, 0);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(ID_NOTIFICATION, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(ID_NOTIFICATION, builder.build());
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;

        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            NotificationCenter.getInstance(i).removeObserver(this, NotificationCenter.fileLoadProgressChanged);
            NotificationCenter.getInstance(i).removeObserver(this, NotificationCenter.onDownloadingFilesChanged);
        }

        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Throwable ignore) {}
            wakeLock = null;
        }

        if (wifiLock != null && wifiLock.isHeld()) {
            try {
                wifiLock.release();
            } catch (Throwable ignore) {}
            wifiLock = null;
        }

        try {
            stopForeground(true);
        } catch (Throwable ignore) {}

        try {
            NotificationManagerCompat.from(this).cancel(ID_NOTIFICATION);
        } catch (Throwable ignore) {}
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.onDownloadingFilesChanged) {
            AndroidUtilities.runOnUIThread(() -> {
                if (!hasActiveDownloads()) {
                    stopSelf();
                } else {
                    updateNotification();
                }
            });
        } else if (id == NotificationCenter.fileLoadProgressChanged) {
            String fileName = (String) args[0];
            Long loadedSize = (Long) args[1];
            Long totalSize = (Long) args[2];

            long now = SystemClock.uptimeMillis();
            if (now - lastProgressUpdate < 500) {
                return;
            }
            lastProgressUpdate = now;

            AndroidUtilities.runOnUIThread(() -> updateNotificationContent(fileName, loadedSize, totalSize));
        }
    }

    public void updateNotification() {
        updateNotificationContent(null, 0, 0);
    }

    private void updateNotificationContent(String fileName, long loaded, long total) {
        if (builder == null) {
            return;
        }
        int totalDownloadingCount = 0;
        MessageObject primaryMessage = null;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                DownloadController controller = DownloadController.getInstance(a);
                if (controller != null && controller.downloadingFiles != null) {
                    totalDownloadingCount += controller.downloadingFiles.size();
                    if (primaryMessage == null && !controller.downloadingFiles.isEmpty()) {
                        primaryMessage = controller.downloadingFiles.get(0);
                    }
                }
            }
        }

        if (totalDownloadingCount == 0) {
            stopSelf();
            return;
        }

        String title;
        if (totalDownloadingCount > 1) {
            title = "Downloading " + totalDownloadingCount + " files";
        } else if (primaryMessage != null) {
            String name = FileLoader.getDocumentFileName(primaryMessage.getDocument());
            if (name == null) {
                name = primaryMessage.getFileName();
            }
            title = "Downloading " + (name != null ? name : "media");
        } else if (fileName != null) {
            title = "Downloading " + fileName;
        } else {
            title = "Downloading media...";
        }

        builder.setContentTitle(title);

        if (total > 0) {
            int percent = (int) ((loaded / (float) total) * 100);
            builder.setProgress(100, percent, false);
            builder.setContentText(AndroidUtilities.formatFileSize(loaded) + " / " + AndroidUtilities.formatFileSize(total) + " (" + percent + "%)");
        } else {
            builder.setProgress(0, 0, true);
            builder.setContentText("Downloading...");
        }

        try {
            NotificationManagerCompat.from(this).notify(ID_NOTIFICATION, builder.build());
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
