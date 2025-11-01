package net.minecraft.network.packet.impl.status.client;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.status.INetHandlerStatusServer;

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
