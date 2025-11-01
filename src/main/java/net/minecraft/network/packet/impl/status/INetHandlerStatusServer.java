package net.minecraft.network.packet.impl.status;

import net.minecraft.network.INetHandler;
import net.minecraft.network.packet.impl.status.client.C00PacketServerQuery;
import net.minecraft.network.packet.impl.status.client.C01PacketPing;

public interface INetHandlerStatusServer extends INetHandler {
    void processPing(C01PacketPing packet);

    void processServerQuery(C00PacketServerQuery packet);
}
