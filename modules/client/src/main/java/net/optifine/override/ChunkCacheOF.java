package net.optifine.override;

import net.minecraft.block.state.IBlockState;
import net.optifine.Config;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.optifine.DynamicLights;
import net.optifine.util.collection.ArrayCache;

import java.util.Arrays;

public class ChunkCacheOF implements IBlockAccess {
    private static final ArrayCache CACHE_COMBINED_LIGHTS = new ArrayCache(Integer.TYPE, 16);
    private static final ArrayCache CACHE_BLOCK_STATES = new ArrayCache(IBlockState.class, 16);
    private final ChunkCache chunkCache;
    private final int posX;
    private final int posY;
    private final int posZ;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final int sizeXY;
    private final int arraySize;
    private final boolean dynamicLights = Config.isDynamicLights();
    private int[] combinedLights;
    private IBlockState[] blockStates;

    public ChunkCacheOF(ChunkCache chunkCache, BlockPos posFromIn, BlockPos posToIn, int subIn) {
        this.chunkCache = chunkCache;
        int i = posFromIn.getX() - subIn >> 4;
        int j = posFromIn.getY() - subIn >> 4;
        int k = posFromIn.getZ() - subIn >> 4;
        int l = posToIn.getX() + subIn >> 4;
        int i1 = posToIn.getY() + subIn >> 4;
        int j1 = posToIn.getZ() + subIn >> 4;
        this.sizeX = l - i + 1 << 4;
        this.sizeY = i1 - j + 1 << 4;
        this.sizeZ = j1 - k + 1 << 4;
        this.sizeXY = this.sizeX * this.sizeY;
        this.arraySize = this.sizeX * this.sizeY * this.sizeZ;
        this.posX = i << 4;
        this.posY = j << 4;
        this.posZ = k << 4;
    }

    private int getPositionIndex(BlockPos pos) {
        int i = pos.getX() - this.posX;

        if (i >= 0 && i < this.sizeX) {
            int j = pos.getY() - this.posY;

            if (j >= 0 && j < this.sizeY) {
                int k = pos.getZ() - this.posZ;
                return k >= 0 && k < this.sizeZ ? k * this.sizeXY + j * this.sizeX + i : -1;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        int i = this.getPositionIndex(pos);

        if (i >= 0 && i < this.arraySize && this.combinedLights != null) {
            int j = this.combinedLights[i];

            if (j == -1) {
                j = this.getCombinedLightRaw(pos, lightValue);
                this.combinedLights[i] = j;
            }

            return j;
        } else {
            return this.getCombinedLightRaw(pos, lightValue);
        }
    }

    private int getCombinedLightRaw(BlockPos pos, int lightValue) {
        int i = this.chunkCache.getCombinedLight(pos, lightValue);

        if (this.dynamicLights && !this.getBlockState(pos).getBlock().isOpaqueCube()) {
            i = DynamicLights.getCombinedLight(pos, i);
        }

        return i;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        int i = this.getPositionIndex(pos);

        if (i >= 0 && i < this.arraySize && this.blockStates != null) {
            IBlockState iblockstate = this.blockStates[i];

            if (iblockstate == null) {
                iblockstate = this.chunkCache.getBlockState(pos);
                this.blockStates[i] = iblockstate;
            }

            return iblockstate;
        } else {
            return this.chunkCache.getBlockState(pos);
        }
    }

    public void renderStart() {
        if (this.combinedLights == null) {
            this.combinedLights = (int[]) CACHE_COMBINED_LIGHTS.allocate(this.arraySize);
        }

        Arrays.fill(this.combinedLights, -1);

        if (this.blockStates == null) {
            this.blockStates = (IBlockState[]) CACHE_BLOCK_STATES.allocate(this.arraySize);
        }

        Arrays.fill(this.blockStates, null);
    }

    public void renderFinish() {
        CACHE_COMBINED_LIGHTS.free(this.combinedLights);
        this.combinedLights = null;
        CACHE_BLOCK_STATES.free(this.blockStates);
        this.blockStates = null;
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return this.chunkCache.extendedLevelsInChunkCache();
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(BlockPos pos) {
        return this.chunkCache.getBiomeGenForCoords(pos);
    }

    @Override
    public int getStrongPower(BlockPos pos, Direction direction) {
        return this.chunkCache.getStrongPower(pos, direction);
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return this.chunkCache.getTileEntity(pos);
    }

    @Override
    public WorldType getWorldType() {
        return this.chunkCache.getWorldType();
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        return this.chunkCache.isAirBlock(pos);
    }
}
