package net.minecraft.network.packet.impl.play.server;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;

public class S0BPacketAnimation implements Packet<INetHandlerPlayClient> {
    private int entityId;
    private int type;

    public S0BPacketAnimation() {
    }

    public S0BPacketAnimation(Entity ent, int animationType) {
        this.entityId = ent.getEntityId();
        this.type = animationType;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.entityId = buf.readVarIntFromBuffer();
        this.type = buf.readUnsignedByte();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarIntToBuffer(this.entityId);
        buf.writeByte(this.type);
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleAnimation(this);
    }

    public int getEntityID() {
        return this.entityId;
    }

    public int getAnimationType() {
        return this.type;
    }
}
