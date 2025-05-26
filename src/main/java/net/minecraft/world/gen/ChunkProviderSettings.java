package net.minecraft.world.gen;

import com.google.gson.*;
import net.minecraft.util.JsonUtils;
import net.minecraft.world.biome.BiomeGenBase;

import java.lang.reflect.Type;

public class ChunkProviderSettings {
    public final float coordinateScale;
    public final float heightScale;
    public final float upperLimitScale;
    public final float lowerLimitScale;
    public final float depthNoiseScaleX;
    public final float depthNoiseScaleZ;
    public final float depthNoiseScaleExponent;
    public final float mainNoiseScaleX;
    public final float mainNoiseScaleY;
    public final float mainNoiseScaleZ;
    public final float baseSize;
    public final float stretchY;
    public final float biomeDepthWeight;
    public final float biomeDepthOffSet;
    public final float biomeScaleWeight;
    public final float biomeScaleOffset;
    public final int seaLevel;
    public final boolean useCaves;
    public final boolean useDungeons;
    public final int dungeonChance;
    public final boolean useStrongholds;
    public final boolean useVillages;
    public final boolean useMineShafts;
    public final boolean useTemples;
    public final boolean useMonuments;
    public final boolean useRavines;
    public final boolean useWaterLakes;
    public final int waterLakeChance;
    public final boolean useLavaLakes;
    public final int lavaLakeChance;
    public final boolean useLavaOceans;
    public final int fixedBiome;
    public final int biomeSize;
    public final int riverSize;
    public final int dirtSize;
    public final int dirtCount;
    public final int dirtMinHeight;
    public final int dirtMaxHeight;
    public final int gravelSize;
    public final int gravelCount;
    public final int gravelMinHeight;
    public final int gravelMaxHeight;
    public final int graniteSize;
    public final int graniteCount;
    public final int graniteMinHeight;
    public final int graniteMaxHeight;
    public final int dioriteSize;
    public final int dioriteCount;
    public final int dioriteMinHeight;
    public final int dioriteMaxHeight;
    public final int andesiteSize;
    public final int andesiteCount;
    public final int andesiteMinHeight;
    public final int andesiteMaxHeight;
    public final int coalSize;
    public final int coalCount;
    public final int coalMinHeight;
    public final int coalMaxHeight;
    public final int ironSize;
    public final int ironCount;
    public final int ironMinHeight;
    public final int ironMaxHeight;
    public final int goldSize;
    public final int goldCount;
    public final int goldMinHeight;
    public final int goldMaxHeight;
    public final int redstoneSize;
    public final int redstoneCount;
    public final int redstoneMinHeight;
    public final int redstoneMaxHeight;
    public final int diamondSize;
    public final int diamondCount;
    public final int diamondMinHeight;
    public final int diamondMaxHeight;
    public final int lapisSize;
    public final int lapisCount;
    public final int lapisCenterHeight;
    public final int lapisSpread;

