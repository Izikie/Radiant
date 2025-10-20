package net.minecraft.util.chat;

import com.google.gson.*;
import net.minecraft.util.json.EnumTypeAdapterFactory;
import net.minecraft.util.json.JsonUtils;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map.Entry;

public interface IChatComponent extends Iterable<IChatComponent> {
    IChatComponent setChatStyle(ChatStyle style);

    ChatStyle getChatStyle();

    IChatComponent appendText(String text);

    IChatComponent appendSibling(IChatComponent component);

    String getUnformattedTextForChat();

    String getUnformattedText();

    String getFormattedText();

    List<IChatComponent> getSiblings();

    IChatComponent createCopy();

    class Serializer implements JsonDeserializer<IChatComponent>, JsonSerializer<IChatComponent> {
        private static final Gson GSON = new GsonBuilder()
                    .registerTypeHierarchyAdapter(IChatComponent.class, new Serializer())
                    .registerTypeHierarchyAdapter(ChatStyle.class, new ChatStyle.Serializer())
                    .registerTypeAdapterFactory(new EnumTypeAdapterFactory())
                    .create();

        @Override
        public IChatComponent deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            if (jsonElement.isJsonPrimitive()) {
                return new ChatComponentText(jsonElement.getAsString());
            } else if (!jsonElement.isJsonObject()) {
                if (jsonElement.isJsonArray()) {
                    JsonArray jsonarray1 = jsonElement.getAsJsonArray();
                    IChatComponent ichatcomponent1 = null;

                    for (JsonElement jsonelement : jsonarray1) {
                        IChatComponent ichatcomponent2 = this.deserialize(jsonelement, jsonelement.getClass(), ctx);

                        if (ichatcomponent1 == null) {
                            ichatcomponent1 = ichatcomponent2;
                        } else {
                            ichatcomponent1.appendSibling(ichatcomponent2);
                        }
                    }

                    return ichatcomponent1;
                } else {
                    throw new JsonParseException("Don't know how to turn " + jsonElement + " into a Component");
                }
            } else {
                JsonObject jsonobject = jsonElement.getAsJsonObject();
                IChatComponent ichatcomponent;

                if (jsonobject.has("text")) {
                    ichatcomponent = new ChatComponentText(jsonobject.get("text").getAsString());
                } else if (jsonobject.has("translate")) {
                    String s = jsonobject.get("translate").getAsString();

                    if (jsonobject.has("with")) {
                        JsonArray jsonarray = jsonobject.getAsJsonArray("with");
                        Object[] aobject = new Object[jsonarray.size()];

                        for (int i = 0; i < aobject.length; ++i) {
                            aobject[i] = this.deserialize(jsonarray.get(i), type, ctx);

                            if (aobject[i] instanceof ChatComponentText chatcomponenttext) {

                                if (chatcomponenttext.getChatStyle().isEmpty() && chatcomponenttext.getSiblings().isEmpty()) {
                                    aobject[i] = chatcomponenttext.getChatComponentText_TextValue();
                                }
                            }
                        }

                        ichatcomponent = new ChatComponentTranslation(s, aobject);
                    } else {
                        ichatcomponent = new ChatComponentTranslation(s);
                    }
                } else if (jsonobject.has("score")) {
                    JsonObject jsonobject1 = jsonobject.getAsJsonObject("score");

                    if (!jsonobject1.has("name") || !jsonobject1.has("objective")) {
                        throw new JsonParseException("A score component needs a least a name and an objective");
                    }

                    ichatcomponent = new ChatComponentScore(JsonUtils.getString(jsonobject1, "name"), JsonUtils.getString(jsonobject1, "objective"));

                    if (jsonobject1.has("value")) {
                        ((ChatComponentScore) ichatcomponent).setValue(JsonUtils.getString(jsonobject1, "value"));
                    }
                } else {
                    if (!jsonobject.has("selector")) {
                        throw new JsonParseException("Don't know how to turn " + jsonElement + " into a Component");
                    }

                    ichatcomponent = new ChatComponentSelector(JsonUtils.getString(jsonobject, "selector"));
                }

                if (jsonobject.has("extra")) {
                    JsonArray jsonarray2 = jsonobject.getAsJsonArray("extra");

                    if (jsonarray2.size() <= 0) {
                        throw new JsonParseException("Unexpected empty array of components");
                    }

                    for (int j = 0; j < jsonarray2.size(); ++j) {
                        ichatcomponent.appendSibling(this.deserialize(jsonarray2.get(j), type, ctx));
                    }
                }

                ichatcomponent.setChatStyle(ctx.deserialize(jsonElement, ChatStyle.class));
                return ichatcomponent;
            }
        }

