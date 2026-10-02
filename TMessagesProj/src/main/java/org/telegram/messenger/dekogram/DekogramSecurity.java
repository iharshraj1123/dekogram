package org.telegram.messenger.dekogram;

public class DekogramSecurity {

    public static boolean isChatNoForwards(boolean original) {
        if (DekogramConfig.BYPASS_RESTRICTED_CONTENT) {
            return false;
        }
        return original;
    }

    public static boolean isPeerNoForwards(boolean original) {
        if (DekogramConfig.BYPASS_RESTRICTED_CONTENT) {
            return false;
        }
        return original;
    }

    public static boolean isUserNoForwards(boolean original) {
        if (DekogramConfig.BYPASS_RESTRICTED_CONTENT) {
            return false;
        }
        return original;
    }

    public static boolean filterNoForwards(boolean original) {
        if (DekogramConfig.BYPASS_RESTRICTED_CONTENT) {
            return false;
        }
        return original;
    }

    public static boolean shouldClearFlagSecure() {
        return DekogramConfig.ALLOW_SCREEN_CAPTURE;
    }

    public static boolean isSecuredNow(boolean original) {
        if (DekogramConfig.ALLOW_SCREEN_CAPTURE) {
            return false;
        }
        return original;
    }

    public static void sanitizeCodeSettings(org.telegram.tgnet.TLRPC.TL_codeSettings settings) {
        if (settings != null) {
            settings.allow_flashcall = false;
            settings.allow_missed_call = false;
        }
    }
}