    private ChunkProviderSettings(Factory settingsFactory) {
        this.coordinateScale = settingsFactory.coordinateScale;
        this.heightScale = settingsFactory.heightScale;
        this.upperLimitScale = settingsFactory.upperLimitScale;
        this.lowerLimitScale = settingsFactory.lowerLimitScale;
        this.depthNoiseScaleX = settingsFactory.depthNoiseScaleX;
        this.depthNoiseScaleZ = settingsFactory.depthNoiseScaleZ;
        this.depthNoiseScaleExponent = settingsFactory.depthNoiseScaleExponent;
        this.mainNoiseScaleX = settingsFactory.mainNoiseScaleX;
        this.mainNoiseScaleY = settingsFactory.mainNoiseScaleY;
        this.mainNoiseScaleZ = settingsFactory.mainNoiseScaleZ;
        this.baseSize = settingsFactory.baseSize;
        this.stretchY = settingsFactory.stretchY;
        this.biomeDepthWeight = settingsFactory.biomeDepthWeight;
        this.biomeDepthOffSet = settingsFactory.biomeDepthOffset;
        this.biomeScaleWeight = settingsFactory.biomeScaleWeight;
        this.biomeScaleOffset = settingsFactory.biomeScaleOffset;
        this.seaLevel = settingsFactory.seaLevel;
        this.useCaves = settingsFactory.useCaves;
        this.useDungeons = settingsFactory.useDungeons;
        this.dungeonChance = settingsFactory.dungeonChance;
        this.useStrongholds = settingsFactory.useStrongholds;
        this.useVillages = settingsFactory.useVillages;
        this.useMineShafts = settingsFactory.useMineShafts;
        this.useTemples = settingsFactory.useTemples;
        this.useMonuments = settingsFactory.useMonuments;
        this.useRavines = settingsFactory.useRavines;
        this.useWaterLakes = settingsFactory.useWaterLakes;
        this.waterLakeChance = settingsFactory.waterLakeChance;
        this.useLavaLakes = settingsFactory.useLavaLakes;
        this.lavaLakeChance = settingsFactory.lavaLakeChance;
        this.useLavaOceans = settingsFactory.useLavaOceans;
        this.fixedBiome = settingsFactory.fixedBiome;
        this.biomeSize = settingsFactory.biomeSize;
        this.riverSize = settingsFactory.riverSize;
        this.dirtSize = settingsFactory.dirtSize;
        this.dirtCount = settingsFactory.dirtCount;
        this.dirtMinHeight = settingsFactory.dirtMinHeight;
        this.dirtMaxHeight = settingsFactory.dirtMaxHeight;
        this.gravelSize = settingsFactory.gravelSize;
        this.gravelCount = settingsFactory.gravelCount;
        this.gravelMinHeight = settingsFactory.gravelMinHeight;
        this.gravelMaxHeight = settingsFactory.gravelMaxHeight;
        this.graniteSize = settingsFactory.graniteSize;
        this.graniteCount = settingsFactory.graniteCount;
        this.graniteMinHeight = settingsFactory.graniteMinHeight;
        this.graniteMaxHeight = settingsFactory.graniteMaxHeight;
        this.dioriteSize = settingsFactory.dioriteSize;
        this.dioriteCount = settingsFactory.dioriteCount;
        this.dioriteMinHeight = settingsFactory.dioriteMinHeight;
        this.dioriteMaxHeight = settingsFactory.dioriteMaxHeight;
        this.andesiteSize = settingsFactory.andesiteSize;
        this.andesiteCount = settingsFactory.andesiteCount;
        this.andesiteMinHeight = settingsFactory.andesiteMinHeight;
        this.andesiteMaxHeight = settingsFactory.andesiteMaxHeight;
        this.coalSize = settingsFactory.coalSize;
        this.coalCount = settingsFactory.coalCount;
        this.coalMinHeight = settingsFactory.coalMinHeight;
        this.coalMaxHeight = settingsFactory.coalMaxHeight;
        this.ironSize = settingsFactory.ironSize;
        this.ironCount = settingsFactory.ironCount;
        this.ironMinHeight = settingsFactory.ironMinHeight;
        this.ironMaxHeight = settingsFactory.ironMaxHeight;
        this.goldSize = settingsFactory.goldSize;
        this.goldCount = settingsFactory.goldCount;
        this.goldMinHeight = settingsFactory.goldMinHeight;
        this.goldMaxHeight = settingsFactory.goldMaxHeight;
        this.redstoneSize = settingsFactory.redstoneSize;
        this.redstoneCount = settingsFactory.redstoneCount;
        this.redstoneMinHeight = settingsFactory.redstoneMinHeight;
        this.redstoneMaxHeight = settingsFactory.redstoneMaxHeight;
        this.diamondSize = settingsFactory.diamondSize;
        this.diamondCount = settingsFactory.diamondCount;
        this.diamondMinHeight = settingsFactory.diamondMinHeight;
        this.diamondMaxHeight = settingsFactory.diamondMaxHeight;
        this.lapisSize = settingsFactory.lapisSize;
        this.lapisCount = settingsFactory.lapisCount;
        this.lapisCenterHeight = settingsFactory.lapisCenterHeight;
        this.lapisSpread = settingsFactory.lapisSpread;
    }

    public static class Factory {
        static final Gson JSON_ADAPTER = new GsonBuilder()
                .registerTypeAdapter(Factory.class, new Serializer())
                .create();
        public float coordinateScale = 684.412F;
        public float heightScale = 684.412F;
        public float upperLimitScale = 512.0F;
        public float lowerLimitScale = 512.0F;
        public float depthNoiseScaleX = 200.0F;
        public float depthNoiseScaleZ = 200.0F;
        public float depthNoiseScaleExponent = 0.5F;
        public float mainNoiseScaleX = 80.0F;
        public float mainNoiseScaleY = 160.0F;
        public float mainNoiseScaleZ = 80.0F;
        public float baseSize = 8.5F;
        public float stretchY = 12.0F;
        public float biomeDepthWeight = 1.0F;
        public float biomeDepthOffset = 0.0F;
        public float biomeScaleWeight = 1.0F;
        public float biomeScaleOffset = 0.0F;
        public int seaLevel = 63;
        public boolean useCaves = true;
        public boolean useDungeons = true;
        public int dungeonChance = 8;
        public boolean useStrongholds = true;
        public boolean useVillages = true;
        public boolean useMineShafts = true;
        public boolean useTemples = true;
        public boolean useMonuments = true;
        public boolean useRavines = true;
        public boolean useWaterLakes = true;
        public int waterLakeChance = 4;
        public boolean useLavaLakes = true;
        public int lavaLakeChance = 80;
        public boolean useLavaOceans = false;
        public int fixedBiome = -1;
        public int biomeSize = 4;
        public int riverSize = 4;
        public int dirtSize = 33;
        public int dirtCount = 10;
        public int dirtMinHeight = 0;
        public int dirtMaxHeight = 256;
        public int gravelSize = 33;
        public int gravelCount = 8;
        public int gravelMinHeight = 0;
        public int gravelMaxHeight = 256;
        public int graniteSize = 33;
        public int graniteCount = 10;
        public int graniteMinHeight = 0;
        public int graniteMaxHeight = 80;
        public int dioriteSize = 33;
        public int dioriteCount = 10;
        public int dioriteMinHeight = 0;
        public int dioriteMaxHeight = 80;
        public int andesiteSize = 33;
        public int andesiteCount = 10;
        public int andesiteMinHeight = 0;
        public int andesiteMaxHeight = 80;
        public int coalSize = 17;
        public int coalCount = 20;
        public int coalMinHeight = 0;
        public int coalMaxHeight = 128;
        public int ironSize = 9;
        public int ironCount = 20;
        public int ironMinHeight = 0;
        public int ironMaxHeight = 64;
        public int goldSize = 9;
        public int goldCount = 2;
        public int goldMinHeight = 0;
        public int goldMaxHeight = 32;
        public int redstoneSize = 8;
        public int redstoneCount = 8;
        public int redstoneMinHeight = 0;
        public int redstoneMaxHeight = 16;
        public int diamondSize = 8;
        public int diamondCount = 1;
        public int diamondMinHeight = 0;
        public int diamondMaxHeight = 16;
        public int lapisSize = 7;
        public int lapisCount = 1;
        public int lapisCenterHeight = 16;
        public int lapisSpread = 16;

