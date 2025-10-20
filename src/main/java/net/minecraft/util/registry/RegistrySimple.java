package net.minecraft.util.registry;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;

public class RegistrySimple<K, V> implements IRegistry<K, V> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrySimple.class);
    protected final Map<K, V> registryObjects = this.createUnderlyingMap();

    protected Map<K, V> createUnderlyingMap() {
        return new HashMap<>();
    }

    @Override
    public V getObject(K name) {
        return this.registryObjects.get(name);
    }

    @Override
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

    @Override
    public Iterator<V> iterator() {
        return this.registryObjects.values().iterator();
    }
}
