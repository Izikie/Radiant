package net.radiant.dev.nativeimage;

public class FailedExerciseException extends RuntimeException {

    public FailedExerciseException(String message) {
        super(message);
    }

    public FailedExerciseException(String message, Exception e) {
        super(message, e);
    }
}
