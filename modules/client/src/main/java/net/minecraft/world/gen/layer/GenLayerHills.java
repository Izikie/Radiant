package net.minecraft.world.gen.layer;

import net.minecraft.world.biome.BiomeGenBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenLayerHills extends GenLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenLayerHills.class);
    private final GenLayer field_151628_d;

    public GenLayerHills(long p_i45479_1_, GenLayer p_i45479_3_, GenLayer p_i45479_4_) {
        super(p_i45479_1_);
        this.parent = p_i45479_3_;
        this.field_151628_d = p_i45479_4_;
    }

    @Override
    public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
        int[] aint = this.parent.getInts(areaX - 1, areaY - 1, areaWidth + 2, areaHeight + 2);
        int[] aint1 = this.field_151628_d.getInts(areaX - 1, areaY - 1, areaWidth + 2, areaHeight + 2);
        int[] aint2 = IntCache.getIntCache(areaWidth * areaHeight);

        for (int i = 0; i < areaHeight; ++i) {
            for (int j = 0; j < areaWidth; ++j) {
                this.initChunkSeed((j + areaX), (i + areaY));
                int k = aint[j + 1 + (i + 1) * (areaWidth + 2)];
                int l = aint1[j + 1 + (i + 1) * (areaWidth + 2)];
                boolean flag = (l - 2) % 29 == 0;

                if (k > 255) {
                    LOGGER.debug("old! {}", k);
                }

                if (k != 0 && l >= 2 && (l - 2) % 29 == 1 && k < 128) {
                    if (BiomeGenBase.getBiome(k + 128) != null) {
                        aint2[j + i * areaWidth] = k + 128;
                    } else {
                        aint2[j + i * areaWidth] = k;
                    }
                } else if (this.nextInt(3) != 0 && !flag) {
                    aint2[j + i * areaWidth] = k;
                } else {
                    int i1 = k;

                    if (k == BiomeGenBase.DESERT.biomeID) {
                        i1 = BiomeGenBase.DESERT_HILLS.biomeID;
                    } else if (k == BiomeGenBase.FOREST.biomeID) {
                        i1 = BiomeGenBase.FOREST_HILLS.biomeID;
                    } else if (k == BiomeGenBase.BIRCH_FOREST.biomeID) {
                        i1 = BiomeGenBase.BIRCH_FOREST_HILLS.biomeID;
                    } else if (k == BiomeGenBase.ROOFED_FOREST.biomeID) {
                        i1 = BiomeGenBase.PLAINS.biomeID;
                    } else if (k == BiomeGenBase.TAIGA.biomeID) {
                        i1 = BiomeGenBase.TAIGA_HILLS.biomeID;
                    } else if (k == BiomeGenBase.MEGA_TAIGA.biomeID) {
                        i1 = BiomeGenBase.MEGA_TAIGA_HILLS.biomeID;
                    } else if (k == BiomeGenBase.COLD_TAIGA.biomeID) {
                        i1 = BiomeGenBase.COLD_TAIGA_HILLS.biomeID;
                    } else if (k == BiomeGenBase.PLAINS.biomeID) {
                        if (this.nextInt(3) == 0) {
                            i1 = BiomeGenBase.FOREST_HILLS.biomeID;
                        } else {
                            i1 = BiomeGenBase.FOREST.biomeID;
                        }
                    } else if (k == BiomeGenBase.ICE_PLAINS.biomeID) {
                        i1 = BiomeGenBase.ICE_MOUNTAINS.biomeID;
                    } else if (k == BiomeGenBase.JUNGLE.biomeID) {
                        i1 = BiomeGenBase.JUNGLE_HILLS.biomeID;
                    } else if (k == BiomeGenBase.OCEAN.biomeID) {
                        i1 = BiomeGenBase.DEEP_OCEAN.biomeID;
                    } else if (k == BiomeGenBase.EXTREME_HILLS.biomeID) {
                        i1 = BiomeGenBase.EXTREME_HILLS_PLUS.biomeID;
                    } else if (k == BiomeGenBase.SAVANNA.biomeID) {
                        i1 = BiomeGenBase.SAVANNA_PLATEAU.biomeID;
                    } else if (biomesEqualOrMesaPlateau(k, BiomeGenBase.MESA_PLATEAU_F.biomeID)) {
                        i1 = BiomeGenBase.MESA.biomeID;
                    } else if (k == BiomeGenBase.DEEP_OCEAN.biomeID && this.nextInt(3) == 0) {
                        int j1 = this.nextInt(2);

                        if (j1 == 0) {
                            i1 = BiomeGenBase.PLAINS.biomeID;
                        } else {
                            i1 = BiomeGenBase.FOREST.biomeID;
                        }
                    }

                    if (flag && i1 != k) {
                        if (BiomeGenBase.getBiome(i1 + 128) != null) {
                            i1 += 128;
                        } else {
                            i1 = k;
                        }
                    }

                    if (i1 == k) {
                        aint2[j + i * areaWidth] = k;
                    } else {
                        int k2 = aint[j + 1 + (i + 1 - 1) * (areaWidth + 2)];
                        int k1 = aint[j + 1 + 1 + (i + 1) * (areaWidth + 2)];
                        int l1 = aint[j + 1 - 1 + (i + 1) * (areaWidth + 2)];
                        int i2 = aint[j + 1 + (i + 1 + 1) * (areaWidth + 2)];
                        int j2 = 0;

                        if (biomesEqualOrMesaPlateau(k2, k)) {
                            ++j2;
                        }

                        if (biomesEqualOrMesaPlateau(k1, k)) {
                            ++j2;
                        }

                        if (biomesEqualOrMesaPlateau(l1, k)) {
                            ++j2;
                        }

                        if (biomesEqualOrMesaPlateau(i2, k)) {
                            ++j2;
                        }

                        if (j2 >= 3) {
                            aint2[j + i * areaWidth] = i1;
                        } else {
                            aint2[j + i * areaWidth] = k;
                        }
                    }
                }
            }
        }

        return aint2;
    }
}
