package org.telegram.messenger.dekogram;

import android.os.Environment;
import android.text.TextUtils;

import org.telegram.messenger.Utilities;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DekogramMedia {

    public static String extractFileId(File sourceFile) {
        if (sourceFile == null) {
            return null;
        }
        String name = sourceFile.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        if (base.contains("_")) {
            int idx = base.indexOf('_');
            String afterUnderscore = base.substring(idx + 1);
            if (afterUnderscore.matches("\\d+")) {
                return afterUnderscore;
            }
        }
        if (base.matches("\\d+")) {
            return base;
        }
        return base;
    }

    public static String resolveSaveFileName(File targetDir, String name, String extension, int type, long postDateSeconds) {
        return resolveSaveFileName(targetDir, null, name, extension, type, postDateSeconds);
    }

    public static String resolveSaveFileName(File targetDir, File sourceFile, String name, String extension, int type, long postDateSeconds) {
        if (!DekogramConfig.PRESERVE_ORIGINAL_FILENAMES) {
            return null;
        }

        String cleanExt = !TextUtils.isEmpty(extension) ? extension : (type == 1 ? "mp4" : (type == 0 ? "jpg" : "bin"));

        // Video Naming Modes (type == 1: Video)
        if (type == 1) {
            int mode = DekogramConfig.getVideoNamingMode();
            String fileId = extractFileId(sourceFile);

            if (mode == DekogramConfig.VIDEO_NAMING_FILE_ID && !TextUtils.isEmpty(fileId)) {
                String candidate = fileId + "." + cleanExt;
                return checkCollision(targetDir, candidate);
            } else if (mode == DekogramConfig.VIDEO_NAMING_FILE_ID_AND_NAME && !TextUtils.isEmpty(fileId)) {
                if (!TextUtils.isEmpty(name) && !name.trim().isEmpty()) {
                    String baseName = name.trim();
                    if (baseName.toLowerCase().endsWith("." + cleanExt.toLowerCase())) {
                        baseName = baseName.substring(0, baseName.length() - (cleanExt.length() + 1));
                    }
                    String candidate = fileId + "_" + baseName + "." + cleanExt;
                    return checkCollision(targetDir, candidate);
                } else {
                    String candidate = fileId + "." + cleanExt;
                    return checkCollision(targetDir, candidate);
                }
            }
        }

        // Default Mode: Original file name
        if (TextUtils.isEmpty(name) || name.trim().isEmpty()) {
            Date date = postDateSeconds > 0 ? new Date(postDateSeconds * 1000L) : new Date();
            String dateStr = new SimpleDateFormat("yyyyMMdd", Locale.US).format(date);
            int random5 = 10000 + Utilities.random.nextInt(90000);
            return dateStr + "_" + random5 + "." + cleanExt;
        }

        String finalName = name.trim();
        if (!TextUtils.isEmpty(cleanExt) && !finalName.toLowerCase().endsWith("." + cleanExt.toLowerCase())) {
            finalName = finalName + "." + cleanExt;
        }

        return checkCollision(targetDir, finalName);
    }

    private static String checkCollision(File targetDir, String finalName) {
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
