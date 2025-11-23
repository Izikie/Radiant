package net.minecraft.network;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.util.chat.IChatComponent;
import net.minecraft.util.json.JsonUtils;

import java.lang.reflect.Type;
import java.util.UUID;

public class ServerStatusResponse {
    private IChatComponent motd;
    private PlayerCountData playerCount;
    private MinecraftProtocolVersionIdentifier protocolVersion;
    private String favicon;

    public IChatComponent getMOTD() {
        return motd;
    }

    public void setServerDescription(IChatComponent motd) {
        this.motd = motd;
    }

    public PlayerCountData getPlayerCountData() {
        return playerCount;
    }

    public void setPlayerCountData(PlayerCountData countData) {
        this.playerCount = countData;
    }

    public MinecraftProtocolVersionIdentifier getProtocolVersionInfo() {
        return protocolVersion;
    }

    public void setProtocolVersionInfo(MinecraftProtocolVersionIdentifier protocolVersionData) {
        this.protocolVersion = protocolVersionData;
    }

    public void setFavicon(String faviconBlob) {
        this.favicon = faviconBlob;
    }

    public String getFavicon() {
        return favicon;
    }

    public record MinecraftProtocolVersionIdentifier(String name, int protocol) {
        public static class Serializer implements JsonDeserializer<MinecraftProtocolVersionIdentifier>, JsonSerializer<MinecraftProtocolVersionIdentifier> {
            @Override
            public MinecraftProtocolVersionIdentifier deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
                JsonObject jsonObject = JsonUtils.getJsonObject(jsonElement, "version");
                return new MinecraftProtocolVersionIdentifier(JsonUtils.getString(jsonObject, "name"), JsonUtils.getInt(jsonObject, "protocol"));
            }

            @Override
            public JsonElement serialize(MinecraftProtocolVersionIdentifier versionIdentifier, Type type, JsonSerializationContext ctx) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("name", versionIdentifier.name());
                jsonObject.addProperty("protocol", versionIdentifier.protocol());
                return jsonObject;
            }
        }
    }

    public static class PlayerCountData {
        private final int maxPlayers;
        private final int onlinePlayers;
        private GameProfile[] players = new GameProfile[0];

        public PlayerCountData(int maxPlayers, int onlinePlayers) {
            this.maxPlayers = maxPlayers;
            this.onlinePlayers = onlinePlayers;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public int getOnlinePlayers() {
            return onlinePlayers;
        }

        public GameProfile[] getPlayers() {
            return players;
        }

        public void setPlayers(GameProfile[] playersIn) {
            this.players = playersIn;
        }

        public static class Serializer implements JsonDeserializer<PlayerCountData>, JsonSerializer<PlayerCountData> {
            @Override
            public PlayerCountData deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
                JsonObject jsonObject = JsonUtils.getJsonObject(jsonElement, "players");
                PlayerCountData playerCountData = new PlayerCountData(JsonUtils.getInt(jsonObject, "max"), JsonUtils.getInt(jsonObject, "online"));

                if (JsonUtils.isJsonArray(jsonObject, "sample")) {
                    JsonArray jsonArray = JsonUtils.getJsonArray(jsonObject, "sample");

                    if (!jsonArray.isEmpty()) {
                        GameProfile[] profiles = new GameProfile[jsonArray.size()];

                        for (int i = 0; i < profiles.length; ++i) {
                            JsonObject playerObject = JsonUtils.getJsonObject(jsonArray.get(i), "player[" + i + "]");
                            profiles[i] = new GameProfile(
                                    UUID.fromString(JsonUtils.getString(playerObject, "id")),
                                    JsonUtils.getString(playerObject, "name")
                            );
                        }

                        playerCountData.setPlayers(profiles);
                    }
                }

                return playerCountData;
            }

            @Override
            public JsonElement serialize(PlayerCountData data, Type type, JsonSerializationContext ctx) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("max", data.getMaxPlayers());
                jsonObject.addProperty("online", data.getOnlinePlayers());

                if (data.getPlayers() != null && data.getPlayers().length > 0) {
                    JsonArray jsonArray = new JsonArray();

                    for (GameProfile player : data.getPlayers()) {
                        JsonObject playerObject = new JsonObject();
                        UUID uuid = player.getId();
                        playerObject.addProperty("id", uuid == null ? "" : uuid.toString());
                        playerObject.addProperty("name", player.getName());
                        jsonArray.add(playerObject);
                    }

                    jsonObject.add("sample", jsonArray);
                }

                return jsonObject;
            }
        }
    }

    public static class Serializer implements JsonDeserializer<ServerStatusResponse>, JsonSerializer<ServerStatusResponse> {
        @Override
        public ServerStatusResponse deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject jsonObject = JsonUtils.getJsonObject(jsonElement, "status");
            ServerStatusResponse response = new ServerStatusResponse();

            if (jsonObject.has("description")) {
                response.setServerDescription(ctx.deserialize(jsonObject.get("description"), IChatComponent.class));
            }

            if (jsonObject.has("players")) {
                response.setPlayerCountData(ctx.deserialize(jsonObject.get("players"), PlayerCountData.class));
            }

            if (jsonObject.has("version")) {
                response.setProtocolVersionInfo(ctx.deserialize(jsonObject.get("version"), MinecraftProtocolVersionIdentifier.class));
            }

            if (jsonObject.has("favicon")) {
                response.setFavicon(JsonUtils.getString(jsonObject, "favicon"));
            }

            return response;
        }

        @Override
        public JsonElement serialize(ServerStatusResponse response, Type type, JsonSerializationContext ctx) {
            JsonObject jsonObject = new JsonObject();

            if (response.getMOTD() != null) {
                jsonObject.add("description", ctx.serialize(response.getMOTD()));
            }

            if (response.getPlayerCountData() != null) {
                jsonObject.add("players", ctx.serialize(response.getPlayerCountData()));
            }

            if (response.getProtocolVersionInfo() != null) {
                jsonObject.add("version", ctx.serialize(response.getProtocolVersionInfo()));
            }

            if (response.getFavicon() != null) {
                jsonObject.addProperty("favicon", response.getFavicon());
            }

            return jsonObject;
        }
    }
}
