package net.radiant.json.adapter;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import java.lang.reflect.Type;

public class PropertyMapAdapter extends AbstractJsonAdapter<PropertyMap> {

    @Override
    public PropertyMap readObject(JSONReader reader, Type fieldType, Object fieldName, long features) {
        PropertyMap result = new PropertyMap();

        if (reader.nextIfObjectStart()) {
            while (!reader.nextIfObjectEnd()) {
                String key = reader.readFieldName();
                if (reader.nextIfArrayStart()) {
                    while (!reader.nextIfArrayEnd()) {
                        result.put(key, new Property(key, reader.readString()));
                    }
                } else {
                    reader.skipValue();
                }

            }
        } else if (reader.nextIfArrayStart()) {
            while (!reader.nextIfArrayEnd()) {
                if (reader.nextIfObjectStart()) {
                    String name = null;
                    String value = null;
                    String signature = null;
                    while (!reader.nextIfObjectEnd()) {
                        String key = reader.readFieldName();
                        switch (key) {
                            case "name" -> name = reader.readString();
                            case "value" -> value = reader.readString();
                            case "signature" -> signature = reader.readString();
                        }
                    }
                    if (name != null && value != null) {
                        if (signature != null) {
                            result.put(name, new Property(name, value, signature));
                        } else {
                            result.put(name, new Property(name, value));
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void writeObject(JSONWriter writer, PropertyMap object, Object fieldName, Type fieldType, long features) {
        writer.startArray();

        for (Property property : object.values()) {
            writer.startObject();

            writer.writeName("name");
            writer.writeString(property.getName());

            writer.writeName("value");
            writer.writeString(property.getValue());

            if (property.hasSignature()) {
                writer.writeName("signature");
                writer.writeString(property.getSignature());
            }

            writer.endObject();
        }

        writer.endArray();
    }
}
