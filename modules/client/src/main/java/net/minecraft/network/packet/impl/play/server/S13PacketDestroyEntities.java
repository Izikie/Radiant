package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;

public class S13PacketDestroyEntities implements Packet<INetHandlerPlayClient> {
    private int[] entityIDs;

    public S13PacketDestroyEntities() {
    }

    public S13PacketDestroyEntities(int... entityIDsIn) {
        this.entityIDs = entityIDsIn;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.entityIDs = new int[buf.readVarInt()];

        for (int i = 0; i < this.entityIDs.length; ++i) {
            this.entityIDs[i] = buf.readVarInt();
        }
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeVarInt(this.entityIDs.length);

        for (int entityID : this.entityIDs) {
            buf.writeVarInt(entityID);
        }
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleDestroyEntities(this);
    }

    public int[] getEntityIDs() {
        return this.entityIDs;
    }
}
