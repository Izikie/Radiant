package net.minecraft.util.json;

import com.google.gson.JsonElement;

public interface IJsonSerializable {
    void fromJson(JsonElement json);

    JsonElement getSerializableElement();
}
