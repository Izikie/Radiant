package net.radiant.logger;

public class LoggerError extends Error {

    public LoggerError() {
    }

    public LoggerError(String message) {
        super(message);
    }

    public LoggerError(String message, Throwable cause) {
        super(message, cause);
    }

    public LoggerError(Throwable cause) {
        super(cause);
    }

    public LoggerError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
