package net.minecraft.network.packet.impl.play.client;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayServer;

import java.io.IOException;

public class C17PacketCustomPayload implements Packet<INetHandlerPlayServer> {
    private String channel;
    private PacketBuffer data;

    public C17PacketCustomPayload() {
    }

    public C17PacketCustomPayload(String channel, PacketBuffer data) {
        this.channel = channel;
        this.data = data;

        if (data.writerIndex() > 32767) {
            throw new IllegalArgumentException("Payload may not be larger than 32767 bytes");
        }
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.channel = buf.readString(20);
        int size = buf.readableBytes();

        if (size >= 0 && size <= 32767) {
            this.data = new PacketBuffer(buf.readBytes(size));
        } else {
            throw new IOException("Payload may not be larger than 32767 bytes");
        }
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeString(this.channel);
        buf.writeBytes(this.data);
    }

    @Override
    public void handle(INetHandlerPlayServer handler) {
        handler.processCustomPayload(this);
    }

    public String getChannel() {
        return this.channel;
    }

    public PacketBuffer getData() {
        return this.data;
    }
}
