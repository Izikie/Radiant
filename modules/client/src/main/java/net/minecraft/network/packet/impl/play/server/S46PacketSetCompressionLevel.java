package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;

public class S46PacketSetCompressionLevel implements Packet<INetHandlerPlayClient> {
    private int threshold;

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.threshold = buf.readVarInt();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeVarInt(this.threshold);
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleSetCompressionLevel(this);
    }

    public int getThreshold() {
        return this.threshold;
    }
}
