package net.radiant.json.adapter.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.radiant.json.adapter.AbstractJsonAdapter;

import java.lang.reflect.Type;

public class PropertyMapAdapter extends AbstractJsonAdapter<PropertyMap> {

    @Override
    public PropertyMap readObject(JSONReader reader, Type fieldType, Object fieldName, long features) {
        Object obj = reader.readAny(); // Read entire JSON into Java object tree

        PropertyMap result = new PropertyMap();

        if (obj instanceof JSONObject jsonObject) {
            for (String key : jsonObject.keySet()) {
                Object value = jsonObject.get(key);

                if (value instanceof JSONArray jsonArray) {
                    for (Object item : jsonArray) {
                        if (item instanceof String str) {
                            result.put(key, new Property(key, str));
                        }
                    }
                }
            }
        } else if (obj instanceof JSONArray jsonArray) {
            for (Object item : jsonArray) {
                if (item instanceof JSONObject propObj) {
                    String name = propObj.getString("name");
                    String value = propObj.getString("value");
                    String signature = propObj.getString("signature");

                    if (signature != null) {
                        result.put(name, new Property(name, value, signature));
                    } else {
                        result.put(name, new Property(name, value));
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
