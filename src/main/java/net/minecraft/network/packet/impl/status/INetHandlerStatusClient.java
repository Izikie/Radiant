package net.minecraft.network.packet.impl.status;

import net.minecraft.network.INetHandler;
import net.minecraft.network.packet.impl.status.server.S00PacketServerInfo;
import net.minecraft.network.packet.impl.status.server.S01PacketPong;

public interface INetHandlerStatusClient extends INetHandler {
    void handleServerInfo(S00PacketServerInfo packet);

    void handlePong(S01PacketPong packet);
}
