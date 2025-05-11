package net.minecraft.util;

import com.google.common.collect.Maps;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegistrySimple<K, V> implements IRegistry<K, V> {
    private static final Logger LOGGER = LogManager.getLogger();
    protected final Map<K, V> registryObjects = this.createUnderlyingMap();

    protected Map<K, V> createUnderlyingMap() {
        return new HashMap<>();
    }

    public V getObject(K name) {
        return this.registryObjects.get(name);
    }

    public void putObject(K key, V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        if (this.registryObjects.containsKey(key)) {
            LOGGER.debug("Adding duplicate key '{}' to registry", key);
        }

        this.registryObjects.put(key, value);
    }

    public Set<K> getKeys() {
        return Collections.unmodifiableSet(this.registryObjects.keySet());
    }

    public boolean containsKey(K key) {
        return this.registryObjects.containsKey(key);
    }

    public Iterator<V> iterator() {
        return this.registryObjects.values().iterator();
    }
}
