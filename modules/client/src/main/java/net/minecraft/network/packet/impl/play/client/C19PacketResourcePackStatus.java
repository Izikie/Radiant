package net.minecraft.network.packet.impl.play.client;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayServer;

import java.io.IOException;

public class C19PacketResourcePackStatus implements Packet<INetHandlerPlayServer> {
    private String hash;
    private Action status;

    public C19PacketResourcePackStatus() {
    }

    public C19PacketResourcePackStatus(String hashIn, Action statusIn) {
        if (hashIn.length() > 40) {
            hashIn = hashIn.substring(0, 40);
        }

        this.hash = hashIn;
        this.status = statusIn;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.hash = buf.readStringFromBuffer(40);
        this.status = buf.readEnumValue(Action.class);
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeString(this.hash);
        buf.writeEnumValue(this.status);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
        handler.handleResourcePackStatus(this);
    }

    public enum Action {
        SUCCESSFULLY_LOADED,
        DECLINED,
        FAILED_DOWNLOAD,
        ACCEPTED
    }
}
