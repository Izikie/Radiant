package com.mojang.authlib.properties;

import com.google.common.collect.ForwardingMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Map;

public class PropertyMap extends ForwardingMultimap<String, Property> {
    private final Multimap<String, Property> properties = LinkedHashMultimap.create();

    protected Multimap<String, Property> delegate() {
        return this.properties;
    }

    public static class Serializer
            implements JsonSerializer<PropertyMap>, JsonDeserializer<PropertyMap> {
        public PropertyMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            PropertyMap result = new PropertyMap();

            if (json instanceof JsonObject object) {

                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    if (entry.getValue() instanceof JsonArray array) {
                        for (JsonElement element : array) {
                            result.put(entry.getKey(), new Property(entry.getKey(), element.getAsString()));
                        }
                    }
                }
            } else if (json instanceof JsonArray array) {
                for (JsonElement element : array) {
                    if (element instanceof JsonObject object) {
                        String name = object.getAsJsonPrimitive("name").getAsString();
                        String value = object.getAsJsonPrimitive("value").getAsString();

                        if (object.has("signature")) {
                            result.put(name, new Property(name, value, object.getAsJsonPrimitive("signature").getAsString()));
                            continue;
                        }
                        result.put(name, new Property(name, value));
                    }
                }
            }


            return result;
        }


        public JsonElement serialize(PropertyMap src, Type typeOfSrc, JsonSerializationContext context) {
            JsonArray result = new JsonArray();

            for (Property property : src.values()) {
                JsonObject object = new JsonObject();

                object.addProperty("name", property.getName());
                object.addProperty("value", property.getValue());

                if (property.hasSignature()) {
                    object.addProperty("signature", property.getSignature());
                }

                result.add(object);
            }

            return result;
        }
    }
}