        private void serializeChatStyle(ChatStyle style, JsonObject object, JsonSerializationContext ctx) {
            JsonElement jsonelement = ctx.serialize(style);

            if (jsonelement.isJsonObject()) {
                JsonObject jsonobject = (JsonObject) jsonelement;

                for (Entry<String, JsonElement> entry : jsonobject.entrySet()) {
                    object.add(entry.getKey(), entry.getValue());
                }
            }
        }

        @Override
        public JsonElement serialize(IChatComponent p_serialize_1_, Type type, JsonSerializationContext ctx) {
            if (p_serialize_1_ instanceof ChatComponentText chatComponents && p_serialize_1_.getChatStyle().isEmpty() && p_serialize_1_.getSiblings().isEmpty()) {
                return new JsonPrimitive(chatComponents.getChatComponentText_TextValue());
            } else {
                JsonObject jsonobject = new JsonObject();

                if (!p_serialize_1_.getChatStyle().isEmpty()) {
                    this.serializeChatStyle(p_serialize_1_.getChatStyle(), jsonobject, ctx);
                }

                if (!p_serialize_1_.getSiblings().isEmpty()) {
                    JsonArray jsonarray = new JsonArray();

                    for (IChatComponent ichatcomponent : p_serialize_1_.getSiblings()) {
                        jsonarray.add(this.serialize(ichatcomponent, ichatcomponent.getClass(), ctx));
                    }

                    jsonobject.add("extra", jsonarray);
                }

                switch (p_serialize_1_) {
                    case ChatComponentText iChatComponents ->
                            jsonobject.addProperty("text", iChatComponents.getChatComponentText_TextValue());
                    case ChatComponentTranslation chatcomponenttranslation -> {
                        jsonobject.addProperty("translate", chatcomponenttranslation.getKey());

                        if (chatcomponenttranslation.getFormatArgs() != null && chatcomponenttranslation.getFormatArgs().length > 0) {
                            JsonArray jsonarray1 = new JsonArray();

                            for (Object object : chatcomponenttranslation.getFormatArgs()) {
                                if (object instanceof IChatComponent chatComponent) {
                                    jsonarray1.add(this.serialize(chatComponent, object.getClass(), ctx));
                                } else {
                                    jsonarray1.add(new JsonPrimitive(String.valueOf(object)));
                                }
                            }

                            jsonobject.add("with", jsonarray1);
                        }
                    }
                    case ChatComponentScore chatcomponentscore -> {
                        JsonObject jsonobject1 = new JsonObject();
                        jsonobject1.addProperty("name", chatcomponentscore.getName());
                        jsonobject1.addProperty("objective", chatcomponentscore.getObjective());
                        jsonobject1.addProperty("value", chatcomponentscore.getUnformattedTextForChat());
                        jsonobject.add("score", jsonobject1);
                    }
                    default -> {
                        if (!(p_serialize_1_ instanceof ChatComponentSelector chatcomponentselector)) {
                            throw new IllegalArgumentException("Don't know how to serialize " + p_serialize_1_ + " as a Component");
                        }

                        jsonobject.addProperty("selector", chatcomponentselector.getSelector());
                    }
                }

                return jsonobject;
            }
        }

        public static String componentToJson(IChatComponent component) {
            return GSON.toJson(component);
        }

        public static IChatComponent jsonToComponent(String json) {
            return GSON.fromJson(json, IChatComponent.class);
        }
    }
}
