package net.radiant.json;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import net.radiant.json.adapter.AbstractJsonAdapter;
import net.radiant.json.adapter.GameProfileAdapter;
import net.radiant.json.adapter.PropertyMapAdapter;
import net.radiant.json.adapter.UuidAdapter;

import java.lang.reflect.Type;
import java.util.UUID;

public class JsonRegistration {

    static {
        registerAdapter(UUID.class, new UuidAdapter());
        registerAdapter(GameProfile.class, new GameProfileAdapter());
        registerAdapter(PropertyMap.class, new PropertyMapAdapter());
    }

    private static void registerAdapter(Type type, AbstractJsonAdapter<?> adapter) {
        JSON.register(type, (ObjectReader<?>)adapter);
        JSON.register(type, (ObjectWriter<?>)adapter);
    }

    public static void init() {
        // Does nothing, used to call the static initializer
    }

}
