package net.minecraft.network.play.server;

import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;
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

    public void readPacketData(PacketBuffer buf) throws IOException {
        this.entityID = buf.readVarIntFromBuffer();
        this.posX = buf.readInt();
        this.posY = buf.readInt();
        this.posZ = buf.readInt();
        this.xpValue = buf.readShort();
    }

    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarIntToBuffer(this.entityID);
        buf.writeInt(this.posX);
        buf.writeInt(this.posY);
        buf.writeInt(this.posZ);
        buf.writeShort(this.xpValue);
    }

    public void processPacket(INetHandlerPlayClient handler) {
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
