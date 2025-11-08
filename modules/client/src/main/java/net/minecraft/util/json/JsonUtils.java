package net.minecraft.util.json;

import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;

public class JsonUtils {
    public static boolean isString(JsonObject jsonObject, String key) {
        return isJsonPrimitive(jsonObject, key) && jsonObject.getAsJsonPrimitive(key).isString();
    }

    public static boolean isString(JsonElement jsonElement) {
        return jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isString();
    }

    public static boolean isBoolean(JsonObject jsonObject, String key) {
        return isJsonPrimitive(jsonObject, key) && jsonObject.getAsJsonPrimitive(key).isBoolean();
    }

    public static boolean isJsonArray(JsonObject jsonObject, String key) {
        return hasField(jsonObject, key) && jsonObject.get(key).isJsonArray();
    }

    public static boolean isJsonPrimitive(JsonObject jsonObject, String key) {
        return hasField(jsonObject, key) && jsonObject.get(key).isJsonPrimitive();
    }

    public static boolean hasField(JsonObject jsonObject, String key) {
        return jsonObject != null && jsonObject.get(key) != null;
    }

    public static String getString(JsonElement jsonElement, String key) {
        if (jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsString();
        } else {
            throw new JsonSyntaxException("Expected " + key + " to be a string, was " + toString(jsonElement));
        }
    }

    public static String getString(JsonObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            return getString(jsonObject.get(key), key);
        } else {
            throw new JsonSyntaxException("Missing " + key + ", expected to find a string");
        }
    }

    public static String getString(JsonObject jsonObject, String key, String defaultValue) {
        return jsonObject.has(key) ? getString(jsonObject.get(key), key) : defaultValue;
    }

    public static boolean getBoolean(JsonElement jsonElement, String key) {
        if (jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsBoolean();
        } else {
            throw new JsonSyntaxException("Expected " + key + " to be a Boolean, was " + toString(jsonElement));
        }
    }

    public static boolean getBoolean(JsonObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            return getBoolean(jsonObject.get(key), key);
        } else {
            throw new JsonSyntaxException("Missing " + key + ", expected to find a Boolean");
        }
    }

    public static boolean getBoolean(JsonObject jsonObject, String key, boolean defaultValue) {
        return jsonObject.has(key) ? getBoolean(jsonObject.get(key), key) : defaultValue;
    }

    public static float getFloat(JsonElement jsonElement, String key) {
        if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isNumber()) {
            return jsonElement.getAsFloat();
        } else {
            throw new JsonSyntaxException("Expected " + key + " to be a Float, was " + toString(jsonElement));
        }
    }

    public static float getFloat(JsonObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            return getFloat(jsonObject.get(key), key);
        } else {
            throw new JsonSyntaxException("Missing " + key + ", expected to find a Float");
        }
    }

    public static float getFloat(JsonObject jsonObject, String key, float defaultValue) {
        return jsonObject.has(key) ? getFloat(jsonObject.get(key), key) : defaultValue;
    }

    public static int getInt(JsonElement jsonElement, String key) {
        if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isNumber()) {
            return jsonElement.getAsInt();
        } else {
            throw new JsonSyntaxException("Expected " + key + " to be a Int, was " + toString(jsonElement));
        }
    }

    public static int getInt(JsonObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            return getInt(jsonObject.get(key), key);
        } else {
            throw new JsonSyntaxException("Missing " + key + ", expected to find a Int");
        }
    }

    public static int getInt(JsonObject jsonObject, String key, int defaultValue) {
        return jsonObject.has(key) ? getInt(jsonObject.get(key), key) : defaultValue;
    }

    public static JsonObject getJsonObject(JsonElement jsonElement, String key) {
        if (jsonElement.isJsonObject()) {
            return jsonElement.getAsJsonObject();
        } else {
            throw new JsonSyntaxException("Expected " + key + " to be a JsonObject, was " + toString(jsonElement));
        }
    }

    public static JsonObject getJsonObject(JsonObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            return getJsonObject(jsonObject.get(key), key);
        } else {
            throw new JsonSyntaxException("Missing " + key + ", expected to find a JsonObject");
        }
    }

    public static JsonObject getJsonObject(JsonObject jsonObject, String key, JsonObject defaultValue) {
        return jsonObject.has(key) ? getJsonObject(jsonObject.get(key), key) : defaultValue;
    }

    public static JsonArray getJsonArray(JsonElement jsonElement, String key) {
        if (jsonElement.isJsonArray()) {
            return jsonElement.getAsJsonArray();
        } else {
            throw new JsonSyntaxException("Expected " + key + " to be a JsonArray, was " + toString(jsonElement));
        }
    }

    public static JsonArray getJsonArray(JsonObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            return getJsonArray(jsonObject.get(key), key);
        } else {
            throw new JsonSyntaxException("Missing " + key + ", expected to find a JsonArray");
        }
    }

    public static JsonArray getJsonArray(JsonObject jsonObject, String key, JsonArray defaultValue) {
        return jsonObject.has(key) ? getJsonArray(jsonObject.get(key), key) : defaultValue;
    }

    public static String toString(JsonElement jsonElement) {
        String s = StringUtils.abbreviateMiddle(String.valueOf(jsonElement), "...", 10);

        if (jsonElement == null) {
            return "null (missing)";
        } else if (jsonElement.isJsonNull()) {
            return "null (json)";
        } else if (jsonElement.isJsonArray()) {
            return "an array (" + s + ")";
        } else if (jsonElement.isJsonObject()) {
            return "an object (" + s + ")";
        } else {
            if (jsonElement.isJsonPrimitive()) {
                JsonPrimitive jsonprimitive = jsonElement.getAsJsonPrimitive();

                if (jsonprimitive.isNumber()) {
                    return "a number (" + s + ")";
                }

                if (jsonprimitive.isBoolean()) {
                    return "a boolean (" + s + ")";
                }
            }

            return s;
        }
    }
}
