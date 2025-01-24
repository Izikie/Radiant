package net.minecraft.crash;

public class ReportedException extends RuntimeException {
    private final CrashReport reportedCrashException;

    public ReportedException(CrashReport report) {
        this.reportedCrashException = report;
    }

    public CrashReport getCrashReport() {
        return this.reportedCrashException;
    }

    public Throwable getCause() {
        return this.reportedCrashException.getCrashCause();
    }

    public String getMessage() {
        return this.reportedCrashException.getDescription();
    }
}
