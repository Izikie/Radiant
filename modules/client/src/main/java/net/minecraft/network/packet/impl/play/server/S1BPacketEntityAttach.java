package net.minecraft.network.packet.impl.play.server;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;

public class S1BPacketEntityAttach implements Packet<INetHandlerPlayClient> {
    private int leash;
    private int entityId;
    private int vehicleEntityId;

    public S1BPacketEntityAttach() {
    }

    public S1BPacketEntityAttach(int leashIn, Entity entityIn, Entity vehicle) {
        this.leash = leashIn;
        this.entityId = entityIn.getEntityId();
        this.vehicleEntityId = vehicle != null ? vehicle.getEntityId() : -1;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.entityId = buf.readInt();
        this.vehicleEntityId = buf.readInt();
        this.leash = buf.readUnsignedByte();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeInt(this.entityId);
        buf.writeInt(this.vehicleEntityId);
        buf.writeByte(this.leash);
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleEntityAttach(this);
    }

    public int getLeash() {
        return this.leash;
    }

    public int getEntityId() {
        return this.entityId;
    }

    public int getVehicleEntityId() {
        return this.vehicleEntityId;
    }
}
