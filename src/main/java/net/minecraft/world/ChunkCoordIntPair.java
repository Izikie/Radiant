package net.minecraft.world;

import net.minecraft.util.BlockPos;

public class ChunkCoordIntPair {
    public final int chunkXPos;
    public final int chunkZPos;
    private int cachedHashCode = 0;

    public ChunkCoordIntPair(int x, int z) {
        this.chunkXPos = x;
        this.chunkZPos = z;
    }

    public static long chunkXZ2Int(int x, int z) {
        return x & 0xffffffffL | (z & 0xffffffffL) << 32;
    }

    public int hashCode() {
        if (this.cachedHashCode == 0) {
            int i = 0x19660d * this.chunkXPos + 0x3c6ef35f;
            int j = 0x19660d * (this.chunkZPos ^ 0xdeadbeef) + 0x3c6ef35f;
            this.cachedHashCode = i ^ j;
        }

        return this.cachedHashCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof ChunkCoordIntPair chunkcoordintpair)) {
            return false;
        } else {
            return this.chunkXPos == chunkcoordintpair.chunkXPos && this.chunkZPos == chunkcoordintpair.chunkZPos;
        }
    }

    public int getCenterXPos() {
        return (this.chunkXPos << 4) + 8;
    }

    public int getCenterZPosition() {
        return (this.chunkZPos << 4) + 8;
    }

    public int getXStart() {
        return this.chunkXPos << 4;
    }

    public int getZStart() {
        return this.chunkZPos << 4;
    }

    public int getXEnd() {
        return (this.chunkXPos << 4) + 15;
    }

    public int getZEnd() {
        return (this.chunkZPos << 4) + 15;
    }

    public BlockPos getBlock(int x, int y, int z) {
        return new BlockPos((this.chunkXPos << 4) + x, y, (this.chunkZPos << 4) + z);
    }

    public BlockPos getCenterBlock(int y) {
        return new BlockPos(this.getCenterXPos(), y, this.getCenterZPosition());
    }

    public String toString() {
        return "[" + this.chunkXPos + ", " + this.chunkZPos + "]";
    }
}
