package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

public class S08PacketPlayerPosLook implements Packet<INetHandlerPlayClient> {
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private EnumSet<Flag> flags;

    public S08PacketPlayerPosLook() {
    }

    public S08PacketPlayerPosLook(double x, double y, double z, float yaw, float pitch, EnumSet<Flag> flags) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.flags = flags;
    }

    public void read(PacketBuffer buf) throws IOException {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.yaw = buf.readFloat();
        this.pitch = buf.readFloat();
        this.flags = Flag.fromByte(buf.readUnsignedByte());
    }

    public void write(PacketBuffer buf) throws IOException {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeFloat(this.yaw);
        buf.writeFloat(this.pitch);
        buf.writeByte(Flag.toByte(this.flags));
    }

    public void handle(INetHandlerPlayClient handler) {
        handler.handlePlayerPosLook(this);
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public Set<Flag> getFlags() {
        return this.flags;
    }

    public enum Flag {
        X(0),
        Y(1),
        Z(2),
        Y_ROT(3),
        X_ROT(4);

        private final int index;

        Flag(int index) {
            this.index = index;
        }

        private int bit() {
            return 1 << this.index;
        }

        private boolean isBit(int b) {
            return (b & this.bit()) == this.bit();
        }

        public static EnumSet<Flag> fromByte(int b) {
            EnumSet<Flag> set = EnumSet.noneOf(Flag.class);
            for (Flag flag : values()) {
                if (flag.isBit(b)) {
                    set.add(flag);
                }
            }
            return set;
        }

        public static int toByte(EnumSet<Flag> flags) {
            int i = 0;

            for (Flag flag : flags) {
                i |= flag.bit();
            }

            return i;
        }
    }
}
