package net.optifine.util;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.optifine.Log;

import java.util.HashMap;
import java.util.Map;

public class EntityUtils {
    private static final Object2IntOpenHashMap<Class<?>> mapIdByClass = new Object2IntOpenHashMap<>();
    private static final Object2IntOpenHashMap<String> mapIdByName = new Object2IntOpenHashMap<>();
    private static final Map<String, Class<?>> mapClassByName = new HashMap<>();

    static {
        mapIdByClass.defaultReturnValue(-1);
        mapIdByName.defaultReturnValue(-1);

        for (int i = 0; i < 1000; ++i) {
            Class<?> oclass = EntityList.getClassFromID(i);

            if (oclass != null) {
                String s = EntityList.getStringFromID(i);

                if (s != null) {
                    if (mapIdByClass.containsKey(oclass)) {
                        Log.error("Duplicate entity class: " + oclass + ", id1: " + mapIdByClass.getInt(oclass) + ", id2: " + i);
                    }

                    if (mapIdByName.containsKey(s)) {
                        Log.error("Duplicate entity name: " + s + ", id1: " + mapIdByName.getInt(s) + ", id2: " + i);
                    }

                    if (mapClassByName.containsKey(s)) {
                        Log.error("Duplicate entity name: " + s + ", class1: " + mapClassByName.get(s) + ", class2: " + oclass);
                    }

                    mapIdByClass.put(oclass, i);
                    mapIdByName.put(s, i);
                    mapClassByName.put(s, oclass);
                }
            }
        }
    }

    public static int getEntityIdByClass(Entity entity) {
        return entity == null ? -1 : getEntityIdByClass(entity.getClass());
    }

    public static int getEntityIdByClass(Class<?> cls) {
        return mapIdByClass.getInt(cls);
    }

    public static int getEntityIdByName(String name) {
        return mapIdByName.getInt(name);
    }

    public static Class<?> getEntityClassByName(String name) {
        return mapClassByName.get(name);
    }
}
