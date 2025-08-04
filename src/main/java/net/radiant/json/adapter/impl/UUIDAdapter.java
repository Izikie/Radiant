package net.radiant.json.adapter.impl;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.mojang.util.UUIDTypeAdapter;
import net.radiant.json.adapter.AbstractJsonAdapter;

import java.lang.reflect.Type;
import java.util.UUID;

public class UUIDAdapter extends AbstractJsonAdapter<UUID> {

    @Override
    public UUID readObject(JSONReader reader, Type fieldType, Object fieldName, long features) {
        String input = reader.readString();
        return UUIDTypeAdapter.fromString(input);
    }

    @Override
    public void writeObject(JSONWriter writer, UUID object, Object fieldName, Type fieldType, long features) {
        writer.writeString(UUIDTypeAdapter.fromUUID(object));
    }
}
