package net.minecraft.network.play.client;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayServer;

import java.io.IOException;

public class C0APacketAnimation implements Packet<INetHandlerPlayServer> {
    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
        handler.handleAnimation(this);
    }
}
