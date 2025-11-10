package net.optifine.util;

public class MemoryMonitor {
    private static final long MB = 1048576L;
    private static long startTimeMs = System.currentTimeMillis();
    private static long startMemory = getMemoryUsed();
    private static long lastTimeMs = startTimeMs;
    private static long lastMemory = startMemory;
    private static int memBytesSec = 0;

    public static void update() {
        long i = System.currentTimeMillis();
        long j = getMemoryUsed();
        boolean gcEvent = j < lastMemory;

        if (gcEvent) {
            long k = lastTimeMs - startTimeMs;
            long l = lastMemory - startMemory;
            double d0 = k / 1000.0D;
            int i1 = (int) (l / d0);

            if (i1 > 0) {
                memBytesSec = i1;
            }

            startTimeMs = i;
            startMemory = j;
        }

        lastTimeMs = i;
        lastMemory = j;
    }

    private static long getMemoryUsed() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    public static long getAllocationRateMb() {
        return memBytesSec / MB;
    }
}
