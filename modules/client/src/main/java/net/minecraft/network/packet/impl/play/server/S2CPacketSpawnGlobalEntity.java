package net.minecraft.network.packet.impl.play.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;

public class S2CPacketSpawnGlobalEntity implements Packet<INetHandlerPlayClient> {
    private int entityId;
    private int x;
    private int y;
    private int z;
    private int type;

    public S2CPacketSpawnGlobalEntity() {
    }

    public S2CPacketSpawnGlobalEntity(Entity entityIn) {
        this.entityId = entityIn.getEntityId();
        this.x = MathHelper.floor(entityIn.posX * 32.0D);
        this.y = MathHelper.floor(entityIn.posY * 32.0D);
        this.z = MathHelper.floor(entityIn.posZ * 32.0D);

        if (entityIn instanceof EntityLightningBolt) {
            this.type = 1;
        }
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.entityId = buf.readVarInt();
        this.type = buf.readByte();
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeVarInt(this.entityId);
        buf.writeByte(this.type);
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleSpawnGlobalEntity(this);
    }

    public int func_149052_c() {
        return this.entityId;
    }

    public int func_149051_d() {
        return this.x;
    }

    public int func_149050_e() {
        return this.y;
    }

    public int func_149049_f() {
        return this.z;
    }

    public int func_149053_g() {
        return this.type;
    }
}
