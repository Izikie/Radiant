package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.potion.PotionEffect;

import java.io.IOException;

public class S1EPacketRemoveEntityEffect implements Packet<INetHandlerPlayClient> {
    private int entityId;
    private int effectId;

    public S1EPacketRemoveEntityEffect() {
    }

    public S1EPacketRemoveEntityEffect(int entityIdIn, PotionEffect effect) {
        this.entityId = entityIdIn;
        this.effectId = effect.getPotionID();
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.entityId = buf.readVarIntFromBuffer();
        this.effectId = buf.readUnsignedByte();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarIntToBuffer(this.entityId);
        buf.writeByte(this.effectId);
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleRemoveEntityEffect(this);
    }

    public int getEntityId() {
        return this.entityId;
    }

    public int getEffectId() {
        return this.effectId;
    }
}