        public static Factory jsonToFactory(String p_177865_0_) {
            if (p_177865_0_.isEmpty()) {
                return new Factory();
            } else {
                try {
                    return JSON_ADAPTER.fromJson(p_177865_0_, Factory.class);
                } catch (Exception exception) {
                    return new Factory();
                }
            }
        }

        public String toString() {
            return JSON_ADAPTER.toJson(this);
        }

        public Factory() {
            this.func_177863_a();
        }

        public void func_177863_a() {
            this.coordinateScale = 684.412F;
            this.heightScale = 684.412F;
            this.upperLimitScale = 512.0F;
            this.lowerLimitScale = 512.0F;
            this.depthNoiseScaleX = 200.0F;
            this.depthNoiseScaleZ = 200.0F;
            this.depthNoiseScaleExponent = 0.5F;
            this.mainNoiseScaleX = 80.0F;
            this.mainNoiseScaleY = 160.0F;
            this.mainNoiseScaleZ = 80.0F;
            this.baseSize = 8.5F;
            this.stretchY = 12.0F;
            this.biomeDepthWeight = 1.0F;
            this.biomeDepthOffset = 0.0F;
            this.biomeScaleWeight = 1.0F;
            this.biomeScaleOffset = 0.0F;
            this.seaLevel = 63;
            this.useCaves = true;
            this.useDungeons = true;
            this.dungeonChance = 8;
            this.useStrongholds = true;
            this.useVillages = true;
            this.useMineShafts = true;
            this.useTemples = true;
            this.useMonuments = true;
            this.useRavines = true;
            this.useWaterLakes = true;
            this.waterLakeChance = 4;
            this.useLavaLakes = true;
            this.lavaLakeChance = 80;
            this.useLavaOceans = false;
            this.fixedBiome = -1;
            this.biomeSize = 4;
            this.riverSize = 4;
            this.dirtSize = 33;
            this.dirtCount = 10;
            this.dirtMinHeight = 0;
            this.dirtMaxHeight = 256;
            this.gravelSize = 33;
            this.gravelCount = 8;
            this.gravelMinHeight = 0;
            this.gravelMaxHeight = 256;
            this.graniteSize = 33;
            this.graniteCount = 10;
            this.graniteMinHeight = 0;
            this.graniteMaxHeight = 80;
            this.dioriteSize = 33;
            this.dioriteCount = 10;
            this.dioriteMinHeight = 0;
            this.dioriteMaxHeight = 80;
            this.andesiteSize = 33;
            this.andesiteCount = 10;
            this.andesiteMinHeight = 0;
            this.andesiteMaxHeight = 80;
            this.coalSize = 17;
            this.coalCount = 20;
            this.coalMinHeight = 0;
            this.coalMaxHeight = 128;
            this.ironSize = 9;
            this.ironCount = 20;
            this.ironMinHeight = 0;
            this.ironMaxHeight = 64;
            this.goldSize = 9;
            this.goldCount = 2;
            this.goldMinHeight = 0;
            this.goldMaxHeight = 32;
            this.redstoneSize = 8;
            this.redstoneCount = 8;
            this.redstoneMinHeight = 0;
            this.redstoneMaxHeight = 16;
            this.diamondSize = 8;
            this.diamondCount = 1;
            this.diamondMinHeight = 0;
            this.diamondMaxHeight = 16;
            this.lapisSize = 7;
            this.lapisCount = 1;
            this.lapisCenterHeight = 16;
            this.lapisSpread = 16;
        }

