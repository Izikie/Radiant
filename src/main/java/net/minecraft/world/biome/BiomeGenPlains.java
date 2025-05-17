package net.minecraft.world.biome;

import java.util.Random;

import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class BiomeGenPlains extends BiomeGenBase {
    protected boolean field_150628_aC;

    protected BiomeGenPlains(int id) {
        super(id);
        this.setTemperatureRainfall(0.8F, 0.4F);
        this.setHeight(HEIGHT_LOW_PLAINS);
        this.spawnableCreatureList.add(new BiomeGenBase.SpawnListEntry(EntityHorse.class, 5, 2, 6));
        this.theBiomeDecorator.treesPerChunk = -999;
        this.theBiomeDecorator.flowersPerChunk = 4;
        this.theBiomeDecorator.grassPerChunk = 10;
    }

    public BlockFlower.FlowerType pickRandomFlower(Random rand, BlockPos pos) {
        double d0 = GRASS_COLOR_NOISE.func_151601_a(pos.getX() / 200.0D, pos.getZ() / 200.0D);

        if (d0 < -0.8D) {
            int j = rand.nextInt(4);

            return switch (j) {
                case 0 -> BlockFlower.FlowerType.ORANGE_TULIP;
                case 1 -> BlockFlower.FlowerType.RED_TULIP;
                case 2 -> BlockFlower.FlowerType.PINK_TULIP;
                default -> BlockFlower.FlowerType.WHITE_TULIP;
            };
        } else if (rand.nextInt(3) > 0) {
            int i = rand.nextInt(3);
            return i == 0 ? BlockFlower.FlowerType.POPPY : (i == 1 ? BlockFlower.FlowerType.HOUSTONIA : BlockFlower.FlowerType.OXEYE_DAISY);
        } else {
            return BlockFlower.FlowerType.DANDELION;
        }
    }

    public void decorate(World worldIn, Random rand, BlockPos pos) {
        double d0 = GRASS_COLOR_NOISE.func_151601_a((pos.getX() + 8) / 200.0D, (pos.getZ() + 8) / 200.0D);

        if (d0 < -0.8D) {
            this.theBiomeDecorator.flowersPerChunk = 15;
            this.theBiomeDecorator.grassPerChunk = 5;
        } else {
            this.theBiomeDecorator.flowersPerChunk = 4;
            this.theBiomeDecorator.grassPerChunk = 10;
            DOUBLE_PLANT_GENERATOR.setPlantType(BlockDoublePlant.EnumPlantType.GRASS);

            for (int i = 0; i < 7; ++i) {
                int j = rand.nextInt(16) + 8;
                int k = rand.nextInt(16) + 8;
                int l = rand.nextInt(worldIn.getHeight(pos.add(j, 0, k)).getY() + 32);
                DOUBLE_PLANT_GENERATOR.generate(worldIn, rand, pos.add(j, l, k));
            }
        }

        if (this.field_150628_aC) {
            DOUBLE_PLANT_GENERATOR.setPlantType(BlockDoublePlant.EnumPlantType.SUNFLOWER);

            for (int i1 = 0; i1 < 10; ++i1) {
                int j1 = rand.nextInt(16) + 8;
                int k1 = rand.nextInt(16) + 8;
                int l1 = rand.nextInt(worldIn.getHeight(pos.add(j1, 0, k1)).getY() + 32);
                DOUBLE_PLANT_GENERATOR.generate(worldIn, rand, pos.add(j1, l1, k1));
            }
        }

        super.decorate(worldIn, rand, pos);
    }

    protected BiomeGenBase createMutatedBiome(int p_180277_1_) {
        BiomeGenPlains biomegenplains = new BiomeGenPlains(p_180277_1_);
        biomegenplains.setBiomeName("Sunflower Plains");
        biomegenplains.field_150628_aC = true;
        biomegenplains.setColor(9286496);
        biomegenplains.field_150609_ah = 14273354;
        return biomegenplains;
    }
}
