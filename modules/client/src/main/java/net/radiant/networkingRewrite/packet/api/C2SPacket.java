package net.radiant.networkingRewrite.packet.api;

import net.radiant.networkingRewrite.PacketBuffer;

public interface C2SPacket extends Packet {

    void write(PacketBuffer buffer);

}
