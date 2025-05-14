package net.minecraft.util;

import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class Util {
    public static OperatingSystem getOSType() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (os.contains("mac") || os.contains("darwin")) {
            return OperatingSystem.MAC;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix") || os.contains("unix") || os.contains("linux")) {
            return OperatingSystem.UNIX;
        } else {
            return OperatingSystem.UNKNOWN;
        }
    }

    public static void openFolder(File path) {
        String pathString = path.getAbsolutePath();
        String cmd = switch (getOSType()) {
            case WINDOWS -> "explorer.exe " + pathString;
            case MAC -> "open" + pathString;
            case UNIX -> "xdg-open " + pathString;
            default -> throw new UnsupportedOperationException("Unsupported OS");
        };
        try {
            Runtime.getRuntime().exec(cmd.split(" "));
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public enum OperatingSystem {
        UNKNOWN,
        WINDOWS,
        UNIX,
        MAC
    }
}
