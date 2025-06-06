package net.minecraft.world.gen.layer;

import net.minecraft.world.biome.BiomeGenBase;

public class GenLayerAddMushroomIsland extends GenLayer {
    public GenLayerAddMushroomIsland(long p_i2120_1_, GenLayer p_i2120_3_) {
        super(p_i2120_1_);
        this.parent = p_i2120_3_;
    }

    public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
        int i = areaX - 1;
        int j = areaY - 1;
        int k = areaWidth + 2;
        int l = areaHeight + 2;
        int[] aint = this.parent.getInts(i, j, k, l);
        int[] aint1 = IntCache.getIntCache(areaWidth * areaHeight);

        for (int i1 = 0; i1 < areaHeight; ++i1) {
            for (int j1 = 0; j1 < areaWidth; ++j1) {
                int k1 = aint[j1 + (i1) * k];
                int l1 = aint[j1 + 2 + (i1) * k];
                int i2 = aint[j1 + (i1 + 2) * k];
                int j2 = aint[j1 + 2 + (i1 + 2) * k];
                int k2 = aint[j1 + 1 + (i1 + 1) * k];
                this.initChunkSeed((j1 + areaX), (i1 + areaY));

                if (k2 == 0 && k1 == 0 && l1 == 0 && i2 == 0 && j2 == 0 && this.nextInt(100) == 0) {
                    aint1[j1 + i1 * areaWidth] = BiomeGenBase.MUSHROOM_ISLAND.biomeID;
                } else {
                    aint1[j1 + i1 * areaWidth] = k2;
                }
            }
        }

        return aint1;
    }
}
