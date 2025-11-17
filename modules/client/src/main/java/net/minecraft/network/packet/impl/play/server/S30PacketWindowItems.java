package net.minecraft.network.packet.impl.play.server;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;
import java.util.List;

public class S30PacketWindowItems implements Packet<INetHandlerPlayClient> {
    private int windowId;
    private ItemStack[] itemStacks;

    public S30PacketWindowItems() {
    }

    public S30PacketWindowItems(int windowIdIn, List<ItemStack> p_i45186_2_) {
        this.windowId = windowIdIn;
        this.itemStacks = new ItemStack[p_i45186_2_.size()];

        for (int i = 0; i < this.itemStacks.length; ++i) {
            ItemStack itemstack = p_i45186_2_.get(i);
            this.itemStacks[i] = itemstack == null ? null : itemstack.copy();
        }
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.windowId = buf.readUnsignedByte();
        int i = buf.readShort();
        this.itemStacks = new ItemStack[i];

        for (int j = 0; j < i; ++j) {
            this.itemStacks[j] = buf.readItemStack();
        }
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeByte(this.windowId);
        buf.writeShort(this.itemStacks.length);

        for (ItemStack itemstack : this.itemStacks) {
            buf.writeItemStack(itemstack);
        }
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleWindowItems(this);
    }

    public int func_148911_c() {
        return this.windowId;
    }

    public ItemStack[] getItemStacks() {
        return this.itemStacks;
    }
}
