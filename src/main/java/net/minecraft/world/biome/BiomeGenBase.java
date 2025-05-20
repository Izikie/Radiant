package net.minecraft.world.biome;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockSand;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.*;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.ColorizerGrass;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraft.world.gen.feature.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public abstract class BiomeGenBase {
    private static final Logger LOGGER = LogManager.getLogger();
    protected static final Height HEIGHT_DEFAULT = new Height(0.1F, 0.2F);
    protected static final Height HEIGHT_SHALLOW_WATERS = new Height(-0.5F, 0.0F);
    protected static final Height HEIGHT_OCEANS = new Height(-1.0F, 0.1F);
    protected static final Height HEIGHT_DEEP_OCEANS = new Height(-1.8F, 0.1F);
    protected static final Height HEIGHT_LOW_PLAINS = new Height(0.125F, 0.05F);
    protected static final Height HEIGHT_MID_PLAINS = new Height(0.2F, 0.2F);
    protected static final Height HEIGHT_LOW_HILLS = new Height(0.45F, 0.3F);
    protected static final Height HEIGHT_HIGH_PLATEAUS = new Height(1.5F, 0.025F);
    protected static final Height HEIGHT_MID_HILLS = new Height(1.0F, 0.5F);
    protected static final Height HEIGHT_SHORES = new Height(0.0F, 0.025F);
    protected static final Height HEIGHT_ROCKY_WATERS = new Height(0.1F, 0.8F);
    protected static final Height HEIGHT_LOW_ISLANDS = new Height(0.2F, 0.3F);
    protected static final Height HEIGHT_PARTIALLY_SUBMERGED = new Height(-0.2F, 0.1F);
    private static final BiomeGenBase[] BIOME_LIST = new BiomeGenBase[256];
    public static final Set<BiomeGenBase> EXPLORATION_BIOMES_LIST = Sets.newHashSet();
    public static final Map<String, BiomeGenBase> BIOME_ID_MAP = new HashMap<>();
    public static final BiomeGenBase OCEAN = (new BiomeGenOcean(0)).setColor(112).setBiomeName("Ocean").setHeight(HEIGHT_OCEANS);
    public static final BiomeGenBase PLAINS = (new BiomeGenPlains(1)).setColor(9286496).setBiomeName("Plains");
    public static final BiomeGenBase DESERT = (new BiomeGenDesert(2)).setColor(16421912).setBiomeName("Desert").setDisableRain().setTemperatureRainfall(2.0F, 0.0F).setHeight(HEIGHT_LOW_PLAINS);
    public static final BiomeGenBase EXTREME_HILLS = (new BiomeGenHills(3, false)).setColor(6316128).setBiomeName("Extreme Hills").setHeight(HEIGHT_MID_HILLS).setTemperatureRainfall(0.2F, 0.3F);
    public static final BiomeGenBase FOREST = (new BiomeGenForest(4, 0)).setColor(353825).setBiomeName("Forest");
    public static final BiomeGenBase TAIGA = (new BiomeGenTaiga(5, 0)).setColor(747097).setBiomeName("Taiga").setFillerBlockMetadata(5159473).setTemperatureRainfall(0.25F, 0.8F).setHeight(HEIGHT_MID_PLAINS);
    public static final BiomeGenBase SWAMPLAND = (new BiomeGenSwamp(6)).setColor(522674).setBiomeName("Swampland").setFillerBlockMetadata(9154376).setHeight(HEIGHT_PARTIALLY_SUBMERGED).setTemperatureRainfall(0.8F, 0.9F);
    public static final BiomeGenBase RIVER = (new BiomeGenRiver(7)).setColor(255).setBiomeName("River").setHeight(HEIGHT_SHALLOW_WATERS);
    public static final BiomeGenBase HELL = (new BiomeGenHell(8)).setColor(16711680).setBiomeName("Hell").setDisableRain().setTemperatureRainfall(2.0F, 0.0F);
    public static final BiomeGenBase SKY = (new BiomeGenEnd(9)).setColor(8421631).setBiomeName("The End").setDisableRain();
    public static final BiomeGenBase FROZEN_OCEAN = (new BiomeGenOcean(10)).setColor(9474208).setBiomeName("FrozenOcean").setEnableSnow().setHeight(HEIGHT_OCEANS).setTemperatureRainfall(0.0F, 0.5F);
    public static final BiomeGenBase FROZEN_RIVER = (new BiomeGenRiver(11)).setColor(10526975).setBiomeName("FrozenRiver").setEnableSnow().setHeight(HEIGHT_SHALLOW_WATERS).setTemperatureRainfall(0.0F, 0.5F);
    public static final BiomeGenBase ICE_PLAINS = (new BiomeGenSnow(12, false)).setColor(16777215).setBiomeName("Ice Plains").setEnableSnow().setTemperatureRainfall(0.0F, 0.5F).setHeight(HEIGHT_LOW_PLAINS);
    public static final BiomeGenBase ICE_MOUNTAINS = (new BiomeGenSnow(13, false)).setColor(10526880).setBiomeName("Ice Mountains").setEnableSnow().setHeight(HEIGHT_LOW_HILLS).setTemperatureRainfall(0.0F, 0.5F);
    public static final BiomeGenBase MUSHROOM_ISLAND = (new BiomeGenMushroomIsland(14)).setColor(16711935).setBiomeName("MushroomIsland").setTemperatureRainfall(0.9F, 1.0F).setHeight(HEIGHT_LOW_ISLANDS);
    public static final BiomeGenBase MUSHROOM_ISLAND_SHORE = (new BiomeGenMushroomIsland(15)).setColor(10486015).setBiomeName("MushroomIslandShore").setTemperatureRainfall(0.9F, 1.0F).setHeight(HEIGHT_SHORES);
    public static final BiomeGenBase BEACH = (new BiomeGenBeach(16)).setColor(16440917).setBiomeName("Beach").setTemperatureRainfall(0.8F, 0.4F).setHeight(HEIGHT_SHORES);
    public static final BiomeGenBase DESERT_HILLS = (new BiomeGenDesert(17)).setColor(13786898).setBiomeName("DesertHills").setDisableRain().setTemperatureRainfall(2.0F, 0.0F).setHeight(HEIGHT_LOW_HILLS);
    public static final BiomeGenBase FOREST_HILLS = (new BiomeGenForest(18, 0)).setColor(2250012).setBiomeName("ForestHills").setHeight(HEIGHT_LOW_HILLS);
    public static final BiomeGenBase TAIGA_HILLS = (new BiomeGenTaiga(19, 0)).setColor(1456435).setBiomeName("TaigaHills").setFillerBlockMetadata(5159473).setTemperatureRainfall(0.25F, 0.8F).setHeight(HEIGHT_LOW_HILLS);
    public static final BiomeGenBase EXTREME_HILLS_EDGE = (new BiomeGenHills(20, true)).setColor(7501978).setBiomeName("Extreme Hills Edge").setHeight(HEIGHT_MID_HILLS.attenuate()).setTemperatureRainfall(0.2F, 0.3F);
    public static final BiomeGenBase JUNGLE = (new BiomeGenJungle(21, false)).setColor(5470985).setBiomeName("Jungle").setFillerBlockMetadata(5470985).setTemperatureRainfall(0.95F, 0.9F);
    public static final BiomeGenBase JUNGLE_HILLS = (new BiomeGenJungle(22, false)).setColor(2900485).setBiomeName("JungleHills").setFillerBlockMetadata(5470985).setTemperatureRainfall(0.95F, 0.9F).setHeight(HEIGHT_LOW_HILLS);
    public static final BiomeGenBase JUNGLE_EDGE = (new BiomeGenJungle(23, true)).setColor(6458135).setBiomeName("JungleEdge").setFillerBlockMetadata(5470985).setTemperatureRainfall(0.95F, 0.8F);
    public static final BiomeGenBase DEEP_OCEAN = (new BiomeGenOcean(24)).setColor(48).setBiomeName("Deep Ocean").setHeight(HEIGHT_DEEP_OCEANS);
    public static final BiomeGenBase STONE_BEACH = (new BiomeGenStoneBeach(25)).setColor(10658436).setBiomeName("Stone Beach").setTemperatureRainfall(0.2F, 0.3F).setHeight(HEIGHT_ROCKY_WATERS);
    public static final BiomeGenBase COLD_BEACH = (new BiomeGenBeach(26)).setColor(16445632).setBiomeName("Cold Beach").setTemperatureRainfall(0.05F, 0.3F).setHeight(HEIGHT_SHORES).setEnableSnow();
    public static final BiomeGenBase BIRCH_FOREST = (new BiomeGenForest(27, 2)).setBiomeName("Birch Forest").setColor(3175492);
    public static final BiomeGenBase BIRCH_FOREST_HILLS = (new BiomeGenForest(28, 2)).setBiomeName("Birch Forest Hills").setColor(2055986).setHeight(HEIGHT_LOW_HILLS);
    public static final BiomeGenBase ROOFED_FOREST = (new BiomeGenForest(29, 3)).setColor(4215066).setBiomeName("Roofed Forest");
    public static final BiomeGenBase COLD_TAIGA = (new BiomeGenTaiga(30, 0)).setColor(3233098).setBiomeName("Cold Taiga").setFillerBlockMetadata(5159473).setEnableSnow().setTemperatureRainfall(-0.5F, 0.4F).setHeight(HEIGHT_MID_PLAINS).func_150563_c(16777215);
    public static final BiomeGenBase COLD_TAIGA_HILLS = (new BiomeGenTaiga(31, 0)).setColor(2375478).setBiomeName("Cold Taiga Hills").setFillerBlockMetadata(5159473).setEnableSnow().setTemperatureRainfall(-0.5F, 0.4F).setHeight(HEIGHT_LOW_HILLS).func_150563_c(16777215);
    public static final BiomeGenBase MEGA_TAIGA = (new BiomeGenTaiga(32, 1)).setColor(5858897).setBiomeName("Mega Taiga").setFillerBlockMetadata(5159473).setTemperatureRainfall(0.3F, 0.8F).setHeight(HEIGHT_MID_PLAINS);
    public static final BiomeGenBase MEGA_TAIGA_HILLS = (new BiomeGenTaiga(33, 1)).setColor(4542270).setBiomeName("Mega Taiga Hills").setFillerBlockMetadata(5159473).setTemperatureRainfall(0.3F, 0.8F).setHeight(HEIGHT_LOW_HILLS);
    public static final BiomeGenBase EXTREME_HILLS_PLUS = (new BiomeGenHills(34, true)).setColor(5271632).setBiomeName("Extreme Hills+").setHeight(HEIGHT_MID_HILLS).setTemperatureRainfall(0.2F, 0.3F);
    public static final BiomeGenBase SAVANNA = (new BiomeGenSavanna(35)).setColor(12431967).setBiomeName("Savanna").setTemperatureRainfall(1.2F, 0.0F).setDisableRain().setHeight(HEIGHT_LOW_PLAINS);
    public static final BiomeGenBase SAVANNA_PLATEAU = (new BiomeGenSavanna(36)).setColor(10984804).setBiomeName("Savanna Plateau").setTemperatureRainfall(1.0F, 0.0F).setDisableRain().setHeight(HEIGHT_HIGH_PLATEAUS);
    public static final BiomeGenBase MESA = (new BiomeGenMesa(37, false, false)).setColor(14238997).setBiomeName("Mesa");
    public static final BiomeGenBase MESA_PLATEAU_F = (new BiomeGenMesa(38, false, true)).setColor(11573093).setBiomeName("Mesa Plateau F").setHeight(HEIGHT_HIGH_PLATEAUS);
    public static final BiomeGenBase MESA_PLATEAU = (new BiomeGenMesa(39, false, false)).setColor(13274213).setBiomeName("Mesa Plateau").setHeight(HEIGHT_HIGH_PLATEAUS);
    public static final BiomeGenBase OCEAN2 = OCEAN;
    protected static final NoiseGeneratorPerlin TEMPERATURE_NOISE;
    protected static final NoiseGeneratorPerlin GRASS_COLOR_NOISE;
    protected static final WorldGenDoublePlant DOUBLE_PLANT_GENERATOR;
    public String biomeName;
    public int color;
    public int field_150609_ah;
    public IBlockState topBlock = Blocks.GRASS.getDefaultState();
    public IBlockState fillerBlock = Blocks.DIRT.getDefaultState();
    public int fillerBlockMetadata = 5169201;
    public float minHeight;
    public float maxHeight;
    public float temperature;
    public float rainfall;
    public int waterColorMultiplier;
    public BiomeDecorator theBiomeDecorator;
    protected List<SpawnListEntry> spawnableMonsterList;
    protected List<SpawnListEntry> spawnableCreatureList;
    protected List<SpawnListEntry> spawnableWaterCreatureList;
    protected List<SpawnListEntry> spawnableCaveCreatureList;
    protected boolean enableSnow;
    protected boolean enableRain;
    public final int biomeID;
    protected WorldGenTrees worldGeneratorTrees;
    protected WorldGenBigTree worldGeneratorBigTree;
    protected WorldGenSwamp worldGeneratorSwamp;

    protected BiomeGenBase(int id) {
        this.minHeight = HEIGHT_DEFAULT.rootHeight;
        this.maxHeight = HEIGHT_DEFAULT.variation;
        this.temperature = 0.5F;
        this.rainfall = 0.5F;
        this.waterColorMultiplier = 16777215;
        this.spawnableMonsterList = Lists.newArrayList();
        this.spawnableCreatureList = Lists.newArrayList();
        this.spawnableWaterCreatureList = Lists.newArrayList();
        this.spawnableCaveCreatureList = Lists.newArrayList();
        this.enableRain = true;
        this.worldGeneratorTrees = new WorldGenTrees(false);
        this.worldGeneratorBigTree = new WorldGenBigTree(false);
        this.worldGeneratorSwamp = new WorldGenSwamp();
        this.biomeID = id;
        BIOME_LIST[id] = this;
        this.theBiomeDecorator = this.createBiomeDecorator();
        this.spawnableCreatureList.add(new SpawnListEntry(EntitySheep.class, 12, 4, 4));
        this.spawnableCreatureList.add(new SpawnListEntry(EntityRabbit.class, 10, 3, 3));
        this.spawnableCreatureList.add(new SpawnListEntry(EntityPig.class, 10, 4, 4));
        this.spawnableCreatureList.add(new SpawnListEntry(EntityChicken.class, 10, 4, 4));
        this.spawnableCreatureList.add(new SpawnListEntry(EntityCow.class, 8, 4, 4));
        this.spawnableMonsterList.add(new SpawnListEntry(EntitySpider.class, 100, 4, 4));
        this.spawnableMonsterList.add(new SpawnListEntry(EntityZombie.class, 100, 4, 4));
        this.spawnableMonsterList.add(new SpawnListEntry(EntitySkeleton.class, 100, 4, 4));
        this.spawnableMonsterList.add(new SpawnListEntry(EntityCreeper.class, 100, 4, 4));
        this.spawnableMonsterList.add(new SpawnListEntry(EntitySlime.class, 100, 4, 4));
        this.spawnableMonsterList.add(new SpawnListEntry(EntityEnderman.class, 10, 1, 4));
        this.spawnableMonsterList.add(new SpawnListEntry(EntityWitch.class, 5, 1, 1));
        this.spawnableWaterCreatureList.add(new SpawnListEntry(EntitySquid.class, 10, 4, 4));
        this.spawnableCaveCreatureList.add(new SpawnListEntry(EntityBat.class, 10, 8, 8));
    }

    protected BiomeDecorator createBiomeDecorator() {
        return new BiomeDecorator();
    }

    protected BiomeGenBase setTemperatureRainfall(float temperatureIn, float rainfallIn) {
        if (temperatureIn > 0.1F && temperatureIn < 0.2F) {
            throw new IllegalArgumentException("Please avoid temperatures in the range 0.1 - 0.2 because of snow");
        } else {
            this.temperature = temperatureIn;
            this.rainfall = rainfallIn;
            return this;
        }
    }

    protected final BiomeGenBase setHeight(Height heights) {
        this.minHeight = heights.rootHeight;
        this.maxHeight = heights.variation;
        return this;
    }

    protected BiomeGenBase setDisableRain() {
        this.enableRain = false;
        return this;
    }

    public WorldGenAbstractTree genBigTreeChance(Random rand) {
        return rand.nextInt(10) == 0 ? this.worldGeneratorBigTree : this.worldGeneratorTrees;
    }

    public WorldGenerator getRandomWorldGenForGrass(Random rand) {
        return new WorldGenTallGrass(BlockTallGrass.EnumType.GRASS);
    }

    public BlockFlower.FlowerType pickRandomFlower(Random rand, BlockPos pos) {
        return rand.nextInt(3) > 0 ? BlockFlower.FlowerType.DANDELION : BlockFlower.FlowerType.POPPY;
    }

    protected BiomeGenBase setEnableSnow() {
        this.enableSnow = true;
        return this;
    }

    protected BiomeGenBase setBiomeName(String name) {
        this.biomeName = name;
        return this;
    }

    protected BiomeGenBase setFillerBlockMetadata(int meta) {
        this.fillerBlockMetadata = meta;
        return this;
    }

    protected BiomeGenBase setColor(int colorIn) {
        this.func_150557_a(colorIn, false);
        return this;
    }

    protected BiomeGenBase func_150563_c(int p_150563_1_) {
        this.field_150609_ah = p_150563_1_;
        return this;
    }

    protected BiomeGenBase func_150557_a(int colorIn, boolean p_150557_2_) {
        this.color = colorIn;

        if (p_150557_2_) {
            this.field_150609_ah = (colorIn & 16711422) >> 1;
        } else {
            this.field_150609_ah = colorIn;
        }

        return this;
    }

    public int getSkyColorByTemp(float p_76731_1_) {
        p_76731_1_ = p_76731_1_ / 3.0F;
        p_76731_1_ = MathHelper.clamp_float(p_76731_1_, -1.0F, 1.0F);
        return MathHelper.hsvToRGB(0.62222224F - p_76731_1_ * 0.05F, 0.5F + p_76731_1_ * 0.1F, 1.0F);
    }

    public List<SpawnListEntry> getSpawnableList(EntityCategory creatureType) {
        return switch (creatureType) {
            case MONSTER -> this.spawnableMonsterList;
            case CREATURE -> this.spawnableCreatureList;
            case WATER_CREATURE -> this.spawnableWaterCreatureList;
            case AMBIENT -> this.spawnableCaveCreatureList;
            default -> Collections.emptyList();
        };
    }

    public boolean getEnableSnow() {
        return this.isSnowyBiome();
    }

    public boolean canRain() {
        return !this.isSnowyBiome() && this.enableRain;
    }

    public boolean isHighHumidity() {
        return this.rainfall > 0.85F;
    }

    public float getSpawningChance() {
        return 0.1F;
    }

    public final int getIntRainfall() {
        return (int) (this.rainfall * 65536.0F);
    }

    public final float getFloatRainfall() {
        return this.rainfall;
    }

    public final float getFloatTemperature(BlockPos pos) {
        if (pos.getY() > 64) {
            float f = (float) (TEMPERATURE_NOISE.func_151601_a(pos.getX() * 1.0D / 8.0D, pos.getZ() * 1.0D / 8.0D) * 4.0D);
            return this.temperature - (f + pos.getY() - 64.0F) * 0.05F / 30.0F;
        } else {
            return this.temperature;
        }
    }

    public void decorate(World worldIn, Random rand, BlockPos pos) {
        this.theBiomeDecorator.decorate(worldIn, rand, this, pos);
    }

    public int getGrassColorAtPos(BlockPos pos) {
        double d0 = MathHelper.clamp_float(this.getFloatTemperature(pos), 0.0F, 1.0F);
        double d1 = MathHelper.clamp_float(this.getFloatRainfall(), 0.0F, 1.0F);
        return ColorizerGrass.getGrassColor(d0, d1);
    }

    public int getFoliageColorAtPos(BlockPos pos) {
        double d0 = MathHelper.clamp_float(this.getFloatTemperature(pos), 0.0F, 1.0F);
        double d1 = MathHelper.clamp_float(this.getFloatRainfall(), 0.0F, 1.0F);
        return ColorizerFoliage.getFoliageColor(d0, d1);
    }

    public boolean isSnowyBiome() {
        return this.enableSnow;
    }

    public void genTerrainBlocks(World worldIn, Random rand, ChunkPrimer chunkPrimerIn, int x, int z, double noiseVal) {
        this.generateBiomeTerrain(worldIn, rand, chunkPrimerIn, x, z, noiseVal);
    }

    public final void generateBiomeTerrain(World worldIn, Random rand, ChunkPrimer chunkPrimerIn, int x, int z, double noiseVal) {
        int i = worldIn.getSeaLevel();
        IBlockState iblockstate = this.topBlock;
        IBlockState iblockstate1 = this.fillerBlock;
        int j = -1;
        int k = (int) (noiseVal / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int l = x & 15;
        int i1 = z & 15;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int j1 = 255; j1 >= 0; --j1) {
            if (j1 <= rand.nextInt(5)) {
                chunkPrimerIn.setBlockState(i1, j1, l, Blocks.BEDROCK.getDefaultState());
            } else {
                IBlockState iblockstate2 = chunkPrimerIn.getBlockState(i1, j1, l);

                if (iblockstate2.getBlock().getMaterial() == Material.AIR) {
                    j = -1;
                } else if (iblockstate2.getBlock() == Blocks.STONE) {
                    if (j == -1) {
                        if (k <= 0) {
                            iblockstate = null;
                            iblockstate1 = Blocks.STONE.getDefaultState();
                        } else if (j1 >= i - 4 && j1 <= i + 1) {
                            iblockstate = this.topBlock;
                            iblockstate1 = this.fillerBlock;
                        }

                        if (j1 < i && (iblockstate == null || iblockstate.getBlock().getMaterial() == Material.AIR)) {
                            if (this.getFloatTemperature(blockpos$mutableblockpos.set(x, j1, z)) < 0.15F) {
                                iblockstate = Blocks.ICE.getDefaultState();
                            } else {
                                iblockstate = Blocks.WATER.getDefaultState();
                            }
                        }

                        j = k;

                        if (j1 >= i - 1) {
                            chunkPrimerIn.setBlockState(i1, j1, l, iblockstate);
                        } else if (j1 < i - 7 - k) {
                            iblockstate = null;
                            iblockstate1 = Blocks.STONE.getDefaultState();
                            chunkPrimerIn.setBlockState(i1, j1, l, Blocks.GRAVEL.getDefaultState());
                        } else {
                            chunkPrimerIn.setBlockState(i1, j1, l, iblockstate1);
                        }
                    } else if (j > 0) {
                        --j;
                        chunkPrimerIn.setBlockState(i1, j1, l, iblockstate1);

                        if (j == 0 && iblockstate1.getBlock() == Blocks.SAND) {
                            j = rand.nextInt(4) + Math.max(0, j1 - 63);
                            iblockstate1 = iblockstate1.getValue(BlockSand.VARIANT) == BlockSand.SandType.RED_SAND ? Blocks.RED_SANDSTONE.getDefaultState() : Blocks.SANDSTONE.getDefaultState();
                        }
                    }
                }
            }
        }
    }

    protected BiomeGenBase createMutation() {
        return this.createMutatedBiome(this.biomeID + 128);
    }

    protected BiomeGenBase createMutatedBiome(int p_180277_1_) {
        return new BiomeGenMutated(p_180277_1_, this);
    }

    public Class<? extends BiomeGenBase> getBiomeClass() {
        return this.getClass();
    }

    public boolean isEqualTo(BiomeGenBase biome) {
        return biome == this || (biome != null && this.getBiomeClass() == biome.getBiomeClass());
    }

    public TempCategory getTempCategory() {
        return this.temperature < 0.2D ? TempCategory.COLD : (this.temperature < 1.0D ? TempCategory.MEDIUM : TempCategory.WARM);
    }

    public static BiomeGenBase[] getBiomeGenArray() {
        return BIOME_LIST;
    }

    public static BiomeGenBase getBiome(int id) {
        return getBiomeFromBiomeList(id, null);
    }

    public static BiomeGenBase getBiomeFromBiomeList(int biomeId, BiomeGenBase biome) {
        if (biomeId >= 0 && biomeId <= BIOME_LIST.length) {
            BiomeGenBase biomegenbase = BIOME_LIST[biomeId];
            return biomegenbase == null ? biome : biomegenbase;
        } else {
            LOGGER.warn("Biome ID is out of bounds: {}, defaulting to 0 (Ocean)", biomeId);
            return OCEAN;
        }
    }

    static {
        PLAINS.createMutation();
        DESERT.createMutation();
        FOREST.createMutation();
        TAIGA.createMutation();
        SWAMPLAND.createMutation();
        ICE_PLAINS.createMutation();
        JUNGLE.createMutation();
        JUNGLE_EDGE.createMutation();
        COLD_TAIGA.createMutation();
        SAVANNA.createMutation();
        SAVANNA_PLATEAU.createMutation();
        MESA.createMutation();
        MESA_PLATEAU_F.createMutation();
        MESA_PLATEAU.createMutation();
        BIRCH_FOREST.createMutation();
        BIRCH_FOREST_HILLS.createMutation();
        ROOFED_FOREST.createMutation();
        MEGA_TAIGA.createMutation();
        EXTREME_HILLS.createMutation();
        EXTREME_HILLS_PLUS.createMutation();
        MEGA_TAIGA.createMutatedBiome(MEGA_TAIGA_HILLS.biomeID + 128).setBiomeName("Redwood Taiga Hills M");

        for (BiomeGenBase biomegenbase : BIOME_LIST) {
            if (biomegenbase != null) {
                if (BIOME_ID_MAP.containsKey(biomegenbase.biomeName)) {
                    throw new Error("Biome \"" + biomegenbase.biomeName + "\" is defined as both ID " + BIOME_ID_MAP.get(biomegenbase.biomeName).biomeID + " and " + biomegenbase.biomeID);
                }

                BIOME_ID_MAP.put(biomegenbase.biomeName, biomegenbase);

                if (biomegenbase.biomeID < 128) {
                    EXPLORATION_BIOMES_LIST.add(biomegenbase);
                }
            }
        }

        EXPLORATION_BIOMES_LIST.remove(HELL);
        EXPLORATION_BIOMES_LIST.remove(SKY);
        EXPLORATION_BIOMES_LIST.remove(FROZEN_OCEAN);
        EXPLORATION_BIOMES_LIST.remove(EXTREME_HILLS_EDGE);
        TEMPERATURE_NOISE = new NoiseGeneratorPerlin(new Random(1234L), 1);
        GRASS_COLOR_NOISE = new NoiseGeneratorPerlin(new Random(2345L), 1);
        DOUBLE_PLANT_GENERATOR = new WorldGenDoublePlant();
    }

    public static class Height {
        public final float rootHeight;
        public final float variation;

        public Height(float rootHeightIn, float variationIn) {
            this.rootHeight = rootHeightIn;
            this.variation = variationIn;
        }

        public Height attenuate() {
            return new Height(this.rootHeight * 0.8F, this.variation * 0.6F);
        }
    }

    public static class SpawnListEntry extends WeightedRandom.Item {
        public final Class<? extends EntityLiving> entityClass;
        public final int minGroupCount;
        public final int maxGroupCount;

        public SpawnListEntry(Class<? extends EntityLiving> entityclassIn, int weight, int groupCountMin, int groupCountMax) {
            super(weight);
            this.entityClass = entityclassIn;
            this.minGroupCount = groupCountMin;
            this.maxGroupCount = groupCountMax;
        }

        public String toString() {
            return this.entityClass.getSimpleName() + "*(" + this.minGroupCount + "-" + this.maxGroupCount + "):" + this.itemWeight;
        }
    }

    public enum TempCategory {
        OCEAN,
        COLD,
        MEDIUM,
        WARM
    }
}
