package net.minecraft.network.packet.api;

import net.minecraft.network.INetHandler;

import java.io.IOException;

public interface Packet<T extends INetHandler> {
    void read(PacketBuffer buf) throws IOException;

    void write(PacketBuffer buf) throws IOException;

    void handle(T handler);
}
