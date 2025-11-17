package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;

public class S48PacketResourcePackSend implements Packet<INetHandlerPlayClient> {
    private String url;
    private String hash;

    public S48PacketResourcePackSend() {
    }

    public S48PacketResourcePackSend(String url, String hash) {
        this.url = url;
        this.hash = hash;

        if (hash.length() > 40) {
            throw new IllegalArgumentException("Hash is too long (max 40, was " + hash.length() + ")");
        }
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.url = buf.readString(32767);
        this.hash = buf.readString(40);
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeString(this.url);
        buf.writeString(this.hash);
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleResourcePack(this);
    }

    public String getURL() {
        return this.url;
    }

    public String getHash() {
        return this.hash;
    }
}
