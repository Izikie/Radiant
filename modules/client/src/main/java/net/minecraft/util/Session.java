package net.minecraft.util;

import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record Session(String username, String playerID, String token, Type sessionType) {
    public Session(String username, String playerID, String token, String session) {
        this(username, playerID, token, Type.setSessionType(session));
    }

    public String getSessionID() {
        return "token:" + this.token + ":" + this.playerID;
    }

    public GameProfile getProfile() {
        try {
            UUID uuid = UUIDTypeAdapter.fromString(this.playerID());
            return new GameProfile(uuid, this.username());
        } catch (IllegalArgumentException exception) {
            return new GameProfile(null, this.username());
        }
    }

    public enum Type {
        LEGACY("legacy"),
        MOJANG("mojang");

        private static final Map<String, Type> SESSION_TYPES = new HashMap<>();
        private final String sessionType;

        Type(String sessionTypeIn) {
            this.sessionType = sessionTypeIn;
        }

        public static Type setSessionType(String sessionTypeIn) {
            return SESSION_TYPES.get(sessionTypeIn.toLowerCase());
        }

        static {
            for (Type type : values()) {
                SESSION_TYPES.put(type.sessionType, type);
            }
        }
    }
}
