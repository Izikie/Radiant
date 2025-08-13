package net.minecraft.client.resources.data;

import com.google.gson.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.util.json.JsonUtils;

import java.lang.reflect.Type;

public class TextureMetadataSectionSerializer extends BaseMetadataSectionSerializer<TextureMetadataSection> {
    public TextureMetadataSection deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        JsonObject jsonobject = jsonElement.getAsJsonObject();
        boolean flag = JsonUtils.getBoolean(jsonobject, "blur", false);
        boolean flag1 = JsonUtils.getBoolean(jsonobject, "clamp", false);
        IntList list = new IntArrayList();

        if (jsonobject.has("mipmaps")) {
            try {
                JsonArray jsonarray = jsonobject.getAsJsonArray("mipmaps");

                for (int i = 0; i < jsonarray.size(); ++i) {
                    JsonElement jsonelement = jsonarray.get(i);

                    if (jsonelement.isJsonPrimitive()) {
                        try {
                            list.add(jsonelement.getAsInt());
                        } catch (NumberFormatException exception) {
                            throw new JsonParseException("Invalid texture->mipmap->" + i + ": expected number, was " + jsonelement, exception);
                        }
                    } else if (jsonelement.isJsonObject()) {
                        throw new JsonParseException("Invalid texture->mipmap->" + i + ": expected number, was " + jsonelement);
                    }
                }
            } catch (ClassCastException exception) {
                throw new JsonParseException("Invalid texture->mipmaps: expected array, was " + jsonobject.get("mipmaps"), exception);
            }
        }

        return new TextureMetadataSection(flag, flag1, list);
    }

    public String getSectionName() {
        return "texture";
    }
}
