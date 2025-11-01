package net.minecraft.network.packet.impl.status.client;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.status.INetHandlerStatusServer;

import java.io.IOException;

public class C01PacketPing implements Packet<INetHandlerStatusServer> {
    private long time;

    public C01PacketPing() {
    }

    public C01PacketPing(long ping) {
        this.time = ping;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.time = buf.readLong();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeLong(this.time);
    }

    @Override
    public void processPacket(INetHandlerStatusServer handler) {
        handler.processPing(this);
    }

    public long getTime() {
        return this.time;
    }
}
