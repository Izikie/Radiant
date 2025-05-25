package net.optifine.expr;

public class ParseException extends Exception {
    public ParseException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public ParseException(String message) {
        super(message);
    }
}
