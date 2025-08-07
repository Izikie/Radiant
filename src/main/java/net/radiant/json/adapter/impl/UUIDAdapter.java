package net.radiant.json.adapter.impl;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import net.radiant.json.adapter.AbstractJsonAdapter;

import java.lang.reflect.Type;
import java.util.UUID;

public class UUIDAdapter extends AbstractJsonAdapter<UUID> {

    @Override
    public UUID readObject(JSONReader reader, Type fieldType, Object fieldName, long features) {
        return fromString(reader.readString());
    }

    @Override
    public void writeObject(JSONWriter writer, UUID object, Object fieldName, Type fieldType, long features) {
        writer.writeString(fromUUID(object));
    }

    public static String fromUUID(UUID value) {
        return value.toString().replace("-", "");
    }

    public static UUID fromString(String input) {
        return UUID.fromString(input.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }
}
