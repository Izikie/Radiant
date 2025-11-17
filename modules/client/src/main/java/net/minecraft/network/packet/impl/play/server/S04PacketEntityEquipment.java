package net.minecraft.network.packet.impl.play.server;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;

public class S04PacketEntityEquipment implements Packet<INetHandlerPlayClient> {
    private int entityID;
    private int equipmentSlot;
    private ItemStack itemStack;

    public S04PacketEntityEquipment() {
    }

    public S04PacketEntityEquipment(int entityIDIn, int p_i45221_2_, ItemStack itemStackIn) {
        this.entityID = entityIDIn;
        this.equipmentSlot = p_i45221_2_;
        this.itemStack = itemStackIn == null ? null : itemStackIn.copy();
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.entityID = buf.readVarInt();
        this.equipmentSlot = buf.readShort();
        this.itemStack = buf.readItemStack();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeVarInt(this.entityID);
        buf.writeShort(this.equipmentSlot);
        buf.writeItemStack(this.itemStack);
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleEntityEquipment(this);
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public int getEntityID() {
        return this.entityID;
    }

    public int getEquipmentSlot() {
        return this.equipmentSlot;
    }
}
