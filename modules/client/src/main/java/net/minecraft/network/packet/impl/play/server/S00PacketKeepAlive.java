package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;

public class S00PacketKeepAlive implements Packet<INetHandlerPlayClient> {
    private int id;

    public S00PacketKeepAlive() {
    }

    public S00PacketKeepAlive(int idIn) {
        this.id = idIn;
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleKeepAlive(this);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.id = buf.readVarInt();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeVarInt(this.id);
    }

    public int func_149134_c() {
        return this.id;
    }
}
