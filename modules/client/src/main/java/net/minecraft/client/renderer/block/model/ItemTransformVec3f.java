package net.minecraft.client.renderer.block.model;

import com.google.gson.*;
import net.minecraft.util.json.JsonUtils;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

import java.lang.reflect.Type;

public record ItemTransformVec3f(Vector3f rotation, Vector3f translation, Vector3f scale) {
    public static final ItemTransformVec3f DEFAULT = new ItemTransformVec3f(new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));

    public ItemTransformVec3f(Vector3f rotation, Vector3f translation, Vector3f scale) {
        this.rotation = new Vector3f(rotation);
        this.translation = new Vector3f(translation);
        this.scale = new Vector3f(scale);
    }

    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (this.getClass() != p_equals_1_.getClass()) {
            return false;
        } else {
            ItemTransformVec3f itemtransformvec3f = (ItemTransformVec3f) p_equals_1_;
            return this.rotation.equals(itemtransformvec3f.rotation) && (this.scale.equals(itemtransformvec3f.scale) && this.translation.equals(itemtransformvec3f.translation));
        }
    }

    static class Deserializer implements JsonDeserializer<ItemTransformVec3f> {
        private static final Vector3f ROTATION_DEFAULT = new Vector3f(0.0F, 0.0F, 0.0F);
        private static final Vector3f TRANSLATION_DEFAULT = new Vector3f(0.0F, 0.0F, 0.0F);
        private static final Vector3f SCALE_DEFAULT = new Vector3f(1.0F, 1.0F, 1.0F);

        @Override
        public ItemTransformVec3f deserialize(JsonElement element, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject jsonObject = element.getAsJsonObject();

            Vector3f rotation = this.parseVector3f(jsonObject, "rotation", ROTATION_DEFAULT);

            Vector3f translation = this.parseVector3f(jsonObject, "translation", TRANSLATION_DEFAULT)
                    .mul(0.0625F);
            translation.x = Math.clamp(translation.x, -1.5F, 1.5F);
            translation.y = Math.clamp(translation.y, -1.5F, 1.5F);
            translation.z = Math.clamp(translation.z, -1.5F, 1.5F);

            Vector3f scale = this.parseVector3f(jsonObject, "scale", SCALE_DEFAULT);
            scale.x = Math.clamp(scale.x, -4.0F, 4.0F);
            scale.y = Math.clamp(scale.y, -4.0F, 4.0F);
            scale.z = Math.clamp(scale.z, -4.0F, 4.0F);

            return new ItemTransformVec3f(rotation, translation, scale);
        }

        private Vector3f parseVector3f(JsonObject jsonObject, String key, Vector3f defaultValue) {
            if (!jsonObject.has(key)) {
                return defaultValue;
            }

            JsonArray jsonArray = JsonUtils.getJsonArray(jsonObject, key);
            if (jsonArray.size() != 3) {
                throw new JsonParseException("Expected 3 " + key + " values, found: " + jsonArray.size());
            }

            float x = JsonUtils.getFloat(jsonArray.get(0), key + "[0]");
            float y = JsonUtils.getFloat(jsonArray.get(1), key + "[1]");
            float z = JsonUtils.getFloat(jsonArray.get(2), key + "[2]");

            return new Vector3f(x, y, z);
        }
    }
}
