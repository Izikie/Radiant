package net.minecraft.world.biome;

import com.google.common.collect.Lists;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;

import java.util.List;
import java.util.Random;

public class WorldChunkManager {
    private GenLayer genBiomes;
    private GenLayer biomeIndexLayer;
    private final BiomeCache biomeCache;
    private final List<BiomeGenBase> biomesToSpawnIn;
    private String generatorOptions;

    protected WorldChunkManager() {
        this.biomeCache = new BiomeCache(this);
        this.generatorOptions = "";
        this.biomesToSpawnIn = Lists.newArrayList();
        this.biomesToSpawnIn.add(BiomeGenBase.FOREST);
        this.biomesToSpawnIn.add(BiomeGenBase.PLAINS);
        this.biomesToSpawnIn.add(BiomeGenBase.TAIGA);
        this.biomesToSpawnIn.add(BiomeGenBase.TAIGA_HILLS);
        this.biomesToSpawnIn.add(BiomeGenBase.FOREST_HILLS);
        this.biomesToSpawnIn.add(BiomeGenBase.JUNGLE);
        this.biomesToSpawnIn.add(BiomeGenBase.JUNGLE_HILLS);
    }

    public WorldChunkManager(long seed, WorldType worldTypeIn, String options) {
        this();
        this.generatorOptions = options;
        GenLayer[] agenlayer = GenLayer.initializeAllBiomeGenerators(seed, worldTypeIn, options);
        this.genBiomes = agenlayer[0];
        this.biomeIndexLayer = agenlayer[1];
    }

    public WorldChunkManager(World worldIn) {
        this(worldIn.getSeed(), worldIn.getWorldInfo().getTerrainType(), worldIn.getWorldInfo().getGeneratorOptions());
    }

    public List<BiomeGenBase> getBiomesToSpawnIn() {
        return this.biomesToSpawnIn;
    }

    public BiomeGenBase getBiomeGenerator(BlockPos pos) {
        return this.getBiomeGenerator(pos, null);
    }

    public BiomeGenBase getBiomeGenerator(BlockPos pos, BiomeGenBase biomeGenBaseIn) {
        return this.biomeCache.func_180284_a(pos.getX(), pos.getZ(), biomeGenBaseIn);
    }

    public float[] getRainfall(float[] listToReuse, int x, int z, int width, int length) {
        IntCache.resetIntCache();

        if (listToReuse == null || listToReuse.length < width * length) {
            listToReuse = new float[width * length];
        }

        int[] aint = this.biomeIndexLayer.getInts(x, z, width, length);

        for (int i = 0; i < width * length; ++i) {
            try {
                float f = BiomeGenBase.getBiomeFromBiomeList(aint[i], BiomeGenBase.OCEAN2).getIntRainfall() / 65536.0F;

                if (f > 1.0F) {
                    f = 1.0F;
                }

                listToReuse[i] = f;
            } catch (Throwable throwable) {
                CrashReport report = CrashReport.makeCrashReport(throwable, "Invalid Biome ID");
                CrashReportCategory category = report.makeCategory("DownfallBlock");
                category.addCrashSection("Biome ID", i);
                category.addCrashSection("DownFalls[] Size", listToReuse.length);
                category.addCrashSection("X", x);
                category.addCrashSection("Z", z);
                category.addCrashSection("Width", width);
                category.addCrashSection("Hight", length);
                throw new ReportedException(report);
            }
        }

        return listToReuse;
    }

    public float getTemperatureAtHeight(float p_76939_1_, int p_76939_2_) {
        return p_76939_1_;
    }

