package net.minecraft.network.play.client;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayServer;

import java.io.IOException;

public class C00PacketKeepAlive implements Packet<INetHandlerPlayServer> {
    private int key;

    public C00PacketKeepAlive() {
    }

    public C00PacketKeepAlive(int key) {
        this.key = key;
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
        handler.processKeepAlive(this);
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.key = buf.readVarIntFromBuffer();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarIntToBuffer(this.key);
    }

    public int getKey() {
        return this.key;
    }
}
