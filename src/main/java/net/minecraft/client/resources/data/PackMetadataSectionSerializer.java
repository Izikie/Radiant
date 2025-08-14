package net.minecraft.client.resources.data;

import com.google.gson.*;
import net.minecraft.util.chat.IChatComponent;
import net.minecraft.util.json.JsonUtils;

import java.lang.reflect.Type;

public class PackMetadataSectionSerializer extends BaseMetadataSectionSerializer<PackMetadataSection> implements JsonSerializer<PackMetadataSection> {
    public PackMetadataSection deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        JsonObject jsonobject = jsonElement.getAsJsonObject();
        IChatComponent ichatcomponent = ctx.deserialize(jsonobject.get("description"), IChatComponent.class);

        if (ichatcomponent == null) {
            throw new JsonParseException("Invalid/missing description!");
        } else {
            int i = JsonUtils.getInt(jsonobject, "pack_format");
            return new PackMetadataSection(ichatcomponent, i);
        }
    }

    public JsonElement serialize(PackMetadataSection p_serialize_1_, Type type, JsonSerializationContext ctx) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("pack_format", p_serialize_1_.getPackFormat());
        jsonobject.add("description", ctx.serialize(p_serialize_1_.getPackDescription()));
        return jsonobject;
    }

    public String getSectionName() {
        return "pack";
    }
}
