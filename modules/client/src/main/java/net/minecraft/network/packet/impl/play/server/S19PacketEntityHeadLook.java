package net.minecraft.network.packet.impl.play.server;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.world.World;

import java.io.IOException;

public class S19PacketEntityHeadLook implements Packet<INetHandlerPlayClient> {
    private int entityId;
    private byte yaw;

    public S19PacketEntityHeadLook() {
    }

    public S19PacketEntityHeadLook(Entity entityIn, byte p_i45214_2_) {
        this.entityId = entityIn.getEntityId();
        this.yaw = p_i45214_2_;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.entityId = buf.readVarInt();
        this.yaw = buf.readByte();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeVarInt(this.entityId);
        buf.writeByte(this.yaw);
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleEntityHeadLook(this);
    }

    public Entity getEntity(World worldIn) {
        return worldIn.getEntityByID(this.entityId);
    }

    public byte getYaw() {
        return this.yaw;
    }
}
