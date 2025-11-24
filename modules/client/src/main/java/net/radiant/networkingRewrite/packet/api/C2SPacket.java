package net.radiant.networkingRewrite.packet.api;

public interface C2SPacket extends Packet {

    void write(PacketBuffer buffer);

}
