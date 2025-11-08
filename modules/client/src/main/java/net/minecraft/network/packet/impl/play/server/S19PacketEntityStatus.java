package net.minecraft.network.packet.impl.play.server;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.world.World;

import java.io.IOException;

public class S19PacketEntityStatus implements Packet<INetHandlerPlayClient> {
    private int entityId;
    private byte logicOpcode;

    public S19PacketEntityStatus() {
    }

    public S19PacketEntityStatus(Entity entityIn, byte opCodeIn) {
        this.entityId = entityIn.getEntityId();
        this.logicOpcode = opCodeIn;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.entityId = buf.readInt();
        this.logicOpcode = buf.readByte();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeInt(this.entityId);
        buf.writeByte(this.logicOpcode);
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleEntityStatus(this);
    }

    public Entity getEntity(World worldIn) {
        return worldIn.getEntityByID(this.entityId);
    }

    public byte getOpCode() {
        return this.logicOpcode;
    }
}
