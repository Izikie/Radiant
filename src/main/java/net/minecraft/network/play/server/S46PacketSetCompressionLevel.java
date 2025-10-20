package net.minecraft.network.play.server;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;

import java.io.IOException;

public class S46PacketSetCompressionLevel implements Packet<INetHandlerPlayClient> {
    private int threshold;

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.threshold = buf.readVarIntFromBuffer();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarIntToBuffer(this.threshold);
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleSetCompressionLevel(this);
    }

    public int getThreshold() {
        return this.threshold;
    }
}
