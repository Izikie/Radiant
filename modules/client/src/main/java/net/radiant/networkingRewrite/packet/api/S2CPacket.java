package net.radiant.networkingRewrite.packet.api;

public interface S2CPacket extends Packet {

    void read(PacketBuffer buffer);

    void handle(PacketBuffer handler);

}
