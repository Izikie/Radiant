package net.minecraft.network.packet.impl.play.client;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayServer;

import java.io.IOException;

public class C00PacketKeepAlive implements Packet<INetHandlerPlayServer> {
    private int key;

    public C00PacketKeepAlive() {
    }

    public C00PacketKeepAlive(int key) {
        this.key = key;
    }

    @Override
    public void handle(INetHandlerPlayServer handler) {
        handler.processKeepAlive(this);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.key = buf.readVarInt();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeVarInt(this.key);
    }

    public int getKey() {
        return this.key;
    }
}
