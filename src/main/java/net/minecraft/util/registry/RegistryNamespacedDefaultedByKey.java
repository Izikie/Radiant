package net.minecraft.util.registry;

import java.util.Objects;

public class RegistryNamespacedDefaultedByKey<K, V> extends RegistryNamespaced<K, V> {
    private final K defaultValueKey;
    private V defaultValue;

    public RegistryNamespacedDefaultedByKey(K defaultValueKeyIn) {
        this.defaultValueKey = defaultValueKeyIn;
    }

    @Override
    public void register(int id, K key, V value) {
        if (this.defaultValueKey.equals(key)) {
            this.defaultValue = value;
        }

        super.register(id, key, value);
    }

    public void validateKey() {
        Objects.requireNonNull(this.defaultValueKey);
    }

    @Override
    public V getObject(K name) {
        V v = super.getObject(name);
        return v == null ? this.defaultValue : v;
    }

    @Override
    public V getObjectById(int id) {
        V v = super.getObjectById(id);
        return v == null ? this.defaultValue : v;
    }
}
