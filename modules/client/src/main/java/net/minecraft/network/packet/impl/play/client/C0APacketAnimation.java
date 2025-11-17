package net.minecraft.network.packet.impl.play.client;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayServer;

import java.io.IOException;

public class C0APacketAnimation implements Packet<INetHandlerPlayServer> {
    @Override
    public void read(PacketBuffer buf) throws IOException {
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
    }

    @Override
    public void handle(INetHandlerPlayServer handler) {
        handler.handleAnimation(this);
    }
}
