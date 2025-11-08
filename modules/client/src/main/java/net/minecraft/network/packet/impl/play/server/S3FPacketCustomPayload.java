package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;

public class S3FPacketCustomPayload implements Packet<INetHandlerPlayClient> {
    private String channel;
    private PacketBuffer data;

    public S3FPacketCustomPayload() {
    }

    public S3FPacketCustomPayload(String channel, PacketBuffer data) {
        this.channel = channel;
        this.data = data;

        if (data.writerIndex() > 1048576) {
            throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
        }
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.channel = buf.readStringFromBuffer(20);
        int size = buf.readableBytes();

        if (size >= 0 && size <= 1048576) {
            this.data = new PacketBuffer(buf.readBytes(size));
        } else {
            throw new IOException("Payload may not be larger than 1048576 bytes");
        }
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeString(this.channel);
        buf.writeBytes(this.data);
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleCustomPayload(this);
    }

    public String getChannel() {
        return this.channel;
    }

    public PacketBuffer getData() {
        return this.data;
    }
}
