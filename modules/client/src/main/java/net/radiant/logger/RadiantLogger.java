package net.radiant.logger;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class RadiantLogger implements Logger {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String name;
    private final RadiantLoggerFile file;

    public RadiantLogger(String name, RadiantLoggerFile file) {
        String[] split = name.split("\\.");
        this.name = split[split.length - 1];
        this.file = file;
    }

    private void log(Level level, @Nullable Marker marker, String msg, @Nullable Throwable t) {
        if (!isLevelEnabled(level, marker)) {
            return;
        }

        String time = TIME_FORMAT.format(LocalDateTime.now());
        String thread = Thread.currentThread().getName();

        AnsiStyle levelColor = switch (level) {
            case ERROR -> AnsiStyle.RED;
            case WARN -> AnsiStyle.YELLOW;
            case INFO, DEBUG -> AnsiStyle.GREEN;
            case TRACE -> AnsiStyle.BLUE;
        };
        AnsiStyle messageColor = (level == Level.ERROR) ? AnsiStyle.RED : AnsiStyle.RESET;

        String timeOutput = AnsiStyle.BLUE.encase("[" + time + "]");
        String levelOutput = levelColor.encase(String.format("[%s/%s]", thread, level.name()));
        String nameOutput = AnsiStyle.CYAN.encase("(" + this.name + ")");
        String messageOutput = messageColor.encase(msg);

        String output = String.format("%s %s %s %s", timeOutput, levelOutput, nameOutput, messageOutput);

        PrintStream printStream = (level == Level.ERROR) ? System.err : System.out;
        printStream.println(output);
        if (t != null) {
            t.printStackTrace(printStream);
        }

        this.file.log(level, msg, time, thread, t);
    }

    private void log(Level level, String msg) {
        log(level, null, msg, null);
    }

    private void log(Level level, Marker marker, String msg) {
        log(level, marker, msg, null);
    }

    private String formatMessage(String format, Object... args) {
        final StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        while (i < format.length()) {
            if (format.charAt(i) == '{' && i + 1 < format.length() && format.charAt(i + 1) == '}') {
                String text;
                if (argIndex < args.length) {
                    text = Objects.toString(args[argIndex]);
                    argIndex++;
                } else {
                    text = "{}";
                }
                sb.append(text);
                i += 2;
            } else {
                sb.append(format.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    private boolean isLevelEnabled(Level level, @Nullable Marker marker) {
        if (marker != null) {
            return switch (level) {
                case TRACE -> isTraceEnabled(marker);
                case DEBUG -> isDebugEnabled(marker);
                case INFO -> isInfoEnabled(marker);
                case WARN -> isWarnEnabled(marker);
                case ERROR -> isErrorEnabled(marker);
            };
        } else {
            return switch (level) {
                case TRACE -> isTraceEnabled();
                case DEBUG -> isDebugEnabled();
                case INFO -> isInfoEnabled();
                case WARN -> isWarnEnabled();
                case ERROR -> isErrorEnabled();
            };
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isTraceEnabled() {
        return RadiantLoggerService.get().isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        log(Level.TRACE, msg);
    }

    @Override
    public void trace(String format, Object arg) {
        log(Level.TRACE, formatMessage(format, arg));
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        log(Level.TRACE, formatMessage(format, arg1, arg2));
    }

    @Override
    public void trace(String format, Object... arguments) {
        log(Level.TRACE, formatMessage(format, arguments));
    }


    @Override
    public void trace(String msg, Throwable t) {
        log(Level.TRACE, null, msg, t);
    }


    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isTraceEnabled();
    }

    @Override
    public void trace(Marker marker, String msg) {
        log(Level.TRACE, marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        log(Level.TRACE, marker, formatMessage(format, arg));
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        log(Level.TRACE, marker, formatMessage(format, arg1, arg2));
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        log(Level.TRACE, marker, formatMessage(format, argArray));
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        log(Level.TRACE, marker, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return RadiantLoggerService.get().isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        log(Level.DEBUG, msg);
    }

    @Override
    public void debug(String format, Object arg) {
        log(Level.DEBUG, formatMessage(format, arg));
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        log(Level.DEBUG, formatMessage(format, arg1, arg2));
    }

    @Override
    public void debug(String format, Object... arguments) {
        log(Level.DEBUG, formatMessage(format, arguments));
    }

    @Override
    public void debug(String msg, Throwable t) {
        log(Level.DEBUG, null, msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isDebugEnabled();
    }

    @Override
    public void debug(Marker marker, String msg) {
        log(Level.DEBUG, marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        log(Level.DEBUG, marker, formatMessage(format, arg));
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        log(Level.DEBUG, marker, formatMessage(format, arg1, arg2));
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        log(Level.DEBUG, marker, formatMessage(format, arguments));
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        log(Level.DEBUG, marker, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return RadiantLoggerService.get().isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        log(Level.INFO, msg);
    }

    @Override
    public void info(String format, Object arg) {
        log(Level.INFO, formatMessage(format, arg));
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        log(Level.INFO, formatMessage(format, arg1, arg2));
    }

    @Override
    public void info(String format, Object... arguments) {
        log(Level.INFO, formatMessage(format, arguments));
    }

    @Override
    public void info(String msg, Throwable t) {
        log(Level.INFO, null, msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isInfoEnabled();
    }

    @Override
    public void info(Marker marker, String msg) {
        log(Level.INFO, marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        log(Level.INFO, marker, formatMessage(format, arg));
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        log(Level.INFO, marker, formatMessage(format, arg1, arg2));
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        log(Level.INFO, marker, formatMessage(format, arguments));
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        log(Level.INFO, marker, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return RadiantLoggerService.get().isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        log(Level.WARN, msg);
    }

    @Override
    public void warn(String format, Object arg) {
        log(Level.WARN, formatMessage(format, arg));
    }

    @Override
    public void warn(String format, Object... arguments) {
        log(Level.WARN, formatMessage(format, arguments));
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        log(Level.WARN, formatMessage(format, arg1, arg2));
    }

    @Override
    public void warn(String msg, Throwable t) {
        log(Level.WARN, null, msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isWarnEnabled();
    }

    @Override
    public void warn(Marker marker, String msg) {
        log(Level.WARN, marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        log(Level.WARN, marker, formatMessage(format, arg));
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        log(Level.WARN, marker, formatMessage(format, arg1, arg2));
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        log(Level.WARN, marker, formatMessage(format, arguments));
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        log(Level.WARN, marker, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return RadiantLoggerService.get().isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        log(Level.ERROR, msg);
    }

    @Override
    public void error(String format, Object arg) {
        log(Level.ERROR, formatMessage(format, arg));
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        log(Level.ERROR, formatMessage(format, arg1, arg2));
    }

    @Override
    public void error(String format, Object... arguments) {
        log(Level.ERROR, formatMessage(format, arguments));
    }

    @Override
    public void error(String msg, Throwable t) {
        log(Level.ERROR, null, msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isErrorEnabled();
    }

    @Override
    public void error(Marker marker, String msg) {
        log(Level.ERROR, marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        log(Level.ERROR, marker, formatMessage(format, arg));
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        log(Level.ERROR, marker, formatMessage(format, arg1, arg2));
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        log(Level.ERROR, marker, formatMessage(format, arguments));
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        log(Level.ERROR, marker, msg, t);
    }
}
