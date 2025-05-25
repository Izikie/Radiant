package net.minecraft.server.management;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;

import java.util.UUID;

public class UserListOpsEntry extends UserListEntry<GameProfile> {
    private final int permissionLevel;
    private final boolean bypassesPlayerLimit;

    public UserListOpsEntry(GameProfile player, int permissionLevelIn, boolean bypassesPlayerLimitIn) {
        super(player);
        this.permissionLevel = permissionLevelIn;
        this.bypassesPlayerLimit = bypassesPlayerLimitIn;
    }

    public UserListOpsEntry(JsonObject jsonObject) {
        super(constructProfile(jsonObject), jsonObject);
        this.permissionLevel = jsonObject.has("level") ? jsonObject.get("level").getAsInt() : 0;
        this.bypassesPlayerLimit = jsonObject.has("bypassesPlayerLimit") && jsonObject.get("bypassesPlayerLimit").getAsBoolean();
    }

    public int getPermissionLevel() {
        return this.permissionLevel;
    }

    public boolean bypassesPlayerLimit() {
        return this.bypassesPlayerLimit;
    }

    protected void onSerialization(JsonObject data) {
        if (this.getValue() != null) {
            data.addProperty("uuid", this.getValue().getId() == null ? "" : this.getValue().getId().toString());
            data.addProperty("name", this.getValue().getName());
            super.onSerialization(data);
            data.addProperty("level", this.permissionLevel);
            data.addProperty("bypassesPlayerLimit", this.bypassesPlayerLimit);
        }
    }

    private static GameProfile constructProfile(JsonObject jsonObject) {
        if (jsonObject.has("uuid") && jsonObject.has("name")) {
            String s = jsonObject.get("uuid").getAsString();
            UUID uuid;

            try {
                uuid = UUID.fromString(s);
            } catch (Throwable throwable) {
                return null;
            }

            return new GameProfile(uuid, jsonObject.get("name").getAsString());
        } else {
            return null;
        }
    }
}
