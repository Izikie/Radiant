package net.radiant.logger;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public class RadiantLoggerService implements SLF4JServiceProvider {

    private static final String REQUESTED_API_VERSION = "2.0.17";

    private static RadiantLoggerService instance;

    private final ILoggerFactory loggerFactory;
    private final IMarkerFactory markerFactory;
    private final MDCAdapter mdcAdapter;

    private final RadiantLoggerFile file;

    private boolean errorEnabled, warnEnabled, infoEnabled, debugEnabled, traceEnabled;

    public RadiantLoggerService() {
        this.loggerFactory = new RadiantLoggerFactory();
        this.markerFactory = new BasicMarkerFactory();
        this.mdcAdapter = new BasicMDCAdapter();
        this.file = new RadiantLoggerFile();

        this.errorEnabled = true;
        this.warnEnabled = true;
        this.infoEnabled = true;
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }

    @Override
    public String getRequestedApiVersion() {
        return REQUESTED_API_VERSION;
    }

    public RadiantLoggerFile getFile() {
        return file;
    }

    @Override
    public void initialize() {
        instance = this;

        this.file.init();
    }

    public boolean isErrorEnabled() {
        return errorEnabled;
    }

    public void setErrorEnabled(boolean errorEnabled) {
        this.errorEnabled = errorEnabled;
    }

    public boolean isWarnEnabled() {
        return warnEnabled;
    }

    public void setWarnEnabled(boolean warnEnabled) {
        this.warnEnabled = warnEnabled;
    }

    public boolean isInfoEnabled() {
        return infoEnabled;
    }

    public void setInfoEnabled(boolean infoEnabled) {
        this.infoEnabled = infoEnabled;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    public static RadiantLoggerService get() {
        return instance;
    }
}
