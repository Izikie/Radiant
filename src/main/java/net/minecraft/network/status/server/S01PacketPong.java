package net.minecraft.network.status.server;

import java.io.IOException;

import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.status.INetHandlerStatusClient;

public class S01PacketPong implements Packet<INetHandlerStatusClient> {
    private long time;

    public S01PacketPong() {}

    public S01PacketPong(long time) {
        this.time = time;
    }

    public void readPacketData(PacketBuffer buf) throws IOException {
        this.time = buf.readLong();
    }

    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeLong(this.time);
    }

    public void processPacket(INetHandlerStatusClient handler) {
        handler.handlePong(this);
    }
}
