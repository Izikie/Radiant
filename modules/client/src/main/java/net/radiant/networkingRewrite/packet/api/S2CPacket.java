package net.radiant.networkingRewrite.packet.api;

import net.radiant.networkingRewrite.PacketBuffer;

public interface S2CPacket extends Packet {

    void read(PacketBuffer buffer);

    void handle(PacketBuffer handler);

}
