package net.minecraft.world.gen.layer;

import net.minecraft.world.biome.BiomeGenBase;

public class GenLayerRiverMix extends GenLayer {
    private final GenLayer biomePatternGeneratorChain;
    private final GenLayer riverPatternGeneratorChain;

    public GenLayerRiverMix(long p_i2129_1_, GenLayer p_i2129_3_, GenLayer p_i2129_4_) {
        super(p_i2129_1_);
        this.biomePatternGeneratorChain = p_i2129_3_;
        this.riverPatternGeneratorChain = p_i2129_4_;
    }

    @Override
    public void initWorldGenSeed(long seed) {
        this.biomePatternGeneratorChain.initWorldGenSeed(seed);
        this.riverPatternGeneratorChain.initWorldGenSeed(seed);
        super.initWorldGenSeed(seed);
    }

    @Override
    public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
        int[] aint = this.biomePatternGeneratorChain.getInts(areaX, areaY, areaWidth, areaHeight);
        int[] aint1 = this.riverPatternGeneratorChain.getInts(areaX, areaY, areaWidth, areaHeight);
        int[] aint2 = IntCache.getIntCache(areaWidth * areaHeight);

        for (int i = 0; i < areaWidth * areaHeight; ++i) {
            if (aint[i] != BiomeGenBase.OCEAN.biomeID && aint[i] != BiomeGenBase.DEEP_OCEAN.biomeID) {
                if (aint1[i] == BiomeGenBase.RIVER.biomeID) {
                    if (aint[i] == BiomeGenBase.ICE_PLAINS.biomeID) {
                        aint2[i] = BiomeGenBase.FROZEN_RIVER.biomeID;
                    } else if (aint[i] != BiomeGenBase.MUSHROOM_ISLAND.biomeID && aint[i] != BiomeGenBase.MUSHROOM_ISLAND_SHORE.biomeID) {
                        aint2[i] = aint1[i] & 255;
                    } else {
                        aint2[i] = BiomeGenBase.MUSHROOM_ISLAND_SHORE.biomeID;
                    }
                } else {
                    aint2[i] = aint[i];
                }
            } else {
                aint2[i] = aint[i];
            }
        }

        return aint2;
    }
}
