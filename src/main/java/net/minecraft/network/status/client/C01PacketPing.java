package net.minecraft.network.status.client;

import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.status.INetHandlerStatusServer;

import java.io.IOException;

public class C01PacketPing implements Packet<INetHandlerStatusServer> {
    private long time;

    public C01PacketPing() {}

    public C01PacketPing(long ping) {
        this.time = ping;
    }

    public void readPacketData(PacketBuffer buf) throws IOException {
        this.time = buf.readLong();
    }

    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeLong(this.time);
    }

    public void processPacket(INetHandlerStatusServer handler) {
        handler.processPing(this);
    }

    public long getTime() {
        return this.time;
    }
}
