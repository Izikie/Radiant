package net.minecraft.network.packet.impl.play.server;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.world.World;

import java.io.IOException;

public class S49PacketUpdateEntityNBT implements Packet<INetHandlerPlayClient> {
    private int entityId;
    private NBTTagCompound tagCompound;

    public S49PacketUpdateEntityNBT() {
    }

    public S49PacketUpdateEntityNBT(int entityIdIn, NBTTagCompound tagCompoundIn) {
        this.entityId = entityIdIn;
        this.tagCompound = tagCompoundIn;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.entityId = buf.readVarIntFromBuffer();
        this.tagCompound = buf.readNBTTagCompoundFromBuffer();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarIntToBuffer(this.entityId);
        buf.writeNBTTagCompoundToBuffer(this.tagCompound);
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleEntityNBT(this);
    }

    public NBTTagCompound getTagCompound() {
        return this.tagCompound;
    }

    public Entity getEntity(World worldIn) {
        return worldIn.getEntityByID(this.entityId);
    }
}
