package net.minecraft.network.packet.impl.login.server;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.login.INetHandlerLoginClient;

import java.io.IOException;
import java.util.UUID;

public class S02PacketLoginSuccess implements Packet<INetHandlerLoginClient> {
    private GameProfile profile;

    public S02PacketLoginSuccess() {
    }

    public S02PacketLoginSuccess(GameProfile profileIn) {
        this.profile = profileIn;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        String uuid = buf.readString(36);
        String name = buf.readString(16);
        this.profile = new GameProfile(UUID.fromString(uuid), name);
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        UUID uuid = this.profile.getId();
        buf.writeString(uuid == null ? "" : uuid.toString());
        buf.writeString(this.profile.getName());
    }

    @Override
    public void handle(INetHandlerLoginClient handler) {
        handler.handleLoginSuccess(this);
    }

    public GameProfile getProfile() {
        return this.profile;
    }
}
