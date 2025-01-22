package net.minecraft.world.gen.layer;

import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.ChunkProviderSettings;

public class GenLayerBiome extends GenLayer {
    private BiomeGenBase[] field_151623_c = new BiomeGenBase[]{BiomeGenBase.DESERT, BiomeGenBase.DESERT, BiomeGenBase.DESERT, BiomeGenBase.SAVANNA, BiomeGenBase.SAVANNA, BiomeGenBase.PLAINS};
    private final BiomeGenBase[] field_151621_d = new BiomeGenBase[]{BiomeGenBase.FOREST, BiomeGenBase.ROOFED_FOREST, BiomeGenBase.EXTREME_HILLS, BiomeGenBase.PLAINS, BiomeGenBase.BIRCH_FOREST, BiomeGenBase.SWAMPLAND};
    private final BiomeGenBase[] field_151622_e = new BiomeGenBase[]{BiomeGenBase.FOREST, BiomeGenBase.EXTREME_HILLS, BiomeGenBase.TAIGA, BiomeGenBase.PLAINS};
    private final BiomeGenBase[] field_151620_f = new BiomeGenBase[]{BiomeGenBase.ICE_PLAINS, BiomeGenBase.ICE_PLAINS, BiomeGenBase.ICE_PLAINS, BiomeGenBase.COLD_TAIGA};
    private final ChunkProviderSettings field_175973_g;

    public GenLayerBiome(long p_i45560_1_, GenLayer p_i45560_3_, WorldType p_i45560_4_, String p_i45560_5_) {
        super(p_i45560_1_);
        this.parent = p_i45560_3_;

        if (p_i45560_4_ == WorldType.DEFAULT_1_1) {
            this.field_151623_c = new BiomeGenBase[]{BiomeGenBase.DESERT, BiomeGenBase.FOREST, BiomeGenBase.EXTREME_HILLS, BiomeGenBase.SWAMPLAND, BiomeGenBase.PLAINS, BiomeGenBase.TAIGA};
            this.field_175973_g = null;
        } else if (p_i45560_4_ == WorldType.CUSTOMIZED) {
            this.field_175973_g = ChunkProviderSettings.Factory.jsonToFactory(p_i45560_5_).func_177864_b();
        } else {
            this.field_175973_g = null;
        }
    }

    public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
        int[] aint = this.parent.getInts(areaX, areaY, areaWidth, areaHeight);
        int[] aint1 = IntCache.getIntCache(areaWidth * areaHeight);

        for (int i = 0; i < areaHeight; ++i) {
            for (int j = 0; j < areaWidth; ++j) {
                this.initChunkSeed((j + areaX), (i + areaY));
                int k = aint[j + i * areaWidth];
                int l = (k & 3840) >> 8;
                k = k & -3841;

                if (this.field_175973_g != null && this.field_175973_g.fixedBiome >= 0) {
                    aint1[j + i * areaWidth] = this.field_175973_g.fixedBiome;
                } else if (isBiomeOceanic(k)) {
                    aint1[j + i * areaWidth] = k;
                } else if (k == BiomeGenBase.MUSHROOM_ISLAND.biomeID) {
                    aint1[j + i * areaWidth] = k;
                } else if (k == 1) {
                    if (l > 0) {
                        if (this.nextInt(3) == 0) {
                            aint1[j + i * areaWidth] = BiomeGenBase.MESA_PLATEAU.biomeID;
                        } else {
                            aint1[j + i * areaWidth] = BiomeGenBase.MESA_PLATEAU_F.biomeID;
                        }
                    } else {
                        aint1[j + i * areaWidth] = this.field_151623_c[this.nextInt(this.field_151623_c.length)].biomeID;
                    }
                } else if (k == 2) {
                    if (l > 0) {
                        aint1[j + i * areaWidth] = BiomeGenBase.JUNGLE.biomeID;
                    } else {
                        aint1[j + i * areaWidth] = this.field_151621_d[this.nextInt(this.field_151621_d.length)].biomeID;
                    }
                } else if (k == 3) {
                    if (l > 0) {
                        aint1[j + i * areaWidth] = BiomeGenBase.MEGA_TAIGA.biomeID;
                    } else {
                        aint1[j + i * areaWidth] = this.field_151622_e[this.nextInt(this.field_151622_e.length)].biomeID;
                    }
                } else if (k == 4) {
                    aint1[j + i * areaWidth] = this.field_151620_f[this.nextInt(this.field_151620_f.length)].biomeID;
                } else {
                    aint1[j + i * areaWidth] = BiomeGenBase.MUSHROOM_ISLAND.biomeID;
                }
            }
        }

        return aint1;
    }
}
