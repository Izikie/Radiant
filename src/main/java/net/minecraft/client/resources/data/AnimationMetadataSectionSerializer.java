package net.minecraft.client.resources.data;

import com.google.gson.*;
import net.minecraft.util.JsonUtils;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AnimationMetadataSectionSerializer extends BaseMetadataSectionSerializer<AnimationMetadataSection> implements JsonSerializer<AnimationMetadataSection> {
    public AnimationMetadataSection deserialize(JsonElement p_deserialize_1_, Type p_deserialize_2_, JsonDeserializationContext p_deserialize_3_) throws JsonParseException {
        List<AnimationFrame> list = new ArrayList<>();
        JsonObject jsonobject = JsonUtils.getJsonObject(p_deserialize_1_, "metadata section");
        int i = JsonUtils.getInt(jsonobject, "frametime", 1);

        if (i != 1) {
            Validate.inclusiveBetween(1L, 2147483647L, i, "Invalid default frame time");
        }

        if (jsonobject.has("frames")) {
            try {
                JsonArray jsonarray = JsonUtils.getJsonArray(jsonobject, "frames");

                for (int j = 0; j < jsonarray.size(); ++j) {
                    JsonElement jsonelement = jsonarray.get(j);
                    AnimationFrame animationframe = this.parseAnimationFrame(j, jsonelement);

                    if (animationframe != null) {
                        list.add(animationframe);
                    }
                }
            } catch (ClassCastException exception) {
                throw new JsonParseException("Invalid animation->frames: expected array, was " + jsonobject.get("frames"), exception);
            }
        }

        int k = JsonUtils.getInt(jsonobject, "width", -1);
        int l = JsonUtils.getInt(jsonobject, "height", -1);

        if (k != -1) {
            Validate.inclusiveBetween(1L, 2147483647L, k, "Invalid width");
        }

        if (l != -1) {
            Validate.inclusiveBetween(1L, 2147483647L, l, "Invalid height");
        }

        boolean flag = JsonUtils.getBoolean(jsonobject, "interpolate", false);
        return new AnimationMetadataSection(list, k, l, i, flag);
    }

    private AnimationFrame parseAnimationFrame(int p_110492_1_, JsonElement p_110492_2_) {
        if (p_110492_2_.isJsonPrimitive()) {
            return new AnimationFrame(JsonUtils.getInt(p_110492_2_, "frames[" + p_110492_1_ + "]"));
        } else if (p_110492_2_.isJsonObject()) {
            JsonObject jsonobject = JsonUtils.getJsonObject(p_110492_2_, "frames[" + p_110492_1_ + "]");
            int i = JsonUtils.getInt(jsonobject, "time", -1);

            if (jsonobject.has("time")) {
                Validate.inclusiveBetween(1L, 2147483647L, i, "Invalid frame time");
            }

            int j = JsonUtils.getInt(jsonobject, "index");
            Validate.inclusiveBetween(0L, 2147483647L, j, "Invalid frame index");
            return new AnimationFrame(j, i);
        } else {
            return null;
        }
    }

    public JsonElement serialize(AnimationMetadataSection p_serialize_1_, Type type, JsonSerializationContext ctx) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("frametime", p_serialize_1_.getFrameTime());

        if (p_serialize_1_.getFrameWidth() != -1) {
            jsonobject.addProperty("width", p_serialize_1_.getFrameWidth());
        }

        if (p_serialize_1_.getFrameHeight() != -1) {
            jsonobject.addProperty("height", p_serialize_1_.getFrameHeight());
        }

        if (p_serialize_1_.getFrameCount() > 0) {
            JsonArray jsonarray = new JsonArray();

            for (int i = 0; i < p_serialize_1_.getFrameCount(); ++i) {
                if (p_serialize_1_.frameHasTime(i)) {
                    JsonObject jsonobject1 = new JsonObject();
                    jsonobject1.addProperty("index", p_serialize_1_.getFrameIndex(i));
                    jsonobject1.addProperty("time", p_serialize_1_.getFrameTimeSingle(i));
                    jsonarray.add(jsonobject1);
                } else {
                    jsonarray.add(new JsonPrimitive(p_serialize_1_.getFrameIndex(i)));
                }
            }

            jsonobject.add("frames", jsonarray);
        }

        return jsonobject;
    }

    public String getSectionName() {
        return "animation";
    }
}
