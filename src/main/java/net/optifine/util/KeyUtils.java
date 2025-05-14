package net.optifine.util;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.settings.KeyBinding;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class KeyUtils {
    public static void fixKeyConflicts(KeyBinding[] keys, KeyBinding[] priorityKeys) {
        IntSet keyCodes = new IntOpenHashSet();

        for (KeyBinding key : priorityKeys) {
            keyCodes.add(key.getKeyCode());
        }

        Set<KeyBinding> nonPriorityKeys = new HashSet<>(Arrays.asList(keys));
        nonPriorityKeys.removeAll(Arrays.asList(priorityKeys));

        for (KeyBinding keybinding1 : nonPriorityKeys) {
            if (keyCodes.contains(keybinding1.getKeyCode())) {
                keybinding1.setKeyCode(0);
            }
        }
    }
}
