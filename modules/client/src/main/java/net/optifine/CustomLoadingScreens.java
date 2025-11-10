package net.optifine;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.packet.api.PacketThreadUtil;
import net.optifine.util.ResUtils;
import net.optifine.util.StrUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Properties;

public class CustomLoadingScreens {
    private static CustomLoadingScreen[] screens = null;
    private static int screensMinDimensionId = 0;

    public static CustomLoadingScreen getCustomLoadingScreen() {
        if (screens == null) {
            return null;
        } else {
            int i = PacketThreadUtil.lastDimensionId;
            int j = i - screensMinDimensionId;
            CustomLoadingScreen customloadingscreen = null;

            if (j >= 0 && j < screens.length) {
                customloadingscreen = screens[j];
            }

            return customloadingscreen;
        }
    }

    public static void update() {
        screens = null;
        screensMinDimensionId = 0;
        Pair<CustomLoadingScreen[], Integer> pair = parseScreens();
        screens = pair.getLeft();
        screensMinDimensionId = pair.getRight();
    }

    private static Pair<CustomLoadingScreen[], Integer> parseScreens() {
        String s = "optifine/gui/loading/background";
        String s1 = ".png";
        String[] astring = ResUtils.collectFiles(s, s1);
        Int2ObjectMap<String> map = new Int2ObjectOpenHashMap<>();

        for (String s2 : astring) {
            String s3 = StrUtils.removePrefixSuffix(s2, s, s1);
            int j = Config.parseInt(s3, Integer.MIN_VALUE);

            if (j == Integer.MIN_VALUE) {
                warn("Invalid dimension ID: " + s3 + ", path: " + s2);
            } else {
                map.put(j, s2);
            }
        }

        int[] keys = map.keySet().toIntArray();
        Arrays.sort(keys);

        if (keys.length == 0) {
            return new ImmutablePair<>(null, 0);
        } else {
            String s5 = "optifine/gui/loading/loading.properties";
            Properties properties = ResUtils.readProperties(s5, "CustomLoadingScreens");
            int k = keys[0];
            int l = keys[keys.length - 1];
            int i1 = l - k + 1;
            CustomLoadingScreen[] acustomloadingscreen = new CustomLoadingScreen[i1];

            for (int integer : keys) {
                String s4 = map.get(integer);
                acustomloadingscreen[integer - k] = CustomLoadingScreen.parseScreen(s4, integer, properties);
            }

            return new ImmutablePair<>(acustomloadingscreen, k);
        }
    }

    public static void warn(String str) {
        Log.error("CustomLoadingScreen: " + str);
    }

    public static void dbg(String str) {
        Log.info("CustomLoadingScreen: " + str);
    }
}
