package net.minecraft.network.play.client;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.world.WorldServer;

import java.io.IOException;
import java.util.UUID;

public class C18PacketSpectate implements Packet<INetHandlerPlayServer> {
    private UUID id;

    public C18PacketSpectate() {
    }

    public C18PacketSpectate(UUID id) {
        this.id = id;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.id = buf.readUuid();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeUuid(this.id);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
        handler.handleSpectate(this);
    }

    public Entity getEntity(WorldServer worldIn) {
        return worldIn.getEntityFromUuid(this.id);
    }
}
