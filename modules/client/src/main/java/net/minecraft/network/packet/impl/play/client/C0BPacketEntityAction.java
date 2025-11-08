package net.minecraft.network.packet.impl.play.client;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayServer;

import java.io.IOException;

public class C0BPacketEntityAction implements Packet<INetHandlerPlayServer> {
    private int entityID;
    private Action action;
    private int auxData;

    public C0BPacketEntityAction() {
    }

    public C0BPacketEntityAction(Entity entity, Action action) {
        this(entity, action, 0);
    }

    public C0BPacketEntityAction(Entity entity, Action action, int auxData) {
        this.entityID = entity.getEntityId();
        this.action = action;
        this.auxData = auxData;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.entityID = buf.readVarIntFromBuffer();
        this.action = buf.readEnumValue(Action.class);
        this.auxData = buf.readVarIntFromBuffer();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarIntToBuffer(this.entityID);
        buf.writeEnumValue(this.action);
        buf.writeVarIntToBuffer(this.auxData);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
        handler.processEntityAction(this);
    }

    public Action getAction() {
        return this.action;
    }

    public int getAuxData() {
        return this.auxData;
    }

    public enum Action {
        START_SNEAKING,
        STOP_SNEAKING,
        STOP_SLEEPING,
        START_SPRINTING,
        STOP_SPRINTING,
        RIDING_JUMP,
        OPEN_INVENTORY
    }
}
