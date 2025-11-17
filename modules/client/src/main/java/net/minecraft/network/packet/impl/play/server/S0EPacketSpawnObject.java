package net.minecraft.network.packet.impl.play.server;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;

public class S0EPacketSpawnObject implements Packet<INetHandlerPlayClient> {
    private int entityId;
    private int x;
    private int y;
    private int z;
    private int speedX;
    private int speedY;
    private int speedZ;
    private int pitch;
    private int yaw;
    private int type;
    private int extraData;

    public S0EPacketSpawnObject() {
    }

    public S0EPacketSpawnObject(Entity entityIn, int typeIn) {
        this(entityIn, typeIn, 0);
    }

    public S0EPacketSpawnObject(Entity entityIn, int typeIn, int extraData) {
        this.entityId = entityIn.getEntityId();
        this.x = MathHelper.floor(entityIn.posX * 32.0D);
        this.y = MathHelper.floor(entityIn.posY * 32.0D);
        this.z = MathHelper.floor(entityIn.posZ * 32.0D);
        this.pitch = MathHelper.floor(entityIn.rotationPitch * 256.0F / 360.0F);
        this.yaw = MathHelper.floor(entityIn.rotationYaw * 256.0F / 360.0F);
        this.type = typeIn;
        this.extraData = extraData;

        if (extraData > 0) {
            double d0 = entityIn.motionX;
            double d1 = entityIn.motionY;
            double d2 = entityIn.motionZ;
            double d3 = 3.9D;

            if (d0 < -d3) {
                d0 = -d3;
            }

            if (d1 < -d3) {
                d1 = -d3;
            }

            if (d2 < -d3) {
                d2 = -d3;
            }

            if (d0 > d3) {
                d0 = d3;
            }

            if (d1 > d3) {
                d1 = d3;
            }

            if (d2 > d3) {
                d2 = d3;
            }

            this.speedX = (int) (d0 * 8000.0D);
            this.speedY = (int) (d1 * 8000.0D);
            this.speedZ = (int) (d2 * 8000.0D);
        }
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.entityId = buf.readVarInt();
        this.type = buf.readByte();
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.pitch = buf.readByte();
        this.yaw = buf.readByte();
        this.extraData = buf.readInt();

        if (this.extraData > 0) {
            this.speedX = buf.readShort();
            this.speedY = buf.readShort();
            this.speedZ = buf.readShort();
        }
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeVarInt(this.entityId);
        buf.writeByte(this.type);
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
        buf.writeByte(this.pitch);
        buf.writeByte(this.yaw);
        buf.writeInt(this.extraData);

        if (this.extraData > 0) {
            buf.writeShort(this.speedX);
            buf.writeShort(this.speedY);
            buf.writeShort(this.speedZ);
        }
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleSpawnObject(this);
    }

    public int getEntityID() {
        return this.entityId;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public int getSpeedX() {
        return this.speedX;
    }

    public int getSpeedY() {
        return this.speedY;
    }

    public int getSpeedZ() {
        return this.speedZ;
    }

    public int getPitch() {
        return this.pitch;
    }

    public int getYaw() {
        return this.yaw;
    }

    public int getType() {
        return this.type;
    }

    public int getExtraData() {
        return this.extraData;
    }

    public void setX(int newX) {
        this.x = newX;
    }

    public void setY(int newY) {
        this.y = newY;
    }

    public void setZ(int newZ) {
        this.z = newZ;
    }

    public void setSpeedX(int newSpeedX) {
        this.speedX = newSpeedX;
    }

    public void setSpeedY(int newSpeedY) {
        this.speedY = newSpeedY;
    }

    public void setSpeedZ(int newSpeedZ) {
        this.speedZ = newSpeedZ;
    }

    public void setExtraData(int extraData) {
        this.extraData = extraData;
    }
}
