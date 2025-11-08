package net.minecraft.client.renderer.block.model;

import com.google.gson.*;
import net.minecraft.util.json.JsonUtils;

import java.lang.reflect.Type;

public class BlockFaceUV {
    public float[] uvs;
    public final int rotation;

    public BlockFaceUV(float[] uvsIn, int rotationIn) {
        this.uvs = uvsIn;
        this.rotation = rotationIn;
    }

    public float func_178348_a(int p_178348_1_) {
        if (this.uvs == null) {
            throw new NullPointerException("uvs");
        } else {
            int i = this.func_178347_d(p_178348_1_);
            return i != 0 && i != 1 ? this.uvs[2] : this.uvs[0];
        }
    }

    public float func_178346_b(int p_178346_1_) {
        if (this.uvs == null) {
            throw new NullPointerException("uvs");
        } else {
            int i = this.func_178347_d(p_178346_1_);
            return i != 0 && i != 3 ? this.uvs[3] : this.uvs[1];
        }
    }

    private int func_178347_d(int p_178347_1_) {
        return (p_178347_1_ + this.rotation / 90) % 4;
    }

    public int func_178345_c(int p_178345_1_) {
        return (p_178345_1_ + (4 - this.rotation / 90)) % 4;
    }

    public void setUvs(float[] uvsIn) {
        if (this.uvs == null) {
            this.uvs = uvsIn;
        }
    }

    static class Deserializer implements JsonDeserializer<BlockFaceUV> {
        @Override
        public BlockFaceUV deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject jsonobject = jsonElement.getAsJsonObject();
            float[] afloat = this.parseUV(jsonobject);
            int i = this.parseRotation(jsonobject);
            return new BlockFaceUV(afloat, i);
        }

        protected int parseRotation(JsonObject jsonObject) {
            int i = JsonUtils.getInt(jsonObject, "rotation", 0);

            if (i >= 0 && i % 90 == 0 && i / 90 <= 3) {
                return i;
            } else {
                throw new JsonParseException("Invalid rotation " + i + " found, only 0/90/180/270 allowed");
            }
        }

        private float[] parseUV(JsonObject jsonObject) {
            if (!jsonObject.has("uv")) {
                return null;
            } else {
                JsonArray jsonarray = JsonUtils.getJsonArray(jsonObject, "uv");

                if (jsonarray.size() != 4) {
                    throw new JsonParseException("Expected 4 uv values, found: " + jsonarray.size());
                } else {
                    float[] afloat = new float[4];

                    for (int i = 0; i < afloat.length; ++i) {
                        afloat[i] = JsonUtils.getFloat(jsonarray.get(i), "uv[" + i + "]");
                    }

                    return afloat;
                }
            }
        }
    }
}
