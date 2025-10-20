package net.minecraft.network.login.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketBuffer;
import net.minecraft.network.login.INetHandlerLoginServer;

import java.io.IOException;

public class C00PacketLoginStart implements Packet<INetHandlerLoginServer> {
    private GameProfile profile;

    public C00PacketLoginStart() {
    }

    public C00PacketLoginStart(GameProfile profileIn) {
        this.profile = profileIn;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.profile = new GameProfile(null, buf.readStringFromBuffer(16));
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeString(this.profile.getName());
    }

    @Override
    public void processPacket(INetHandlerLoginServer handler) {
        handler.processLoginStart(this);
    }

    public GameProfile getProfile() {
        return this.profile;
    }
}
