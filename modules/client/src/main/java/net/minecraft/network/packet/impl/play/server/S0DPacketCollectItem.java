package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;

public class S0DPacketCollectItem implements Packet<INetHandlerPlayClient> {
    private int collectedItemEntityId;
    private int entityId;

    public S0DPacketCollectItem() {
    }

    public S0DPacketCollectItem(int collectedItemEntityIdIn, int entityIdIn) {
        this.collectedItemEntityId = collectedItemEntityIdIn;
        this.entityId = entityIdIn;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.collectedItemEntityId = buf.readVarInt();
        this.entityId = buf.readVarInt();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeVarInt(this.collectedItemEntityId);
        buf.writeVarInt(this.entityId);
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleCollectItem(this);
    }

    public int getCollectedItemEntityID() {
        return this.collectedItemEntityId;
    }

    public int getEntityID() {
        return this.entityId;
    }
}
