package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTTagByte extends NBTBase.NBTPrimitive {
    private byte data;

    NBTTagByte() {
    }

    public NBTTagByte(byte data) {
        this.data = data;
    }

    @Override
    void write(DataOutput output) throws IOException {
        output.writeByte(this.data);
    }

    @Override
    void read(DataInput input, int depth, NBTSizeTracker sizeTracker) throws IOException {
        sizeTracker.read(72L);
        this.data = input.readByte();
    }

    @Override
    public byte getId() {
        return (byte) 1;
    }

    public String toString() {
        return this.data + "b";
    }

    @Override
    public NBTBase copy() {
        return new NBTTagByte(this.data);
    }

    public boolean equals(Object p_equals_1_) {
        if (super.equals(p_equals_1_)) {
            NBTTagByte nbttagbyte = (NBTTagByte) p_equals_1_;
            return this.data == nbttagbyte.data;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return super.hashCode() ^ this.data;
    }

    @Override
    public long getLong() {
        return this.data;
    }

    @Override
    public int getInt() {
        return this.data;
    }

    @Override
    public short getShort() {
        return this.data;
    }

    @Override
    public byte getByte() {
        return this.data;
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
