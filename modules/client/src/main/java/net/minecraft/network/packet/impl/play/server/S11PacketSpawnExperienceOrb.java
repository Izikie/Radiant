package net.minecraft.network.packet.impl.play.server;

import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;

public class S11PacketSpawnExperienceOrb implements Packet<INetHandlerPlayClient> {
    private int entityID;
    private int posX;
    private int posY;
    private int posZ;
    private int xpValue;

    public S11PacketSpawnExperienceOrb() {
    }

    public S11PacketSpawnExperienceOrb(EntityXPOrb xpOrb) {
        this.entityID = xpOrb.getEntityId();
        this.posX = MathHelper.floor(xpOrb.posX * 32.0D);
        this.posY = MathHelper.floor(xpOrb.posY * 32.0D);
        this.posZ = MathHelper.floor(xpOrb.posZ * 32.0D);
        this.xpValue = xpOrb.getXpValue();
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.entityID = buf.readVarInt();
        this.posX = buf.readInt();
        this.posY = buf.readInt();
        this.posZ = buf.readInt();
        this.xpValue = buf.readShort();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeVarInt(this.entityID);
        buf.writeInt(this.posX);
        buf.writeInt(this.posY);
        buf.writeInt(this.posZ);
        buf.writeShort(this.xpValue);
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleSpawnExperienceOrb(this);
    }

    public int getEntityID() {
        return this.entityID;
    }

    public int getX() {
        return this.posX;
    }

    public int getY() {
        return this.posY;
    }

    public int getZ() {
        return this.posZ;
    }

    public int getXPValue() {
        return this.xpValue;
    }
}