    public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase[] biomes, int x, int z, int width, int height) {
        IntCache.resetIntCache();

        if (biomes == null || biomes.length < width * height) {
            biomes = new BiomeGenBase[width * height];
        }

        int[] aint = this.genBiomes.getInts(x, z, width, height);

        try {
            for (int i = 0; i < width * height; ++i) {
                biomes[i] = BiomeGenBase.getBiomeFromBiomeList(aint[i], BiomeGenBase.OCEAN2);
            }

            return biomes;
        } catch (Throwable throwable) {
            CrashReport report = CrashReport.makeCrashReport(throwable, "Invalid Biome id");
            CrashReportCategory category = report.makeCategory("RawBiomeBlock");
            category.addCrashSection("Biomes[] Size", biomes.length);
            category.addCrashSection("X", x);
            category.addCrashSection("Z", z);
            category.addCrashSection("Width", width);
            category.addCrashSection("Height", height);
            throw new ReportedException(report);
        }
    }

    public BiomeGenBase[] loadBlockGeneratorData(BiomeGenBase[] oldBiomeList, int x, int z, int width, int depth) {
        return this.getBiomeGenAt(oldBiomeList, x, z, width, depth, true);
    }

    public BiomeGenBase[] getBiomeGenAt(BiomeGenBase[] listToReuse, int x, int z, int width, int length, boolean cacheFlag) {
        IntCache.resetIntCache();

        if (listToReuse == null || listToReuse.length < width * length) {
            listToReuse = new BiomeGenBase[width * length];
        }

        if (cacheFlag && width == 16 && length == 16 && (x & 15) == 0 && (z & 15) == 0) {
            BiomeGenBase[] abiomegenbase = this.biomeCache.getCachedBiomes(x, z);
            System.arraycopy(abiomegenbase, 0, listToReuse, 0, width * length);
        } else {
            int[] aint = this.biomeIndexLayer.getInts(x, z, width, length);

            for (int i = 0; i < width * length; ++i) {
                listToReuse[i] = BiomeGenBase.getBiomeFromBiomeList(aint[i], BiomeGenBase.OCEAN2);
            }

        }
        return listToReuse;
    }

    public boolean areBiomesViable(int p_76940_1_, int p_76940_2_, int p_76940_3_, List<BiomeGenBase> p_76940_4_) {
        IntCache.resetIntCache();
        int i = p_76940_1_ - p_76940_3_ >> 2;
        int j = p_76940_2_ - p_76940_3_ >> 2;
        int k = p_76940_1_ + p_76940_3_ >> 2;
        int l = p_76940_2_ + p_76940_3_ >> 2;
        int i1 = k - i + 1;
        int j1 = l - j + 1;
        int[] aint = this.genBiomes.getInts(i, j, i1, j1);

        try {
            for (int k1 = 0; k1 < i1 * j1; ++k1) {
                BiomeGenBase biomegenbase = BiomeGenBase.getBiome(aint[k1]);

                if (!p_76940_4_.contains(biomegenbase)) {
                    return false;
                }
            }

            return true;
        } catch (Throwable throwable) {
            CrashReport report = CrashReport.makeCrashReport(throwable, "Invalid Biome id");
            CrashReportCategory category = report.makeCategory("Layer");
            category.addCrashSection("Layer", this.genBiomes.toString());
            category.addCrashSection("X", p_76940_1_);
            category.addCrashSection("Z", p_76940_2_);
            category.addCrashSection("Radius", p_76940_3_);
            category.addCrashSection("Allowed", p_76940_4_);
            throw new ReportedException(report);
        }
    }

    public BlockPos findBiomePosition(int x, int z, int range, List<BiomeGenBase> biomes, Random random) {
        IntCache.resetIntCache();
        int i = x - range >> 2;
        int j = z - range >> 2;
        int k = x + range >> 2;
        int l = z + range >> 2;
        int i1 = k - i + 1;
        int j1 = l - j + 1;
        int[] aint = this.genBiomes.getInts(i, j, i1, j1);
        BlockPos blockpos = null;
        int k1 = 0;

        for (int l1 = 0; l1 < i1 * j1; ++l1) {
            int i2 = i + l1 % i1 << 2;
            int j2 = j + l1 / i1 << 2;
            BiomeGenBase biomegenbase = BiomeGenBase.getBiome(aint[l1]);

            if (biomes.contains(biomegenbase) && (blockpos == null || random.nextInt(k1 + 1) == 0)) {
                blockpos = new BlockPos(i2, 0, j2);
                ++k1;
            }
        }

        return blockpos;
    }

    public void cleanupCache() {
        this.biomeCache.cleanupCache();
    }
}
