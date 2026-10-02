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

    public static void handleDownloadCompleted(Object parentObject, org.telegram.tgnet.TLRPC.Document document, File finalFile, org.telegram.messenger.FileLoader fileLoader, org.telegram.messenger.DownloadController downloadController) {
        if (!(parentObject instanceof org.telegram.messenger.MessageObject)) {
            return;
        }
        org.telegram.messenger.MessageObject messageObject = (org.telegram.messenger.MessageObject) parentObject;
        if (document != null && (messageObject.putInDownloadsStore || messageObject.isVideo() || messageObject.isDocument())) {
            if (downloadController != null) {
                downloadController.onDownloadComplete(messageObject);
            }
            if (!messageObject.isRoundVideo() && !messageObject.isVoice() && !messageObject.isAnyKindOfSticker()) {
                String fileNameToSave = org.telegram.messenger.FileLoader.getDocumentFileName(document);
                int mediaType = messageObject.isVideo() ? 1 : (messageObject.isDocument() ? 2 : 0);
                long postDate = messageObject.messageOwner != null ? messageObject.messageOwner.date : 0;
                final File origFile = finalFile;
                org.telegram.messenger.MediaController.saveFile(finalFile.getAbsolutePath(), org.telegram.messenger.ApplicationLoader.applicationContext, mediaType, fileNameToSave, document.mime_type, uri -> {
                    if (uri != null) {
                        String savedPath = org.telegram.messenger.AndroidUtilities.getPath(uri);
                        if (TextUtils.isEmpty(savedPath) && "file".equalsIgnoreCase(uri.getScheme())) {
                            savedPath = uri.getPath();
                        }
                        if (TextUtils.isEmpty(savedPath)) {
                            File dir = mediaType == 1
                                    ? new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DekogramConfig.DIR_NAME + File.separator + "Videos")
                                    : new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DekogramConfig.DIR_NAME);
                            File candidate = new File(dir, fileNameToSave);
                            if (candidate.exists() && candidate.length() > 0) {
                                savedPath = candidate.getAbsolutePath();
                            }
                        }
                        if (!TextUtils.isEmpty(savedPath)) {
                            File savedFile = new File(savedPath);
                            if (savedFile.exists() && savedFile.length() > 0) {
                                int dirType = mediaType == 1 ? org.telegram.messenger.FileLoader.MEDIA_DIR_VIDEO : org.telegram.messenger.FileLoader.MEDIA_DIR_DOCUMENT;
                                if (fileLoader != null) {
                                    fileLoader.getFileDatabase().putPath(document.id, document.dc_id, dirType, 0, savedPath);
                                }
                                document.localPath = savedPath;
                                if (origFile != null && origFile.exists() && !origFile.getAbsolutePath().equals(savedPath)) {
                                    origFile.delete();
                                }
                            }
                        }
                    }
                }, false, postDate);
            }
        } else if (messageObject.isPhoto() && messageObject.putInDownloadsStore) {
            long postDate = messageObject.messageOwner != null ? messageObject.messageOwner.date : 0;
            org.telegram.messenger.MediaController.saveFile(finalFile.getAbsolutePath(), org.telegram.messenger.ApplicationLoader.applicationContext, 0, null, "image/jpeg", null, false, postDate);
        }
    }

    public static void applyDownloadBoost(org.telegram.messenger.FileLoadOperation op, boolean forceSmallChunk) {
        if (op == null || forceSmallChunk) {
            return;
        }
        int mode = DekogramConfig.getDownloadBoostMode();
        if (mode == DekogramConfig.DOWNLOAD_BOOST_OFF) {
            return;
        }
        op.downloadChunkSizeBig = 1024 * 512;
        op.downloadChunkSize = 1024 * 128;
        op.bigFileSizeFrom = 1024 * 1024;
        op.downloadChunkSizeAnimation = 1024 * 256;
        op.maxDownloadRequestsAnimation = 8;
        if (mode == DekogramConfig.DOWNLOAD_BOOST_FAST) {
            op.maxDownloadRequests = 8;
            op.maxDownloadRequestsBig = 8;
        } else {
            op.maxDownloadRequests = 12;
            op.maxDownloadRequestsBig = 12;
        }
    }
}
