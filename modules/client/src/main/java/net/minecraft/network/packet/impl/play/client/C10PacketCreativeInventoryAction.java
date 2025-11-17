package net.minecraft.network.packet.impl.play.client;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayServer;

import java.io.IOException;

public class C10PacketCreativeInventoryAction implements Packet<INetHandlerPlayServer> {
    private int slotId;
    private ItemStack stack;

    public C10PacketCreativeInventoryAction() {
    }

    public C10PacketCreativeInventoryAction(int slotIdIn, ItemStack stackIn) {
        this.slotId = slotIdIn;
        this.stack = stackIn != null ? stackIn.copy() : null;
    }

    @Override
    public void handle(INetHandlerPlayServer handler) {
        handler.processCreativeInventoryAction(this);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.slotId = buf.readShort();
        this.stack = buf.readItemStack();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeShort(this.slotId);
        buf.writeItemStack(this.stack);
    }

    public int getSlotId() {
        return this.slotId;
    }

    public ItemStack getStack() {
        return this.stack;
    }
}
