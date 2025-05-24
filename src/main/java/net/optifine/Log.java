package net.optifine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log {
    private static final Logger LOGGER = LogManager.getLogger("Optifine");

    public static void info(String s) {
        LOGGER.info("{}", s);
    }

    public static void info(String format, Object... args) {
        String s = String.format(format, args);
        LOGGER.info(s);
    }

    public static void warn(String s) {
        LOGGER.warn(s);
    }

    public static void warn(String format, Object... args) {
        String s = String.format(format, args);
        LOGGER.warn(s);
    }

    public static void warn(String s, Throwable t) {
        LOGGER.warn(s, t);
    }

    public static void error(String s) {
        LOGGER.error(s);
    }

    public static void error(String s, Throwable t) {
        LOGGER.error(s, t);
    }
}
