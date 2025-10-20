package net.minecraft.network.status.client;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketBuffer;
import net.minecraft.network.status.INetHandlerStatusServer;

import java.io.IOException;

public class C00PacketServerQuery implements Packet<INetHandlerStatusServer> {
    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
    }

    @Override
    public void processPacket(INetHandlerStatusServer handler) {
        handler.processServerQuery(this);
    }
}
