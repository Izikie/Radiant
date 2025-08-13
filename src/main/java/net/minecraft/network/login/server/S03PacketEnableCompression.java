package net.minecraft.network.login.server;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketBuffer;
import net.minecraft.network.login.INetHandlerLoginClient;

import java.io.IOException;

public class S03PacketEnableCompression implements Packet<INetHandlerLoginClient> {
    private int compressionThreshold;

    public S03PacketEnableCompression() {
    }

    public S03PacketEnableCompression(int compressionTresholdIn) {
        this.compressionThreshold = compressionTresholdIn;
    }

    public void readPacketData(PacketBuffer buf) throws IOException {
        this.compressionThreshold = buf.readVarIntFromBuffer();
    }

    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarIntToBuffer(this.compressionThreshold);
    }

    public void processPacket(INetHandlerLoginClient handler) {
        handler.handleEnableCompression(this);
    }

    public int getCompressionThreshold() {
        return this.compressionThreshold;
    }
}
