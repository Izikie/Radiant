package net.optifine.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.optifine.Log;

import java.util.HashMap;
import java.util.Map;

public class EntityUtils {
    private static final Map<Class<?>, Integer> mapIdByClass = new HashMap<>();
    private static final Object2IntMap<String> mapIdByName = new Object2IntOpenHashMap<>();
    private static final Map<String, Class<?>> mapClassByName = new HashMap<>();

    static {
        for (int i = 0; i < 1000; ++i) {
            Class<?> oclass = EntityList.getClassFromID(i);

            if (oclass != null) {
                String s = EntityList.getStringFromID(i);

                if (s != null) {
                    if (mapIdByClass.containsKey(oclass)) {
                        Log.error("Duplicate entity class: " + oclass + ", id1: " + mapIdByClass.get(oclass) + ", id2: " + i);
                    }

                    if (mapIdByName.containsKey(s)) {
                        Log.error("Duplicate entity name: " + s + ", id1: " + mapIdByName.get(s) + ", id2: " + i);
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
        Integer integer = mapIdByClass.get(cls);
        return integer == null ? -1 : integer;
    }

    public static int getEntityIdByName(String name) {
        Integer integer = mapIdByName.get(name);
        return integer == null ? -1 : integer;
    }

    public static Class<?> getEntityClassByName(String name) {
        return mapClassByName.get(name);
    }
}
