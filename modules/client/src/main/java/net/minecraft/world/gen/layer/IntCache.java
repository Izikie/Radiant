package net.minecraft.world.gen.layer;

import java.util.ArrayList;
import java.util.List;

public class IntCache {
    private static int intCacheSize = 256;
    private static final List<int[]> FREE_SMALL_ARRAYS = new ArrayList<>();
    private static final List<int[]> IN_USE_SMALL_ARRAYS = new ArrayList<>();
    private static final List<int[]> FREE_LARGE_ARRAYS = new ArrayList<>();
    private static final List<int[]> IN_USE_LARGE_ARRAYS = new ArrayList<>();

    public static synchronized int[] getIntCache(int p_76445_0_) {
        if (p_76445_0_ <= 256) {
            if (FREE_SMALL_ARRAYS.isEmpty()) {
                int[] aint4 = new int[256];
                IN_USE_SMALL_ARRAYS.add(aint4);
                return aint4;
            } else {
                int[] aint3 = FREE_SMALL_ARRAYS.removeLast();
                IN_USE_SMALL_ARRAYS.add(aint3);
                return aint3;
            }
        } else if (p_76445_0_ > intCacheSize) {
            intCacheSize = p_76445_0_;
            FREE_LARGE_ARRAYS.clear();
            IN_USE_LARGE_ARRAYS.clear();
            int[] aint2 = new int[intCacheSize];
            IN_USE_LARGE_ARRAYS.add(aint2);
            return aint2;
        } else if (FREE_LARGE_ARRAYS.isEmpty()) {
            int[] aint1 = new int[intCacheSize];
            IN_USE_LARGE_ARRAYS.add(aint1);
            return aint1;
        } else {
            int[] aint = FREE_LARGE_ARRAYS.removeLast();
            IN_USE_LARGE_ARRAYS.add(aint);
            return aint;
        }
    }

    public static synchronized void resetIntCache() {
        if (!FREE_LARGE_ARRAYS.isEmpty()) {
            FREE_LARGE_ARRAYS.removeLast();
        }

        if (!FREE_SMALL_ARRAYS.isEmpty()) {
            FREE_SMALL_ARRAYS.removeLast();
        }

        FREE_LARGE_ARRAYS.addAll(IN_USE_LARGE_ARRAYS);
        FREE_SMALL_ARRAYS.addAll(IN_USE_SMALL_ARRAYS);
        IN_USE_LARGE_ARRAYS.clear();
        IN_USE_SMALL_ARRAYS.clear();
    }

    public static synchronized String getCacheSizes() {
        return "cache: " + FREE_LARGE_ARRAYS.size() + ", tcache: " + FREE_SMALL_ARRAYS.size() + ", allocated: " + IN_USE_LARGE_ARRAYS.size() + ", tallocated: " + IN_USE_SMALL_ARRAYS.size();
    }
}
