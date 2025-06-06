package net.minecraft.util;

import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Session {
    private final String username;
    private final String playerID;
    private final String token;
    private final Type sessionType;

    public Session(String usernameIn, String playerIDIn, String tokenIn, String sessionTypeIn) {
        this.username = usernameIn;
        this.playerID = playerIDIn;
        this.token = tokenIn;
        this.sessionType = Type.setSessionType(sessionTypeIn);
    }

    public String getSessionID() {
        return "token:" + this.token + ":" + this.playerID;
    }

    public String getPlayerID() {
        return this.playerID;
    }

    public String getUsername() {
        return this.username;
    }

    public String getToken() {
        return this.token;
    }

    public GameProfile getProfile() {
        try {
            UUID uuid = UUIDTypeAdapter.fromString(this.getPlayerID());
            return new GameProfile(uuid, this.getUsername());
        } catch (IllegalArgumentException exception) {
            return new GameProfile(null, this.getUsername());
        }
    }

    public Type getSessionType() {
        return this.sessionType;
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
