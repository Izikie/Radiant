package net.minecraft.network.play.server;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.world.World;

import java.io.IOException;

public class S43PacketCamera implements Packet<INetHandlerPlayClient> {
    public int entityId;

    public S43PacketCamera() {
    }

    public S43PacketCamera(Entity entityIn) {
        this.entityId = entityIn.getEntityId();
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.entityId = buf.readVarIntFromBuffer();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarIntToBuffer(this.entityId);
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleCamera(this);
    }

    public Entity getEntity(World worldIn) {
        return worldIn.getEntityByID(this.entityId);
    }
}
