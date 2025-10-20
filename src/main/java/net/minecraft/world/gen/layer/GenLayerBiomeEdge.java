package net.minecraft.world.gen.layer;

import net.minecraft.world.biome.BiomeGenBase;

public class GenLayerBiomeEdge extends GenLayer {
    public GenLayerBiomeEdge(long p_i45475_1_, GenLayer p_i45475_3_) {
        super(p_i45475_1_);
        this.parent = p_i45475_3_;
    }

    @Override
    public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
        int[] aint = this.parent.getInts(areaX - 1, areaY - 1, areaWidth + 2, areaHeight + 2);
        int[] aint1 = IntCache.getIntCache(areaWidth * areaHeight);

        for (int i = 0; i < areaHeight; ++i) {
            for (int j = 0; j < areaWidth; ++j) {
                this.initChunkSeed((j + areaX), (i + areaY));
                int k = aint[j + 1 + (i + 1) * (areaWidth + 2)];

                if (!this.replaceBiomeEdgeIfNecessary(aint, aint1, j, i, areaWidth, k, BiomeGenBase.EXTREME_HILLS.biomeID, BiomeGenBase.EXTREME_HILLS_EDGE.biomeID) && !this.replaceBiomeEdge(aint, aint1, j, i, areaWidth, k, BiomeGenBase.MESA_PLATEAU_F.biomeID, BiomeGenBase.MESA.biomeID) && !this.replaceBiomeEdge(aint, aint1, j, i, areaWidth, k, BiomeGenBase.MESA_PLATEAU.biomeID, BiomeGenBase.MESA.biomeID) && !this.replaceBiomeEdge(aint, aint1, j, i, areaWidth, k, BiomeGenBase.MEGA_TAIGA.biomeID, BiomeGenBase.TAIGA.biomeID)) {
                    if (k == BiomeGenBase.DESERT.biomeID) {
                        int l1 = aint[j + 1 + (i + 1 - 1) * (areaWidth + 2)];
                        int i2 = aint[j + 1 + 1 + (i + 1) * (areaWidth + 2)];
                        int j2 = aint[j + 1 - 1 + (i + 1) * (areaWidth + 2)];
                        int k2 = aint[j + 1 + (i + 1 + 1) * (areaWidth + 2)];

                        if (l1 != BiomeGenBase.ICE_PLAINS.biomeID && i2 != BiomeGenBase.ICE_PLAINS.biomeID && j2 != BiomeGenBase.ICE_PLAINS.biomeID && k2 != BiomeGenBase.ICE_PLAINS.biomeID) {
                            aint1[j + i * areaWidth] = k;
                        } else {
                            aint1[j + i * areaWidth] = BiomeGenBase.EXTREME_HILLS_PLUS.biomeID;
                        }
                    } else if (k == BiomeGenBase.SWAMPLAND.biomeID) {
                        int l = aint[j + 1 + (i + 1 - 1) * (areaWidth + 2)];
                        int i1 = aint[j + 1 + 1 + (i + 1) * (areaWidth + 2)];
                        int j1 = aint[j + 1 - 1 + (i + 1) * (areaWidth + 2)];
                        int k1 = aint[j + 1 + (i + 1 + 1) * (areaWidth + 2)];

                        if (l != BiomeGenBase.DESERT.biomeID && i1 != BiomeGenBase.DESERT.biomeID && j1 != BiomeGenBase.DESERT.biomeID && k1 != BiomeGenBase.DESERT.biomeID && l != BiomeGenBase.COLD_TAIGA.biomeID && i1 != BiomeGenBase.COLD_TAIGA.biomeID && j1 != BiomeGenBase.COLD_TAIGA.biomeID && k1 != BiomeGenBase.COLD_TAIGA.biomeID && l != BiomeGenBase.ICE_PLAINS.biomeID && i1 != BiomeGenBase.ICE_PLAINS.biomeID && j1 != BiomeGenBase.ICE_PLAINS.biomeID && k1 != BiomeGenBase.ICE_PLAINS.biomeID) {
                            if (l != BiomeGenBase.JUNGLE.biomeID && k1 != BiomeGenBase.JUNGLE.biomeID && i1 != BiomeGenBase.JUNGLE.biomeID && j1 != BiomeGenBase.JUNGLE.biomeID) {
                                aint1[j + i * areaWidth] = k;
                            } else {
                                aint1[j + i * areaWidth] = BiomeGenBase.JUNGLE_EDGE.biomeID;
                            }
                        } else {
                            aint1[j + i * areaWidth] = BiomeGenBase.PLAINS.biomeID;
                        }
                    } else {
                        aint1[j + i * areaWidth] = k;
                    }
                }
            }
        }

