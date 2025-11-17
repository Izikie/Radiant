package net.minecraft.network.packet.impl.play.client;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayServer;

import java.io.IOException;

public class C09PacketHeldItemChange implements Packet<INetHandlerPlayServer> {
    private int slotId;

    public C09PacketHeldItemChange() {
    }

    public C09PacketHeldItemChange(int slotId) {
        this.slotId = slotId;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.slotId = buf.readShort();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeShort(this.slotId);
    }

    @Override
    public void handle(INetHandlerPlayServer handler) {
        handler.processHeldItemChange(this);
    }

    public int getSlotId() {
        return this.slotId;
    }
}
