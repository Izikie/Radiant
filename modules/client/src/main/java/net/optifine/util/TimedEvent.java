package net.optifine.util;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

public class TimedEvent {
    private static final Object2LongMap<String> mapEventTimes = new Object2LongOpenHashMap<>();

    public static boolean isActive(String name, long timeIntervalMs) {
        synchronized (mapEventTimes) {
            long i = System.currentTimeMillis();

            long j = mapEventTimes.computeIfAbsent(name, _ -> i);

            if (i < j + timeIntervalMs) {
                return false;
            } else {
                mapEventTimes.put(name, i);
                return true;
            }
        }
    }
}