        return aint1;
    }

    private boolean replaceBiomeEdgeIfNecessary(int[] p_151636_1_, int[] p_151636_2_, int p_151636_3_, int p_151636_4_, int p_151636_5_, int p_151636_6_, int p_151636_7_, int p_151636_8_) {
        if (!biomesEqualOrMesaPlateau(p_151636_6_, p_151636_7_)) {
            return false;
        } else {
            int i = p_151636_1_[p_151636_3_ + 1 + (p_151636_4_ + 1 - 1) * (p_151636_5_ + 2)];
            int j = p_151636_1_[p_151636_3_ + 1 + 1 + (p_151636_4_ + 1) * (p_151636_5_ + 2)];
            int k = p_151636_1_[p_151636_3_ + 1 - 1 + (p_151636_4_ + 1) * (p_151636_5_ + 2)];
            int l = p_151636_1_[p_151636_3_ + 1 + (p_151636_4_ + 1 + 1) * (p_151636_5_ + 2)];

            if (this.canBiomesBeNeighbors(i, p_151636_7_) && this.canBiomesBeNeighbors(j, p_151636_7_) && this.canBiomesBeNeighbors(k, p_151636_7_) && this.canBiomesBeNeighbors(l, p_151636_7_)) {
                p_151636_2_[p_151636_3_ + p_151636_4_ * p_151636_5_] = p_151636_6_;
            } else {
                p_151636_2_[p_151636_3_ + p_151636_4_ * p_151636_5_] = p_151636_8_;
            }

            return true;
        }
    }

    private boolean replaceBiomeEdge(int[] p_151635_1_, int[] p_151635_2_, int p_151635_3_, int p_151635_4_, int p_151635_5_, int p_151635_6_, int p_151635_7_, int p_151635_8_) {
        if (p_151635_6_ != p_151635_7_) {
            return false;
        } else {
            int i = p_151635_1_[p_151635_3_ + 1 + (p_151635_4_ + 1 - 1) * (p_151635_5_ + 2)];
            int j = p_151635_1_[p_151635_3_ + 1 + 1 + (p_151635_4_ + 1) * (p_151635_5_ + 2)];
            int k = p_151635_1_[p_151635_3_ + 1 - 1 + (p_151635_4_ + 1) * (p_151635_5_ + 2)];
            int l = p_151635_1_[p_151635_3_ + 1 + (p_151635_4_ + 1 + 1) * (p_151635_5_ + 2)];

            if (biomesEqualOrMesaPlateau(i, p_151635_7_) && biomesEqualOrMesaPlateau(j, p_151635_7_) && biomesEqualOrMesaPlateau(k, p_151635_7_) && biomesEqualOrMesaPlateau(l, p_151635_7_)) {
                p_151635_2_[p_151635_3_ + p_151635_4_ * p_151635_5_] = p_151635_6_;
            } else {
                p_151635_2_[p_151635_3_ + p_151635_4_ * p_151635_5_] = p_151635_8_;
            }

            return true;
        }
    }

    private boolean canBiomesBeNeighbors(int p_151634_1_, int p_151634_2_) {
        if (biomesEqualOrMesaPlateau(p_151634_1_, p_151634_2_)) {
            return true;
        } else {
            BiomeGenBase biomegenbase = BiomeGenBase.getBiome(p_151634_1_);
            BiomeGenBase biomegenbase1 = BiomeGenBase.getBiome(p_151634_2_);

            if (biomegenbase != null && biomegenbase1 != null) {
                BiomeGenBase.TempCategory biomegenbase$tempcategory = biomegenbase.getTempCategory();
                BiomeGenBase.TempCategory biomegenbase$tempcategory1 = biomegenbase1.getTempCategory();
                return biomegenbase$tempcategory == biomegenbase$tempcategory1 || biomegenbase$tempcategory == BiomeGenBase.TempCategory.MEDIUM || biomegenbase$tempcategory1 == BiomeGenBase.TempCategory.MEDIUM;
            } else {
                return false;
            }
        }
    }
}
