package net.minecraft.network.packet.impl.play.server;

import com.google.common.base.MoreObjects;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.util.chat.IChatComponent;
import net.minecraft.world.WorldSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class S38PacketPlayerListItem implements Packet<INetHandlerPlayClient> {
    private Action action;
    private final List<AddPlayerData> players = new ArrayList<>();

    public S38PacketPlayerListItem() {
    }

    public S38PacketPlayerListItem(Action actionIn, EntityPlayerMP... players) {
        this.action = actionIn;

        for (EntityPlayerMP entityplayermp : players) {
            this.players.add(new AddPlayerData(entityplayermp.getGameProfile(), entityplayermp.ping, entityplayermp.theItemInWorldManager.getGameType(), entityplayermp.getTabListDisplayName()));
        }
    }

    public S38PacketPlayerListItem(Action actionIn, Iterable<EntityPlayerMP> players) {
        this.action = actionIn;

        for (EntityPlayerMP entityplayermp : players) {
            this.players.add(new AddPlayerData(entityplayermp.getGameProfile(), entityplayermp.ping, entityplayermp.theItemInWorldManager.getGameType(), entityplayermp.getTabListDisplayName()));
        }
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.action = buf.readEnum(Action.class);
        int i = buf.readVarInt();

        for (int j = 0; j < i; ++j) {
            GameProfile gameprofile = null;
            int k = 0;
            WorldSettings.GameType worldsettings$gametype = null;
            IChatComponent ichatcomponent = null;

            switch (this.action) {
                case ADD_PLAYER:
                    gameprofile = new GameProfile(buf.readUuid(), buf.readString(16));
                    int l = buf.readVarInt();
                    int i1 = 0;

                    for (; i1 < l; ++i1) {
                        String s = buf.readString(32767);
                        String s1 = buf.readString(32767);

                        if (buf.readBoolean()) {
                            gameprofile.getProperties().put(s, new Property(s, s1, buf.readString(32767)));
                        } else {
                            gameprofile.getProperties().put(s, new Property(s, s1));
                        }
                    }

                    worldsettings$gametype = WorldSettings.GameType.getByID(buf.readVarInt());
                    k = buf.readVarInt();

                    if (buf.readBoolean()) {
                        ichatcomponent = buf.readChatComponent();
                    }

                    break;

                case UPDATE_GAME_MODE:
                    gameprofile = new GameProfile(buf.readUuid(), null);
                    worldsettings$gametype = WorldSettings.GameType.getByID(buf.readVarInt());
                    break;

                case UPDATE_LATENCY:
                    gameprofile = new GameProfile(buf.readUuid(), null);
                    k = buf.readVarInt();
                    break;

                case UPDATE_DISPLAY_NAME:
                    gameprofile = new GameProfile(buf.readUuid(), null);

                    if (buf.readBoolean()) {
                        ichatcomponent = buf.readChatComponent();
                    }

                    break;

                case REMOVE_PLAYER:
                    gameprofile = new GameProfile(buf.readUuid(), null);
            }

            this.players.add(new AddPlayerData(gameprofile, k, worldsettings$gametype, ichatcomponent));
        }
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeEnum(this.action);
        buf.writeVarInt(this.players.size());

        for (AddPlayerData s38packetplayerlistitem$addplayerdata : this.players) {
            switch (this.action) {
                case ADD_PLAYER:
                    buf.writeUuid(s38packetplayerlistitem$addplayerdata.getProfile().getId());
                    buf.writeString(s38packetplayerlistitem$addplayerdata.getProfile().getName());
                    buf.writeVarInt(s38packetplayerlistitem$addplayerdata.getProfile().getProperties().size());

                    for (Property property : s38packetplayerlistitem$addplayerdata.getProfile().getProperties().values()) {
                        buf.writeString(property.getName());
                        buf.writeString(property.getValue());

                        if (property.hasSignature()) {
                            buf.writeBoolean(true);
                            buf.writeString(property.getSignature());
                        } else {
                            buf.writeBoolean(false);
                        }
                    }

                    buf.writeVarInt(s38packetplayerlistitem$addplayerdata.getGameMode().getID());
                    buf.writeVarInt(s38packetplayerlistitem$addplayerdata.getPing());

                    if (s38packetplayerlistitem$addplayerdata.getDisplayName() == null) {
                        buf.writeBoolean(false);
                    } else {
                        buf.writeBoolean(true);
                        buf.writeChatComponent(s38packetplayerlistitem$addplayerdata.getDisplayName());
                    }

                    break;

                case UPDATE_GAME_MODE:
                    buf.writeUuid(s38packetplayerlistitem$addplayerdata.getProfile().getId());
                    buf.writeVarInt(s38packetplayerlistitem$addplayerdata.getGameMode().getID());
                    break;

                case UPDATE_LATENCY:
                    buf.writeUuid(s38packetplayerlistitem$addplayerdata.getProfile().getId());
                    buf.writeVarInt(s38packetplayerlistitem$addplayerdata.getPing());
                    break;

                case UPDATE_DISPLAY_NAME:
                    buf.writeUuid(s38packetplayerlistitem$addplayerdata.getProfile().getId());

                    if (s38packetplayerlistitem$addplayerdata.getDisplayName() == null) {
                        buf.writeBoolean(false);
                    } else {
                        buf.writeBoolean(true);
                        buf.writeChatComponent(s38packetplayerlistitem$addplayerdata.getDisplayName());
                    }

                    break;

                case REMOVE_PLAYER:
                    buf.writeUuid(s38packetplayerlistitem$addplayerdata.getProfile().getId());
            }
        }
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handlePlayerListItem(this);
    }

    public List<AddPlayerData> getEntries() {
        return this.players;
    }

    public Action getAction() {
        return this.action;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this).add("action", this.action).add("entries", this.players).toString();
    }

    public enum Action {
        ADD_PLAYER,
        UPDATE_GAME_MODE,
        UPDATE_LATENCY,
        UPDATE_DISPLAY_NAME,
        REMOVE_PLAYER
    }

    public static class AddPlayerData {
        private final int ping;
        private final WorldSettings.GameType gamemode;
        private final GameProfile profile;
        private final IChatComponent displayName;

        public AddPlayerData(GameProfile profile, int pingIn, WorldSettings.GameType gamemodeIn, IChatComponent displayNameIn) {
            this.profile = profile;
            this.ping = pingIn;
            this.gamemode = gamemodeIn;
            this.displayName = displayNameIn;
        }

        public GameProfile getProfile() {
            return this.profile;
        }

        public int getPing() {
            return this.ping;
        }

        public WorldSettings.GameType getGameMode() {
            return this.gamemode;
        }

        public IChatComponent getDisplayName() {
            return this.displayName;
        }

        public String toString() {
            return MoreObjects.toStringHelper(this).add("latency", this.ping).add("gameMode", this.gamemode).add("profile", this.profile).add("displayName", this.displayName == null ? null : IChatComponent.Serializer.componentToJson(this.displayName)).toString();
        }
    }
}
