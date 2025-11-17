package net.minecraft.network.packet.impl.status.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.status.INetHandlerStatusClient;

import java.io.IOException;

public class S01PacketPong implements Packet<INetHandlerStatusClient> {
    private long time;

    public S01PacketPong() {
    }

    public S01PacketPong(long time) {
        this.time = time;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.time = buf.readLong();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeLong(this.time);
    }

    @Override
    public void handle(INetHandlerStatusClient handler) {
        handler.handlePong(this);
    }
}
