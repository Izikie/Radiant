package net.radiant.logger;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RadiantLoggerFactory implements ILoggerFactory {

    private final Map<String, Logger> loggers = new ConcurrentHashMap<>();

    public RadiantLoggerFactory() {}

    @Override
    public Logger getLogger(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Logger name cannot be null");
        }
        return this.loggers.computeIfAbsent(name, s -> new RadiantLogger(s, RadiantLoggerService.get().getFile()));
    }

}