        public boolean equals(Object p_equals_1_) {
            if (this == p_equals_1_) {
                return true;
            } else if (p_equals_1_ != null && this.getClass() == p_equals_1_.getClass()) {
                Factory chunkprovidersettings$factory = (Factory) p_equals_1_;
                return this.andesiteCount == chunkprovidersettings$factory.andesiteCount && (this.andesiteMaxHeight == chunkprovidersettings$factory.andesiteMaxHeight && (this.andesiteMinHeight == chunkprovidersettings$factory.andesiteMinHeight && (this.andesiteSize == chunkprovidersettings$factory.andesiteSize && (Float.compare(chunkprovidersettings$factory.baseSize, this.baseSize) == 0 && (Float.compare(chunkprovidersettings$factory.biomeDepthOffset, this.biomeDepthOffset) == 0 && (Float.compare(chunkprovidersettings$factory.biomeDepthWeight, this.biomeDepthWeight) == 0 && (Float.compare(chunkprovidersettings$factory.biomeScaleOffset, this.biomeScaleOffset) == 0 && (Float.compare(chunkprovidersettings$factory.biomeScaleWeight, this.biomeScaleWeight) == 0 && (this.biomeSize == chunkprovidersettings$factory.biomeSize && (this.coalCount == chunkprovidersettings$factory.coalCount && (this.coalMaxHeight == chunkprovidersettings$factory.coalMaxHeight && (this.coalMinHeight == chunkprovidersettings$factory.coalMinHeight && (this.coalSize == chunkprovidersettings$factory.coalSize && (Float.compare(chunkprovidersettings$factory.coordinateScale, this.coordinateScale) == 0 && (Float.compare(chunkprovidersettings$factory.depthNoiseScaleExponent, this.depthNoiseScaleExponent) == 0 && (Float.compare(chunkprovidersettings$factory.depthNoiseScaleX, this.depthNoiseScaleX) == 0 && (Float.compare(chunkprovidersettings$factory.depthNoiseScaleZ, this.depthNoiseScaleZ) == 0 && (this.diamondCount == chunkprovidersettings$factory.diamondCount && (this.diamondMaxHeight == chunkprovidersettings$factory.diamondMaxHeight && (this.diamondMinHeight == chunkprovidersettings$factory.diamondMinHeight && (this.diamondSize == chunkprovidersettings$factory.diamondSize && (this.dioriteCount == chunkprovidersettings$factory.dioriteCount && (this.dioriteMaxHeight == chunkprovidersettings$factory.dioriteMaxHeight && (this.dioriteMinHeight == chunkprovidersettings$factory.dioriteMinHeight && (this.dioriteSize == chunkprovidersettings$factory.dioriteSize && (this.dirtCount == chunkprovidersettings$factory.dirtCount && (this.dirtMaxHeight == chunkprovidersettings$factory.dirtMaxHeight && (this.dirtMinHeight == chunkprovidersettings$factory.dirtMinHeight && (this.dirtSize == chunkprovidersettings$factory.dirtSize && (this.dungeonChance == chunkprovidersettings$factory.dungeonChance && (this.fixedBiome == chunkprovidersettings$factory.fixedBiome && (this.goldCount == chunkprovidersettings$factory.goldCount && (this.goldMaxHeight == chunkprovidersettings$factory.goldMaxHeight && (this.goldMinHeight == chunkprovidersettings$factory.goldMinHeight && (this.goldSize == chunkprovidersettings$factory.goldSize && (this.graniteCount == chunkprovidersettings$factory.graniteCount && (this.graniteMaxHeight == chunkprovidersettings$factory.graniteMaxHeight && (this.graniteMinHeight == chunkprovidersettings$factory.graniteMinHeight && (this.graniteSize == chunkprovidersettings$factory.graniteSize && (this.gravelCount == chunkprovidersettings$factory.gravelCount && (this.gravelMaxHeight == chunkprovidersettings$factory.gravelMaxHeight && (this.gravelMinHeight == chunkprovidersettings$factory.gravelMinHeight && (this.gravelSize == chunkprovidersettings$factory.gravelSize && (Float.compare(chunkprovidersettings$factory.heightScale, this.heightScale) == 0 && (this.ironCount == chunkprovidersettings$factory.ironCount && (this.ironMaxHeight == chunkprovidersettings$factory.ironMaxHeight && (this.ironMinHeight == chunkprovidersettings$factory.ironMinHeight && (this.ironSize == chunkprovidersettings$factory.ironSize && (this.lapisCenterHeight == chunkprovidersettings$factory.lapisCenterHeight && (this.lapisCount == chunkprovidersettings$factory.lapisCount && (this.lapisSize == chunkprovidersettings$factory.lapisSize && (this.lapisSpread == chunkprovidersettings$factory.lapisSpread && (this.lavaLakeChance == chunkprovidersettings$factory.lavaLakeChance && (Float.compare(chunkprovidersettings$factory.lowerLimitScale, this.lowerLimitScale) == 0 && (Float.compare(chunkprovidersettings$factory.mainNoiseScaleX, this.mainNoiseScaleX) == 0 && (Float.compare(chunkprovidersettings$factory.mainNoiseScaleY, this.mainNoiseScaleY) == 0 && (Float.compare(chunkprovidersettings$factory.mainNoiseScaleZ, this.mainNoiseScaleZ) == 0 && (this.redstoneCount == chunkprovidersettings$factory.redstoneCount && (this.redstoneMaxHeight == chunkprovidersettings$factory.redstoneMaxHeight && (this.redstoneMinHeight == chunkprovidersettings$factory.redstoneMinHeight && (this.redstoneSize == chunkprovidersettings$factory.redstoneSize && (this.riverSize == chunkprovidersettings$factory.riverSize && (this.seaLevel == chunkprovidersettings$factory.seaLevel && (Float.compare(chunkprovidersettings$factory.stretchY, this.stretchY) == 0 && (Float.compare(chunkprovidersettings$factory.upperLimitScale, this.upperLimitScale) == 0 && (this.useCaves == chunkprovidersettings$factory.useCaves && (this.useDungeons == chunkprovidersettings$factory.useDungeons && (this.useLavaLakes == chunkprovidersettings$factory.useLavaLakes && (this.useLavaOceans == chunkprovidersettings$factory.useLavaOceans && (this.useMineShafts == chunkprovidersettings$factory.useMineShafts && (this.useRavines == chunkprovidersettings$factory.useRavines && (this.useStrongholds == chunkprovidersettings$factory.useStrongholds && (this.useTemples == chunkprovidersettings$factory.useTemples && (this.useMonuments == chunkprovidersettings$factory.useMonuments && (this.useVillages == chunkprovidersettings$factory.useVillages && (this.useWaterLakes == chunkprovidersettings$factory.useWaterLakes && this.waterLakeChance == chunkprovidersettings$factory.waterLakeChance))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))));
            } else {
                return false;
            }
        }

        public int hashCode() {
            int i = this.coordinateScale != 0.0F ? Float.floatToIntBits(this.coordinateScale) : 0;
            i = 31 * i + (this.heightScale != 0.0F ? Float.floatToIntBits(this.heightScale) : 0);
            i = 31 * i + (this.upperLimitScale != 0.0F ? Float.floatToIntBits(this.upperLimitScale) : 0);
            i = 31 * i + (this.lowerLimitScale != 0.0F ? Float.floatToIntBits(this.lowerLimitScale) : 0);
            i = 31 * i + (this.depthNoiseScaleX != 0.0F ? Float.floatToIntBits(this.depthNoiseScaleX) : 0);
            i = 31 * i + (this.depthNoiseScaleZ != 0.0F ? Float.floatToIntBits(this.depthNoiseScaleZ) : 0);
            i = 31 * i + (this.depthNoiseScaleExponent != 0.0F ? Float.floatToIntBits(this.depthNoiseScaleExponent) : 0);
            i = 31 * i + (this.mainNoiseScaleX != 0.0F ? Float.floatToIntBits(this.mainNoiseScaleX) : 0);
            i = 31 * i + (this.mainNoiseScaleY != 0.0F ? Float.floatToIntBits(this.mainNoiseScaleY) : 0);
            i = 31 * i + (this.mainNoiseScaleZ != 0.0F ? Float.floatToIntBits(this.mainNoiseScaleZ) : 0);
            i = 31 * i + (this.baseSize != 0.0F ? Float.floatToIntBits(this.baseSize) : 0);
            i = 31 * i + (this.stretchY != 0.0F ? Float.floatToIntBits(this.stretchY) : 0);
            i = 31 * i + (this.biomeDepthWeight != 0.0F ? Float.floatToIntBits(this.biomeDepthWeight) : 0);
            i = 31 * i + (this.biomeDepthOffset != 0.0F ? Float.floatToIntBits(this.biomeDepthOffset) : 0);
            i = 31 * i + (this.biomeScaleWeight != 0.0F ? Float.floatToIntBits(this.biomeScaleWeight) : 0);
            i = 31 * i + (this.biomeScaleOffset != 0.0F ? Float.floatToIntBits(this.biomeScaleOffset) : 0);
            i = 31 * i + this.seaLevel;
            i = 31 * i + (this.useCaves ? 1 : 0);
            i = 31 * i + (this.useDungeons ? 1 : 0);
            i = 31 * i + this.dungeonChance;
            i = 31 * i + (this.useStrongholds ? 1 : 0);
            i = 31 * i + (this.useVillages ? 1 : 0);
            i = 31 * i + (this.useMineShafts ? 1 : 0);
            i = 31 * i + (this.useTemples ? 1 : 0);
            i = 31 * i + (this.useMonuments ? 1 : 0);
            i = 31 * i + (this.useRavines ? 1 : 0);
            i = 31 * i + (this.useWaterLakes ? 1 : 0);
            i = 31 * i + this.waterLakeChance;
            i = 31 * i + (this.useLavaLakes ? 1 : 0);
            i = 31 * i + this.lavaLakeChance;
            i = 31 * i + (this.useLavaOceans ? 1 : 0);
            i = 31 * i + this.fixedBiome;
            i = 31 * i + this.biomeSize;
            i = 31 * i + this.riverSize;
            i = 31 * i + this.dirtSize;
            i = 31 * i + this.dirtCount;
            i = 31 * i + this.dirtMinHeight;
            i = 31 * i + this.dirtMaxHeight;
            i = 31 * i + this.gravelSize;
            i = 31 * i + this.gravelCount;
            i = 31 * i + this.gravelMinHeight;
            i = 31 * i + this.gravelMaxHeight;
            i = 31 * i + this.graniteSize;
            i = 31 * i + this.graniteCount;
            i = 31 * i + this.graniteMinHeight;
            i = 31 * i + this.graniteMaxHeight;
            i = 31 * i + this.dioriteSize;
            i = 31 * i + this.dioriteCount;
            i = 31 * i + this.dioriteMinHeight;
            i = 31 * i + this.dioriteMaxHeight;
            i = 31 * i + this.andesiteSize;
            i = 31 * i + this.andesiteCount;
            i = 31 * i + this.andesiteMinHeight;
            i = 31 * i + this.andesiteMaxHeight;
            i = 31 * i + this.coalSize;
            i = 31 * i + this.coalCount;
            i = 31 * i + this.coalMinHeight;
            i = 31 * i + this.coalMaxHeight;
            i = 31 * i + this.ironSize;
            i = 31 * i + this.ironCount;
            i = 31 * i + this.ironMinHeight;
            i = 31 * i + this.ironMaxHeight;
            i = 31 * i + this.goldSize;
            i = 31 * i + this.goldCount;
            i = 31 * i + this.goldMinHeight;
            i = 31 * i + this.goldMaxHeight;
            i = 31 * i + this.redstoneSize;
            i = 31 * i + this.redstoneCount;
            i = 31 * i + this.redstoneMinHeight;
            i = 31 * i + this.redstoneMaxHeight;
            i = 31 * i + this.diamondSize;
            i = 31 * i + this.diamondCount;
            i = 31 * i + this.diamondMinHeight;
            i = 31 * i + this.diamondMaxHeight;
            i = 31 * i + this.lapisSize;
            i = 31 * i + this.lapisCount;
            i = 31 * i + this.lapisCenterHeight;
            i = 31 * i + this.lapisSpread;
            return i;
        }

        public ChunkProviderSettings func_177864_b() {
            return new ChunkProviderSettings(this);
        }
    }

    public static class Serializer implements JsonDeserializer<Factory>, JsonSerializer<Factory> {
        public Factory deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            Factory factory = new Factory();

            try {
                factory.coordinateScale = JsonUtils.getFloat(jsonObject, "coordinateScale", factory.coordinateScale);
                factory.heightScale = JsonUtils.getFloat(jsonObject, "heightScale", factory.heightScale);
                factory.lowerLimitScale = JsonUtils.getFloat(jsonObject, "lowerLimitScale", factory.lowerLimitScale);
                factory.upperLimitScale = JsonUtils.getFloat(jsonObject, "upperLimitScale", factory.upperLimitScale);
                factory.depthNoiseScaleX = JsonUtils.getFloat(jsonObject, "depthNoiseScaleX", factory.depthNoiseScaleX);
                factory.depthNoiseScaleZ = JsonUtils.getFloat(jsonObject, "depthNoiseScaleZ", factory.depthNoiseScaleZ);
                factory.depthNoiseScaleExponent = JsonUtils.getFloat(jsonObject, "depthNoiseScaleExponent", factory.depthNoiseScaleExponent);
                factory.mainNoiseScaleX = JsonUtils.getFloat(jsonObject, "mainNoiseScaleX", factory.mainNoiseScaleX);
                factory.mainNoiseScaleY = JsonUtils.getFloat(jsonObject, "mainNoiseScaleY", factory.mainNoiseScaleY);
                factory.mainNoiseScaleZ = JsonUtils.getFloat(jsonObject, "mainNoiseScaleZ", factory.mainNoiseScaleZ);
                factory.baseSize = JsonUtils.getFloat(jsonObject, "baseSize", factory.baseSize);
                factory.stretchY = JsonUtils.getFloat(jsonObject, "stretchY", factory.stretchY);
                factory.biomeDepthWeight = JsonUtils.getFloat(jsonObject, "biomeDepthWeight", factory.biomeDepthWeight);
                factory.biomeDepthOffset = JsonUtils.getFloat(jsonObject, "biomeDepthOffset", factory.biomeDepthOffset);
                factory.biomeScaleWeight = JsonUtils.getFloat(jsonObject, "biomeScaleWeight", factory.biomeScaleWeight);
                factory.biomeScaleOffset = JsonUtils.getFloat(jsonObject, "biomeScaleOffset", factory.biomeScaleOffset);
                factory.seaLevel = JsonUtils.getInt(jsonObject, "seaLevel", factory.seaLevel);
                factory.useCaves = JsonUtils.getBoolean(jsonObject, "useCaves", factory.useCaves);
                factory.useDungeons = JsonUtils.getBoolean(jsonObject, "useDungeons", factory.useDungeons);
                factory.dungeonChance = JsonUtils.getInt(jsonObject, "dungeonChance", factory.dungeonChance);
                factory.useStrongholds = JsonUtils.getBoolean(jsonObject, "useStrongholds", factory.useStrongholds);
                factory.useVillages = JsonUtils.getBoolean(jsonObject, "useVillages", factory.useVillages);
                factory.useMineShafts = JsonUtils.getBoolean(jsonObject, "useMineShafts", factory.useMineShafts);
                factory.useTemples = JsonUtils.getBoolean(jsonObject, "useTemples", factory.useTemples);
                factory.useMonuments = JsonUtils.getBoolean(jsonObject, "useMonuments", factory.useMonuments);
                factory.useRavines = JsonUtils.getBoolean(jsonObject, "useRavines", factory.useRavines);
                factory.useWaterLakes = JsonUtils.getBoolean(jsonObject, "useWaterLakes", factory.useWaterLakes);
                factory.waterLakeChance = JsonUtils.getInt(jsonObject, "waterLakeChance", factory.waterLakeChance);
                factory.useLavaLakes = JsonUtils.getBoolean(jsonObject, "useLavaLakes", factory.useLavaLakes);
                factory.lavaLakeChance = JsonUtils.getInt(jsonObject, "lavaLakeChance", factory.lavaLakeChance);
                factory.useLavaOceans = JsonUtils.getBoolean(jsonObject, "useLavaOceans", factory.useLavaOceans);
                factory.fixedBiome = JsonUtils.getInt(jsonObject, "fixedBiome", factory.fixedBiome);

                if (factory.fixedBiome < 38 && factory.fixedBiome >= -1) {
                    if (factory.fixedBiome >= BiomeGenBase.HELL.biomeID) {
                        factory.fixedBiome += 2;
                    }
                } else {
                    factory.fixedBiome = -1;
                }

                factory.biomeSize = JsonUtils.getInt(jsonObject, "biomeSize", factory.biomeSize);
                factory.riverSize = JsonUtils.getInt(jsonObject, "riverSize", factory.riverSize);
                factory.dirtSize = JsonUtils.getInt(jsonObject, "dirtSize", factory.dirtSize);
                factory.dirtCount = JsonUtils.getInt(jsonObject, "dirtCount", factory.dirtCount);
                factory.dirtMinHeight = JsonUtils.getInt(jsonObject, "dirtMinHeight", factory.dirtMinHeight);
                factory.dirtMaxHeight = JsonUtils.getInt(jsonObject, "dirtMaxHeight", factory.dirtMaxHeight);
                factory.gravelSize = JsonUtils.getInt(jsonObject, "gravelSize", factory.gravelSize);
                factory.gravelCount = JsonUtils.getInt(jsonObject, "gravelCount", factory.gravelCount);
                factory.gravelMinHeight = JsonUtils.getInt(jsonObject, "gravelMinHeight", factory.gravelMinHeight);
                factory.gravelMaxHeight = JsonUtils.getInt(jsonObject, "gravelMaxHeight", factory.gravelMaxHeight);
                factory.graniteSize = JsonUtils.getInt(jsonObject, "graniteSize", factory.graniteSize);
                factory.graniteCount = JsonUtils.getInt(jsonObject, "graniteCount", factory.graniteCount);
                factory.graniteMinHeight = JsonUtils.getInt(jsonObject, "graniteMinHeight", factory.graniteMinHeight);
                factory.graniteMaxHeight = JsonUtils.getInt(jsonObject, "graniteMaxHeight", factory.graniteMaxHeight);
                factory.dioriteSize = JsonUtils.getInt(jsonObject, "dioriteSize", factory.dioriteSize);
                factory.dioriteCount = JsonUtils.getInt(jsonObject, "dioriteCount", factory.dioriteCount);
                factory.dioriteMinHeight = JsonUtils.getInt(jsonObject, "dioriteMinHeight", factory.dioriteMinHeight);
                factory.dioriteMaxHeight = JsonUtils.getInt(jsonObject, "dioriteMaxHeight", factory.dioriteMaxHeight);
                factory.andesiteSize = JsonUtils.getInt(jsonObject, "andesiteSize", factory.andesiteSize);
                factory.andesiteCount = JsonUtils.getInt(jsonObject, "andesiteCount", factory.andesiteCount);
                factory.andesiteMinHeight = JsonUtils.getInt(jsonObject, "andesiteMinHeight", factory.andesiteMinHeight);
                factory.andesiteMaxHeight = JsonUtils.getInt(jsonObject, "andesiteMaxHeight", factory.andesiteMaxHeight);
                factory.coalSize = JsonUtils.getInt(jsonObject, "coalSize", factory.coalSize);
                factory.coalCount = JsonUtils.getInt(jsonObject, "coalCount", factory.coalCount);
                factory.coalMinHeight = JsonUtils.getInt(jsonObject, "coalMinHeight", factory.coalMinHeight);
                factory.coalMaxHeight = JsonUtils.getInt(jsonObject, "coalMaxHeight", factory.coalMaxHeight);
                factory.ironSize = JsonUtils.getInt(jsonObject, "ironSize", factory.ironSize);
                factory.ironCount = JsonUtils.getInt(jsonObject, "ironCount", factory.ironCount);
                factory.ironMinHeight = JsonUtils.getInt(jsonObject, "ironMinHeight", factory.ironMinHeight);
                factory.ironMaxHeight = JsonUtils.getInt(jsonObject, "ironMaxHeight", factory.ironMaxHeight);
                factory.goldSize = JsonUtils.getInt(jsonObject, "goldSize", factory.goldSize);
                factory.goldCount = JsonUtils.getInt(jsonObject, "goldCount", factory.goldCount);
                factory.goldMinHeight = JsonUtils.getInt(jsonObject, "goldMinHeight", factory.goldMinHeight);
                factory.goldMaxHeight = JsonUtils.getInt(jsonObject, "goldMaxHeight", factory.goldMaxHeight);
                factory.redstoneSize = JsonUtils.getInt(jsonObject, "redstoneSize", factory.redstoneSize);
                factory.redstoneCount = JsonUtils.getInt(jsonObject, "redstoneCount", factory.redstoneCount);
                factory.redstoneMinHeight = JsonUtils.getInt(jsonObject, "redstoneMinHeight", factory.redstoneMinHeight);
                factory.redstoneMaxHeight = JsonUtils.getInt(jsonObject, "redstoneMaxHeight", factory.redstoneMaxHeight);
                factory.diamondSize = JsonUtils.getInt(jsonObject, "diamondSize", factory.diamondSize);
                factory.diamondCount = JsonUtils.getInt(jsonObject, "diamondCount", factory.diamondCount);
                factory.diamondMinHeight = JsonUtils.getInt(jsonObject, "diamondMinHeight", factory.diamondMinHeight);
                factory.diamondMaxHeight = JsonUtils.getInt(jsonObject, "diamondMaxHeight", factory.diamondMaxHeight);
                factory.lapisSize = JsonUtils.getInt(jsonObject, "lapisSize", factory.lapisSize);
                factory.lapisCount = JsonUtils.getInt(jsonObject, "lapisCount", factory.lapisCount);
                factory.lapisCenterHeight = JsonUtils.getInt(jsonObject, "lapisCenterHeight", factory.lapisCenterHeight);
                factory.lapisSpread = JsonUtils.getInt(jsonObject, "lapisSpread", factory.lapisSpread);
            } catch (Exception ignored) {
            }

            return factory;
        }

        public JsonElement serialize(Factory factory, Type type, JsonSerializationContext ctx) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("coordinateScale", factory.coordinateScale);
            jsonObject.addProperty("heightScale", factory.heightScale);
            jsonObject.addProperty("lowerLimitScale", factory.lowerLimitScale);
            jsonObject.addProperty("upperLimitScale", factory.upperLimitScale);
            jsonObject.addProperty("depthNoiseScaleX", factory.depthNoiseScaleX);
            jsonObject.addProperty("depthNoiseScaleZ", factory.depthNoiseScaleZ);
            jsonObject.addProperty("depthNoiseScaleExponent", factory.depthNoiseScaleExponent);
            jsonObject.addProperty("mainNoiseScaleX", factory.mainNoiseScaleX);
            jsonObject.addProperty("mainNoiseScaleY", factory.mainNoiseScaleY);
            jsonObject.addProperty("mainNoiseScaleZ", factory.mainNoiseScaleZ);
            jsonObject.addProperty("baseSize", factory.baseSize);
            jsonObject.addProperty("stretchY", factory.stretchY);
            jsonObject.addProperty("biomeDepthWeight", factory.biomeDepthWeight);
            jsonObject.addProperty("biomeDepthOffset", factory.biomeDepthOffset);
            jsonObject.addProperty("biomeScaleWeight", factory.biomeScaleWeight);
            jsonObject.addProperty("biomeScaleOffset", factory.biomeScaleOffset);
            jsonObject.addProperty("seaLevel", factory.seaLevel);
            jsonObject.addProperty("useCaves", factory.useCaves);
            jsonObject.addProperty("useDungeons", factory.useDungeons);
            jsonObject.addProperty("dungeonChance", factory.dungeonChance);
            jsonObject.addProperty("useStrongholds", factory.useStrongholds);
            jsonObject.addProperty("useVillages", factory.useVillages);
            jsonObject.addProperty("useMineShafts", factory.useMineShafts);
            jsonObject.addProperty("useTemples", factory.useTemples);
            jsonObject.addProperty("useMonuments", factory.useMonuments);
            jsonObject.addProperty("useRavines", factory.useRavines);
            jsonObject.addProperty("useWaterLakes", factory.useWaterLakes);
            jsonObject.addProperty("waterLakeChance", factory.waterLakeChance);
            jsonObject.addProperty("useLavaLakes", factory.useLavaLakes);
            jsonObject.addProperty("lavaLakeChance", factory.lavaLakeChance);
            jsonObject.addProperty("useLavaOceans", factory.useLavaOceans);
            jsonObject.addProperty("fixedBiome", factory.fixedBiome);
            jsonObject.addProperty("biomeSize", factory.biomeSize);
            jsonObject.addProperty("riverSize", factory.riverSize);
            jsonObject.addProperty("dirtSize", factory.dirtSize);
            jsonObject.addProperty("dirtCount", factory.dirtCount);
            jsonObject.addProperty("dirtMinHeight", factory.dirtMinHeight);
            jsonObject.addProperty("dirtMaxHeight", factory.dirtMaxHeight);
            jsonObject.addProperty("gravelSize", factory.gravelSize);
            jsonObject.addProperty("gravelCount", factory.gravelCount);
            jsonObject.addProperty("gravelMinHeight", factory.gravelMinHeight);
            jsonObject.addProperty("gravelMaxHeight", factory.gravelMaxHeight);
            jsonObject.addProperty("graniteSize", factory.graniteSize);
            jsonObject.addProperty("graniteCount", factory.graniteCount);
            jsonObject.addProperty("graniteMinHeight", factory.graniteMinHeight);
            jsonObject.addProperty("graniteMaxHeight", factory.graniteMaxHeight);
            jsonObject.addProperty("dioriteSize", factory.dioriteSize);
            jsonObject.addProperty("dioriteCount", factory.dioriteCount);
            jsonObject.addProperty("dioriteMinHeight", factory.dioriteMinHeight);
            jsonObject.addProperty("dioriteMaxHeight", factory.dioriteMaxHeight);
            jsonObject.addProperty("andesiteSize", factory.andesiteSize);
            jsonObject.addProperty("andesiteCount", factory.andesiteCount);
            jsonObject.addProperty("andesiteMinHeight", factory.andesiteMinHeight);
            jsonObject.addProperty("andesiteMaxHeight", factory.andesiteMaxHeight);
            jsonObject.addProperty("coalSize", factory.coalSize);
            jsonObject.addProperty("coalCount", factory.coalCount);
            jsonObject.addProperty("coalMinHeight", factory.coalMinHeight);
            jsonObject.addProperty("coalMaxHeight", factory.coalMaxHeight);
            jsonObject.addProperty("ironSize", factory.ironSize);
            jsonObject.addProperty("ironCount", factory.ironCount);
            jsonObject.addProperty("ironMinHeight", factory.ironMinHeight);
            jsonObject.addProperty("ironMaxHeight", factory.ironMaxHeight);
            jsonObject.addProperty("goldSize", factory.goldSize);
            jsonObject.addProperty("goldCount", factory.goldCount);
            jsonObject.addProperty("goldMinHeight", factory.goldMinHeight);
            jsonObject.addProperty("goldMaxHeight", factory.goldMaxHeight);
            jsonObject.addProperty("redstoneSize", factory.redstoneSize);
            jsonObject.addProperty("redstoneCount", factory.redstoneCount);
            jsonObject.addProperty("redstoneMinHeight", factory.redstoneMinHeight);
            jsonObject.addProperty("redstoneMaxHeight", factory.redstoneMaxHeight);
            jsonObject.addProperty("diamondSize", factory.diamondSize);
            jsonObject.addProperty("diamondCount", factory.diamondCount);
            jsonObject.addProperty("diamondMinHeight", factory.diamondMinHeight);
            jsonObject.addProperty("diamondMaxHeight", factory.diamondMaxHeight);
            jsonObject.addProperty("lapisSize", factory.lapisSize);
            jsonObject.addProperty("lapisCount", factory.lapisCount);
            jsonObject.addProperty("lapisCenterHeight", factory.lapisCenterHeight);
            jsonObject.addProperty("lapisSpread", factory.lapisSpread);
            return jsonObject;
        }
    }
}
