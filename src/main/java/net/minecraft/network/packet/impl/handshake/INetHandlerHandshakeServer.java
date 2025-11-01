package net.minecraft.network.packet.impl.handshake;

import net.minecraft.network.INetHandler;
import net.minecraft.network.packet.impl.handshake.client.C00Handshake;

public interface INetHandlerHandshakeServer extends INetHandler {
    void processHandshake(C00Handshake packet);
}
