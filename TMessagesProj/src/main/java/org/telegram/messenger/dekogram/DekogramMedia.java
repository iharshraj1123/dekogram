package org.telegram.messenger.dekogram;

import android.os.Environment;
import android.text.TextUtils;

import org.telegram.messenger.Utilities;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DekogramMedia {

    public static String resolveSaveFileName(File targetDir, String name, String extension, int type, long postDateSeconds) {
        if (!DekogramConfig.PRESERVE_ORIGINAL_FILENAMES) {
            return null;
        }

        if (TextUtils.isEmpty(name)) {
            Date date = postDateSeconds > 0 ? new Date(postDateSeconds * 1000L) : new Date();
            String dateStr = new SimpleDateFormat("yyyyMMdd", Locale.US).format(date);
            int random5 = 10000 + Utilities.random.nextInt(90000);
            String cleanExt = !TextUtils.isEmpty(extension) ? extension : (type == 1 ? "mp4" : (type == 0 ? "jpg" : "bin"));
            return dateStr + "_" + random5 + "." + cleanExt;
        }

        String finalName = name.trim();
        if (!TextUtils.isEmpty(extension) && !finalName.toLowerCase().endsWith("." + extension.toLowerCase())) {
            finalName = finalName + "." + extension;
        }

        if (targetDir != null && targetDir.exists()) {
            File testFile = new File(targetDir, finalName);
            if (testFile.exists()) {
                int dot = finalName.lastIndexOf('.');
                String base = dot > 0 ? finalName.substring(0, dot) : finalName;
                String ext = dot > 0 ? finalName.substring(dot) : "";
                for (int i = 0; i < 10; i++) {
                    int random5 = 10000 + Utilities.random.nextInt(90000);
                    String candidateName = base + "_" + random5 + ext;
                    if (!new File(targetDir, candidateName).exists()) {
                        return candidateName;
                    }
                }
            }
        }
        return finalName;
    }

    public static String getRelativeStoragePath(int type) {
        if (type == 0) {
            return Environment.DIRECTORY_PICTURES + File.separator + DekogramConfig.DIR_NAME + File.separator;
        } else if (type == 1) {
            return Environment.DIRECTORY_DOWNLOADS + File.separator + DekogramConfig.DIR_NAME + File.separator + "Videos" + File.separator;
        } else if (type == 2) {
            return Environment.DIRECTORY_DOWNLOADS + File.separator + DekogramConfig.DIR_NAME + File.separator;
        } else {
            return Environment.DIRECTORY_MUSIC + File.separator + DekogramConfig.DIR_NAME + File.separator;
        }
    }

    public static File getPublicStorageDirectory(int type) {
        if (type == 0) {
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), DekogramConfig.DIR_NAME);
        } else if (type == 1) {
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DekogramConfig.DIR_NAME + File.separator + "Videos");
        } else if (type == 2) {
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DekogramConfig.DIR_NAME);
        } else {
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), DekogramConfig.DIR_NAME);
        }
    }

    public static boolean shouldDeleteInternalCache() {
        return DekogramConfig.SINGLE_FILE_STORAGE;
    }
}
