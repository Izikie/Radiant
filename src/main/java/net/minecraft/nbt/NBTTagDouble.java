package net.minecraft.nbt;

import net.minecraft.util.math.MathHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTTagDouble extends NBTBase.NBTPrimitive {
    private double data;

    NBTTagDouble() {
    }

    public NBTTagDouble(double data) {
        this.data = data;
    }

    @Override
    void write(DataOutput output) throws IOException {
        output.writeDouble(this.data);
    }

    @Override
    void read(DataInput input, int depth, NBTSizeTracker sizeTracker) throws IOException {
        sizeTracker.read(128L);
        this.data = input.readDouble();
    }

    @Override
    public byte getId() {
        return (byte) 6;
    }

    public String toString() {
        return this.data + "d";
    }

    @Override
    public NBTBase copy() {
        return new NBTTagDouble(this.data);
    }

    public boolean equals(Object p_equals_1_) {
        if (super.equals(p_equals_1_)) {
            NBTTagDouble nbttagdouble = (NBTTagDouble) p_equals_1_;
            return this.data == nbttagdouble.data;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return super.hashCode() ^ Double.hashCode(this.data);
    }

    @Override
    public long getLong() {
        return (long) Math.floor(this.data);
    }

    @Override
    public int getInt() {
        return MathHelper.floor(this.data);
    }

    @Override
    public short getShort() {
        return (short) (MathHelper.floor(this.data) & 65535);
    }

    @Override
    public byte getByte() {
        return (byte) (MathHelper.floor(this.data) & 255);
    }

    @Override
    public double getDouble() {
        return this.data;
    }

    @Override
    public float getFloat() {
        return (float) this.data;
    }
}
