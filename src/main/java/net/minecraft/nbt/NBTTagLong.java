package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTTagLong extends NBTBase.NBTPrimitive {
    private long data;

    NBTTagLong() {
    }

    public NBTTagLong(long data) {
        this.data = data;
    }

    @Override
    void write(DataOutput output) throws IOException {
        output.writeLong(this.data);
    }

    @Override
    void read(DataInput input, int depth, NBTSizeTracker sizeTracker) throws IOException {
        sizeTracker.read(128L);
        this.data = input.readLong();
    }

    @Override
    public byte getId() {
        return (byte) 4;
    }

    public String toString() {
        return this.data + "L";
    }

    @Override
    public NBTBase copy() {
        return new NBTTagLong(this.data);
    }

    public boolean equals(Object p_equals_1_) {
        if (super.equals(p_equals_1_)) {
            NBTTagLong nbttaglong = (NBTTagLong) p_equals_1_;
            return this.data == nbttaglong.data;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return super.hashCode() ^ Long.hashCode(this.data);
    }

    @Override
    public long getLong() {
        return this.data;
    }

    @Override
    public int getInt() {
        return (int) (this.data & -1L);
    }

    @Override
    public short getShort() {
        return (short) ((int) (this.data & 65535L));
    }

    @Override
    public byte getByte() {
        return (byte) ((int) (this.data & 255L));
    }

    @Override
    public double getDouble() {
        return this.data;
    }

    @Override
    public float getFloat() {
        return this.data;
    }
}
