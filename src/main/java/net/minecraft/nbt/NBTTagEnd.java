package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTTagEnd extends NBTBase {
    @Override
    void read(DataInput input, int depth, NBTSizeTracker sizeTracker) throws IOException {
        sizeTracker.read(64L);
    }

    @Override
    void write(DataOutput output) throws IOException {
    }

    @Override
    public byte getId() {
        return (byte) 0;
    }

    public String toString() {
        return "END";
    }

    @Override
    public NBTBase copy() {
        return new NBTTagEnd();
    }
}
