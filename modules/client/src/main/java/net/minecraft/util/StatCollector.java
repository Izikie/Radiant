package net.minecraft.util;

public class StatCollector {
    private static final StringTranslate LOCALIZED_NAME = StringTranslate.getInstance();
    private static final StringTranslate FALLBACK_TRANSLATOR = new StringTranslate();

    public static String translateToLocal(String key) {
        return LOCALIZED_NAME.translateKey(key);
    }

    public static String translateToLocalFormatted(String key, Object... format) {
        return LOCALIZED_NAME.translateKeyFormat(key, format);
    }

    public static String translateToFallback(String key) {
        return FALLBACK_TRANSLATOR.translateKey(key);
    }

    public static boolean canTranslate(String key) {
        return LOCALIZED_NAME.isKeyTranslated(key);
    }

    public static long getLastTranslationUpdateTimeInMilliseconds() {
        return LOCALIZED_NAME.getLastUpdateTimeInMilliseconds();
    }
}
