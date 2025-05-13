package net.minecraft.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

public class StringTranslate {
    private static final Pattern NUMERIC_VARIABLE_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");
    private static final Splitter EQUAL_SIGN_SPLITTER = Splitter.on('=').limit(2);
    private static final StringTranslate INSTANCE = new StringTranslate();
    private final Map<String, String> languageList = new HashMap<>();
    private long lastUpdateTimeInMilliseconds;

    public StringTranslate() {
        InputStream inputstream = StringTranslate.class.getResourceAsStream("/assets/minecraft/lang/en_US.lang");

        for (String s : IOUtils.readLines(inputstream, StandardCharsets.UTF_8)) {
            if (!s.isEmpty() && s.charAt(0) != 35) {
                String[] astring = Iterables.toArray(EQUAL_SIGN_SPLITTER.split(s), String.class);

                if (astring != null && astring.length == 2) {
                    String s1 = astring[0];
                    String s2 = NUMERIC_VARIABLE_PATTERN.matcher(astring[1]).replaceAll("%$1s");
                    this.languageList.put(s1, s2);
                }
            }
        }

        this.lastUpdateTimeInMilliseconds = System.currentTimeMillis();
    }

    static StringTranslate getInstance() {
        return INSTANCE;
    }

    public static synchronized void replaceWith(Map<String, String> p_135063_0_) {
        INSTANCE.languageList.clear();
        INSTANCE.languageList.putAll(p_135063_0_);
        INSTANCE.lastUpdateTimeInMilliseconds = System.currentTimeMillis();
    }

    public synchronized String translateKey(String key) {
        return this.tryTranslateKey(key);
    }

    public synchronized String translateKeyFormat(String key, Object... format) {
        String s = this.tryTranslateKey(key);

        try {
            return String.format(s, format);
        } catch (IllegalFormatException var5) {
            return "Format error: " + s;
        }
    }

    private String tryTranslateKey(String key) {
        String s = this.languageList.get(key);
        return s == null ? key : s;
    }

    public synchronized boolean isKeyTranslated(String key) {
        return this.languageList.containsKey(key);
    }

    public long getLastUpdateTimeInMilliseconds() {
        return this.lastUpdateTimeInMilliseconds;
    }
}
