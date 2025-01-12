package net.minecraft.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.apache.logging.log4j.Logger;

public class Util {
    public static EnumOS getOSType() {
        String os = System.getProperty("os.name").toLowerCase();
        return switch (os) {
            case "win" -> EnumOS.WINDOWS;
            case "mac" -> EnumOS.OSX;
            case "linux", "unix" -> EnumOS.LINUX;
            default -> EnumOS.UNKNOWN;
        };
    }

    public static <V> V runTask(FutureTask<V> task, Logger logger) {
        try {
            task.run();
            return task.get();
        } catch (ExecutionException executionexception) {
            logger.fatal("Error executing task", executionexception);

            if (executionexception.getCause() instanceof OutOfMemoryError outofmemoryerror) {
                throw outofmemoryerror;
            }
        } catch (InterruptedException interruptedexception) {
            logger.fatal("Error executing task", interruptedexception);
        }

        return null;
    }

    public enum EnumOS {
        LINUX,
        WINDOWS,
        OSX,
        UNKNOWN
    }
}
