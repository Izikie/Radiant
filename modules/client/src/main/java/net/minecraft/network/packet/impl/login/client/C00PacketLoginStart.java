package net.minecraft.network.packet.impl.login.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.login.INetHandlerLoginServer;

import java.io.IOException;

public class C00PacketLoginStart implements Packet<INetHandlerLoginServer> {
    private GameProfile profile;

    public C00PacketLoginStart() {
    }

    public C00PacketLoginStart(GameProfile profileIn) {
        this.profile = profileIn;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.profile = new GameProfile(null, buf.readString(16));
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeString(this.profile.getName());
    }

    @Override
    public void handle(INetHandlerLoginServer handler) {
        handler.processLoginStart(this);
    }

    public GameProfile getProfile() {
        return this.profile;
    }
}
