package net.radiant.json.adapter.impl;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.mojang.authlib.GameProfile;
import net.radiant.json.adapter.AbstractJsonAdapter;

import java.lang.reflect.Type;
import java.util.UUID;

public class GameProfileAdapter extends AbstractJsonAdapter<GameProfile> {

    @Override
    public GameProfile readObject(JSONReader reader, Type fieldType, Object fieldName, long features) {
        UUID id = null;
        String name = null;

        if (reader.nextIfObjectStart()) {
            while (!reader.nextIfObjectEnd()) {
                String key = reader.readFieldName();
                switch (key) {
                    case "id" -> id = reader.read(UUID.class);
                    case "name" -> name = reader.readString();
                    default -> reader.skipValue();
                }
            }
        }

        return new GameProfile(id, name);
    }

    @Override
    public void writeObject(JSONWriter writer, GameProfile object, Object fieldName, Type fieldType, long features) {
        writer.startObject();

        if (object.getId() != null) {
            writer.writeName("id");
            writer.writeAs(object.getId(), UUID.class);
        }

        if (object.getName() != null) {
            writer.writeName("name");
            writer.writeString(object.getName());
        }

        writer.endObject();
    }
}
