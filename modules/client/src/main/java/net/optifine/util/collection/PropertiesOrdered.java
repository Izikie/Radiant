package net.optifine.util.collection;

import java.util.*;

public class PropertiesOrdered extends Properties {
    private final Set<Object> keysOrdered = new LinkedHashSet<>();

    @Override
    public synchronized Object put(Object key, Object value) {
        this.keysOrdered.add(key);
        return super.put(key, value);
    }

    @Override
    public Set<Object> keySet() {
        Set<Object> set = super.keySet();
        this.keysOrdered.retainAll(set);
        return Collections.unmodifiableSet(this.keysOrdered);
    }

    @Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(this.keySet());
    }
}
