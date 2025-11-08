package net.minecraft.client.settings;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeyBinding implements Comparable<KeyBinding> {
    private static final List<KeyBinding> KEYBIND_ARRAY = new ArrayList<>();
    private static final Int2ObjectOpenHashMap<KeyBinding> HASH = new Int2ObjectOpenHashMap<>();
    private static final Set<String> KEYBIND_SET = new HashSet<>();
    private final String keyDescription;
    private final int keyCodeDefault;
    private final String keyCategory;
    private int keyCode;
    private boolean pressed;
    private int pressTime;

    public static void onTick(int keyCode) {
        if (keyCode != 0) {
            KeyBinding keybinding = HASH.get(keyCode);

            if (keybinding != null) {
                ++keybinding.pressTime;
            }
        }
    }

    public static void setKeyBindState(int keyCode, boolean pressed) {
        if (keyCode != 0) {
            KeyBinding keybinding = HASH.get(keyCode);

            if (keybinding != null) {
                keybinding.pressed = pressed;
            }
        }
    }

    public static void unPressAllKeys() {
        for (KeyBinding keybinding : KEYBIND_ARRAY) {
            keybinding.unpressKey();
        }
    }

    public static void resetKeyBindingArrayAndHash() {
        HASH.clear();

        for (KeyBinding keybinding : KEYBIND_ARRAY) {
            HASH.put(keybinding.keyCode, keybinding);
        }
    }

    public static Set<String> getKeybinds() {
        return KEYBIND_SET;
    }

    public KeyBinding(String description, int keyCode, String category) {
        this.keyDescription = description;
        this.keyCode = keyCode;
        this.keyCodeDefault = keyCode;
        this.keyCategory = category;
        KEYBIND_ARRAY.add(this);
        HASH.put(keyCode, this);
        KEYBIND_SET.add(category);
    }

    public boolean isKeyDown() {
        return this.pressed;
    }

    public String getKeyCategory() {
        return this.keyCategory;
    }

    public boolean isPressed() {
        if (this.pressTime == 0) {
            return false;
        } else {
            --this.pressTime;
            return true;
        }
    }

    private void unpressKey() {
        this.pressTime = 0;
        this.pressed = false;
    }

    public static List<KeyBinding> getKeybindArray() {
        return KEYBIND_ARRAY;
    }

    public String getKeyDescription() {
        return this.keyDescription;
    }

    public int getKeyCodeDefault() {
        return this.keyCodeDefault;
    }

    public int getKeyCode() {
        return this.keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    @Override
    public int compareTo(KeyBinding p_compareTo_1_) {
        int i = I18n.format(this.keyCategory).compareTo(I18n.format(p_compareTo_1_.keyCategory));

        if (i == 0) {
            i = I18n.format(this.keyDescription).compareTo(I18n.format(p_compareTo_1_.keyDescription));
        }

        return i;
    }
}
