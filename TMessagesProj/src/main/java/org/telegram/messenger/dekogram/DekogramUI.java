package org.telegram.messenger.dekogram;

import android.content.Context;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;

public class DekogramUI {

    public static boolean shouldHideStories() {
        return DekogramConfig.HIDE_STORIES;
    }

    public static boolean showStickerConfirmAlert(Context context, Runnable onConfirmed) {
        if (!DekogramConfig.CONFIRM_SEND_STICKER || context == null) {
            if (onConfirmed != null) {
                onConfirmed.run();
            }
            return false;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString(R.string.SendStickerPreview));
        builder.setMessage(LocaleController.getString(R.string.SendStickerPreview) + "?");
        builder.setPositiveButton(LocaleController.getString(R.string.Send), (dialog, which) -> {
            if (onConfirmed != null) {
                onConfirmed.run();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
        return true;
    }
}
