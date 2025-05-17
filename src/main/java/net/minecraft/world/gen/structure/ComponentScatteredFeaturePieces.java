package net.minecraft.world.gen.structure;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Random;

import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSandStone;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.block.BlockStoneSlab;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.DyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.World;

public class ComponentScatteredFeaturePieces {
    public static void registerScatteredFeaturePieces() {
        MapGenStructureIO.registerStructureComponent(ComponentScatteredFeaturePieces.DesertPyramid.class, "TeDP");
        MapGenStructureIO.registerStructureComponent(ComponentScatteredFeaturePieces.JunglePyramid.class, "TeJP");
        MapGenStructureIO.registerStructureComponent(ComponentScatteredFeaturePieces.SwampHut.class, "TeSH");
    }

    public static class DesertPyramid extends ComponentScatteredFeaturePieces.Feature {
        private final boolean[] hasPlacedChest = new boolean[4];
        private static final List<WeightedRandomChestContent> ITEMS_TO_GENERATE_IN_TEMPLE = Lists.newArrayList(new WeightedRandomChestContent(Items.DIAMOND, 0, 1, 3, 3), new WeightedRandomChestContent(Items.IRON_INGOT, 0, 1, 5, 10), new WeightedRandomChestContent(Items.GOLD_INGOT, 0, 2, 7, 15), new WeightedRandomChestContent(Items.EMERALD, 0, 1, 3, 2), new WeightedRandomChestContent(Items.BONE, 0, 4, 6, 20), new WeightedRandomChestContent(Items.ROTTEN_FLESH, 0, 3, 7, 16), new WeightedRandomChestContent(Items.SADDLE, 0, 1, 1, 3), new WeightedRandomChestContent(Items.IRON_HORSE_ARMOR, 0, 1, 1, 1), new WeightedRandomChestContent(Items.GOLDEN_HORSE_ARMOR, 0, 1, 1, 1), new WeightedRandomChestContent(Items.DIAMOND_HORSE_ARMOR, 0, 1, 1, 1));

        public DesertPyramid() {
        }

        public DesertPyramid(Random p_i2062_1_, int p_i2062_2_, int p_i2062_3_) {
            super(p_i2062_1_, p_i2062_2_, 64, p_i2062_3_, 21, 15, 21);
        }

        protected void writeStructureToNBT(NBTTagCompound tagCompound) {
            super.writeStructureToNBT(tagCompound);
            tagCompound.setBoolean("hasPlacedChest0", this.hasPlacedChest[0]);
            tagCompound.setBoolean("hasPlacedChest1", this.hasPlacedChest[1]);
            tagCompound.setBoolean("hasPlacedChest2", this.hasPlacedChest[2]);
            tagCompound.setBoolean("hasPlacedChest3", this.hasPlacedChest[3]);
        }

        protected void readStructureFromNBT(NBTTagCompound tagCompound) {
            super.readStructureFromNBT(tagCompound);
            this.hasPlacedChest[0] = tagCompound.getBoolean("hasPlacedChest0");
            this.hasPlacedChest[1] = tagCompound.getBoolean("hasPlacedChest1");
            this.hasPlacedChest[2] = tagCompound.getBoolean("hasPlacedChest2");
            this.hasPlacedChest[3] = tagCompound.getBoolean("hasPlacedChest3");
        }

        public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox structureBoundingBoxIn) {
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 0, -4, 0, this.scatteredFeatureSizeX - 1, 0, this.scatteredFeatureSizeZ - 1, Blocks.SANDSTONE.getDefaultState(), Blocks.SANDSTONE.getDefaultState(), false);

            for (int i = 1; i <= 9; ++i) {
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, i, i, i, this.scatteredFeatureSizeX - 1 - i, i, this.scatteredFeatureSizeZ - 1 - i, Blocks.SANDSTONE.getDefaultState(), Blocks.SANDSTONE.getDefaultState(), false);
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, i + 1, i, i + 1, this.scatteredFeatureSizeX - 2 - i, i, this.scatteredFeatureSizeZ - 2 - i, Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState(), false);
            }

            for (int j2 = 0; j2 < this.scatteredFeatureSizeX; ++j2) {
                for (int j = 0; j < this.scatteredFeatureSizeZ; ++j) {
                    int k = -5;
                    this.replaceAirAndLiquidDownwards(worldIn, Blocks.SANDSTONE.getDefaultState(), j2, k, j, structureBoundingBoxIn);
                }
            }

            int k2 = this.getMetadataWithOffset(Blocks.SANDSTONE_STAIRS, 3);
            int l2 = this.getMetadataWithOffset(Blocks.SANDSTONE_STAIRS, 2);
            int i3 = this.getMetadataWithOffset(Blocks.SANDSTONE_STAIRS, 0);
            int l = this.getMetadataWithOffset(Blocks.SANDSTONE_STAIRS, 1);
            int i1 = ~DyeColor.ORANGE.getDyeDamage() & 15;
            int j1 = ~DyeColor.BLUE.getDyeDamage() & 15;
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 0, 0, 0, 4, 9, 4, Blocks.SANDSTONE.getDefaultState(), Blocks.AIR.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 1, 10, 1, 3, 10, 3, Blocks.SANDSTONE.getDefaultState(), Blocks.SANDSTONE.getDefaultState(), false);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getStateFromMeta(k2), 2, 10, 0, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getStateFromMeta(l2), 2, 10, 4, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getStateFromMeta(i3), 0, 10, 2, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getStateFromMeta(l), 4, 10, 2, structureBoundingBoxIn);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, this.scatteredFeatureSizeX - 5, 0, 0, this.scatteredFeatureSizeX - 1, 9, 4, Blocks.SANDSTONE.getDefaultState(), Blocks.AIR.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, this.scatteredFeatureSizeX - 4, 10, 1, this.scatteredFeatureSizeX - 2, 10, 3, Blocks.SANDSTONE.getDefaultState(), Blocks.SANDSTONE.getDefaultState(), false);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getStateFromMeta(k2), this.scatteredFeatureSizeX - 3, 10, 0, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getStateFromMeta(l2), this.scatteredFeatureSizeX - 3, 10, 4, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getStateFromMeta(i3), this.scatteredFeatureSizeX - 5, 10, 2, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getStateFromMeta(l), this.scatteredFeatureSizeX - 1, 10, 2, structureBoundingBoxIn);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 8, 0, 0, 12, 4, 4, Blocks.SANDSTONE.getDefaultState(), Blocks.AIR.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 9, 1, 0, 11, 3, 4, Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState(), false);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), 9, 1, 1, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), 9, 2, 1, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), 9, 3, 1, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), 10, 3, 1, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), 11, 3, 1, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), 11, 2, 1, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), 11, 1, 1, structureBoundingBoxIn);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 4, 1, 1, 8, 3, 3, Blocks.SANDSTONE.getDefaultState(), Blocks.AIR.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 4, 1, 2, 8, 2, 2, Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 12, 1, 1, 16, 3, 3, Blocks.SANDSTONE.getDefaultState(), Blocks.AIR.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 12, 1, 2, 16, 2, 2, Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 5, 4, 5, this.scatteredFeatureSizeX - 6, 4, this.scatteredFeatureSizeZ - 6, Blocks.SANDSTONE.getDefaultState(), Blocks.SANDSTONE.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 9, 4, 9, 11, 4, 11, Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 8, 1, 8, 8, 3, 8, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 12, 1, 8, 12, 3, 8, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 8, 1, 12, 8, 3, 12, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 12, 1, 12, 12, 3, 12, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 1, 1, 5, 4, 4, 11, Blocks.SANDSTONE.getDefaultState(), Blocks.SANDSTONE.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, this.scatteredFeatureSizeX - 5, 1, 5, this.scatteredFeatureSizeX - 2, 4, 11, Blocks.SANDSTONE.getDefaultState(), Blocks.SANDSTONE.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 6, 7, 9, 6, 7, 11, Blocks.SANDSTONE.getDefaultState(), Blocks.SANDSTONE.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, this.scatteredFeatureSizeX - 7, 7, 9, this.scatteredFeatureSizeX - 7, 7, 11, Blocks.SANDSTONE.getDefaultState(), Blocks.SANDSTONE.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 5, 5, 9, 5, 7, 11, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, this.scatteredFeatureSizeX - 6, 5, 9, this.scatteredFeatureSizeX - 6, 7, 11, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), false);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 5, 5, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 5, 6, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 6, 6, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), this.scatteredFeatureSizeX - 6, 5, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), this.scatteredFeatureSizeX - 6, 6, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), this.scatteredFeatureSizeX - 7, 6, 10, structureBoundingBoxIn);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 2, 4, 4, 2, 6, 4, Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, this.scatteredFeatureSizeX - 3, 4, 4, this.scatteredFeatureSizeX - 3, 6, 4, Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState(), false);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getStateFromMeta(k2), 2, 4, 5, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getStateFromMeta(k2), 2, 3, 4, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getStateFromMeta(k2), this.scatteredFeatureSizeX - 3, 4, 5, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getStateFromMeta(k2), this.scatteredFeatureSizeX - 3, 3, 4, structureBoundingBoxIn);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 1, 1, 3, 2, 2, 3, Blocks.SANDSTONE.getDefaultState(), Blocks.SANDSTONE.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, this.scatteredFeatureSizeX - 3, 1, 3, this.scatteredFeatureSizeX - 2, 2, 3, Blocks.SANDSTONE.getDefaultState(), Blocks.SANDSTONE.getDefaultState(), false);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getDefaultState(), 1, 1, 2, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getDefaultState(), this.scatteredFeatureSizeX - 2, 1, 2, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STONE_SLAB.getStateFromMeta(BlockStoneSlab.EnumType.SAND.getMetadata()), 1, 2, 2, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STONE_SLAB.getStateFromMeta(BlockStoneSlab.EnumType.SAND.getMetadata()), this.scatteredFeatureSizeX - 2, 2, 2, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getStateFromMeta(l), 2, 1, 2, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE_STAIRS.getStateFromMeta(i3), this.scatteredFeatureSizeX - 3, 1, 2, structureBoundingBoxIn);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 4, 3, 5, 4, 3, 18, Blocks.SANDSTONE.getDefaultState(), Blocks.SANDSTONE.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, this.scatteredFeatureSizeX - 5, 3, 5, this.scatteredFeatureSizeX - 5, 3, 17, Blocks.SANDSTONE.getDefaultState(), Blocks.SANDSTONE.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 3, 1, 5, 4, 2, 16, Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, this.scatteredFeatureSizeX - 6, 1, 5, this.scatteredFeatureSizeX - 5, 2, 16, Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState(), false);

            for (int k1 = 5; k1 <= 17; k1 += 2) {
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), 4, 1, k1, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.CHISELED.getMetadata()), 4, 2, k1, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), this.scatteredFeatureSizeX - 5, 1, k1, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.CHISELED.getMetadata()), this.scatteredFeatureSizeX - 5, 2, k1, structureBoundingBoxIn);
            }

            this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), 10, 0, 7, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), 10, 0, 8, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), 9, 0, 9, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), 11, 0, 9, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), 8, 0, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), 12, 0, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), 7, 0, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), 13, 0, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), 9, 0, 11, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), 11, 0, 11, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), 10, 0, 12, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), 10, 0, 13, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(j1), 10, 0, 10, structureBoundingBoxIn);

            for (int j3 = 0; j3 <= this.scatteredFeatureSizeX - 1; j3 += this.scatteredFeatureSizeX - 1) {
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), j3, 2, 1, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), j3, 2, 2, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), j3, 2, 3, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), j3, 3, 1, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), j3, 3, 2, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), j3, 3, 3, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), j3, 4, 1, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.CHISELED.getMetadata()), j3, 4, 2, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), j3, 4, 3, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), j3, 5, 1, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), j3, 5, 2, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), j3, 5, 3, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), j3, 6, 1, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.CHISELED.getMetadata()), j3, 6, 2, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), j3, 6, 3, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), j3, 7, 1, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), j3, 7, 2, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), j3, 7, 3, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), j3, 8, 1, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), j3, 8, 2, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), j3, 8, 3, structureBoundingBoxIn);
            }

            for (int k3 = 2; k3 <= this.scatteredFeatureSizeX - 3; k3 += this.scatteredFeatureSizeX - 3 - 2) {
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), k3 - 1, 2, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), k3, 2, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), k3 + 1, 2, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), k3 - 1, 3, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), k3, 3, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), k3 + 1, 3, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), k3 - 1, 4, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.CHISELED.getMetadata()), k3, 4, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), k3 + 1, 4, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), k3 - 1, 5, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), k3, 5, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), k3 + 1, 5, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), k3 - 1, 6, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.CHISELED.getMetadata()), k3, 6, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), k3 + 1, 6, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), k3 - 1, 7, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), k3, 7, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), k3 + 1, 7, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), k3 - 1, 8, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), k3, 8, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), k3 + 1, 8, 0, structureBoundingBoxIn);
            }

            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 8, 4, 0, 12, 6, 0, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), false);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 8, 6, 0, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 12, 6, 0, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), 9, 5, 0, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.CHISELED.getMetadata()), 10, 5, 0, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(i1), 11, 5, 0, structureBoundingBoxIn);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 8, -14, 8, 12, -11, 12, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 8, -10, 8, 12, -10, 12, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.CHISELED.getMetadata()), Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.CHISELED.getMetadata()), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 8, -9, 8, 12, -9, 12, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 8, -8, 8, 12, -1, 12, Blocks.SANDSTONE.getDefaultState(), Blocks.SANDSTONE.getDefaultState(), false);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 9, -11, 9, 11, -1, 11, Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState(), false);
            this.setBlockState(worldIn, Blocks.STONE_PRESSURE_PLATE.getDefaultState(), 10, -11, 10, structureBoundingBoxIn);
            this.fillWithBlocks(worldIn, structureBoundingBoxIn, 9, -13, 9, 11, -13, 11, Blocks.TNT.getDefaultState(), Blocks.AIR.getDefaultState(), false);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 8, -11, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 8, -10, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.CHISELED.getMetadata()), 7, -10, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), 7, -11, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 12, -11, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 12, -10, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.CHISELED.getMetadata()), 13, -10, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), 13, -11, 10, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 10, -11, 8, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 10, -10, 8, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.CHISELED.getMetadata()), 10, -10, 7, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), 10, -11, 7, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 10, -11, 12, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 10, -10, 12, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.CHISELED.getMetadata()), 10, -10, 13, structureBoundingBoxIn);
            this.setBlockState(worldIn, Blocks.SANDSTONE.getStateFromMeta(BlockSandStone.SandStoneType.SMOOTH.getMetadata()), 10, -11, 13, structureBoundingBoxIn);

            for (Direction enumfacing : Direction.Plane.HORIZONTAL) {
                if (!this.hasPlacedChest[enumfacing.getHorizontalIndex()]) {
                    int l1 = enumfacing.getFrontOffsetX() * 2;
                    int i2 = enumfacing.getFrontOffsetZ() * 2;
                    this.hasPlacedChest[enumfacing.getHorizontalIndex()] = this.generateChestContents(worldIn, structureBoundingBoxIn, randomIn, 10 + l1, -11, 10 + i2, WeightedRandomChestContent.func_177629_a(ITEMS_TO_GENERATE_IN_TEMPLE, Items.ENCHANTED_BOOK.getRandom(randomIn)), 2 + randomIn.nextInt(5));
                }
            }

            return true;
        }
    }

    abstract static class Feature extends StructureComponent {
        protected int scatteredFeatureSizeX;
        protected int scatteredFeatureSizeY;
        protected int scatteredFeatureSizeZ;
        protected int field_74936_d = -1;

        public Feature() {
        }

        protected Feature(Random p_i2065_1_, int p_i2065_2_, int p_i2065_3_, int p_i2065_4_, int p_i2065_5_, int p_i2065_6_, int p_i2065_7_) {
            super(0);
            this.scatteredFeatureSizeX = p_i2065_5_;
            this.scatteredFeatureSizeY = p_i2065_6_;
            this.scatteredFeatureSizeZ = p_i2065_7_;
            this.coordBaseMode = Direction.Plane.HORIZONTAL.random(p_i2065_1_);

            switch (this.coordBaseMode) {
                case NORTH:
                case SOUTH:
                    this.boundingBox = new StructureBoundingBox(p_i2065_2_, p_i2065_3_, p_i2065_4_, p_i2065_2_ + p_i2065_5_ - 1, p_i2065_3_ + p_i2065_6_ - 1, p_i2065_4_ + p_i2065_7_ - 1);
                    break;

                default:
                    this.boundingBox = new StructureBoundingBox(p_i2065_2_, p_i2065_3_, p_i2065_4_, p_i2065_2_ + p_i2065_7_ - 1, p_i2065_3_ + p_i2065_6_ - 1, p_i2065_4_ + p_i2065_5_ - 1);
            }
        }

        protected void writeStructureToNBT(NBTTagCompound tagCompound) {
            tagCompound.setInteger("Width", this.scatteredFeatureSizeX);
            tagCompound.setInteger("Height", this.scatteredFeatureSizeY);
            tagCompound.setInteger("Depth", this.scatteredFeatureSizeZ);
            tagCompound.setInteger("HPos", this.field_74936_d);
        }

        protected void readStructureFromNBT(NBTTagCompound tagCompound) {
            this.scatteredFeatureSizeX = tagCompound.getInteger("Width");
            this.scatteredFeatureSizeY = tagCompound.getInteger("Height");
            this.scatteredFeatureSizeZ = tagCompound.getInteger("Depth");
            this.field_74936_d = tagCompound.getInteger("HPos");
        }

        protected boolean func_74935_a(World worldIn, StructureBoundingBox p_74935_2_, int p_74935_3_) {
            if (this.field_74936_d >= 0) {
                return true;
            } else {
                int i = 0;
                int j = 0;
                BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

                for (int k = this.boundingBox.minZ; k <= this.boundingBox.maxZ; ++k) {
                    for (int l = this.boundingBox.minX; l <= this.boundingBox.maxX; ++l) {
                        blockpos$mutableblockpos.set(l, 64, k);

                        if (p_74935_2_.isVecInside(blockpos$mutableblockpos)) {
                            i += Math.max(worldIn.getTopSolidOrLiquidBlock(blockpos$mutableblockpos).getY(), worldIn.provider.getAverageGroundLevel());
                            ++j;
                        }
                    }
                }

                if (j == 0) {
                    return false;
                } else {
                    this.field_74936_d = i / j;
                    this.boundingBox.offset(0, this.field_74936_d - this.boundingBox.minY + p_74935_3_, 0);
                    return true;
                }
            }
        }
    }

    public static class JunglePyramid extends ComponentScatteredFeaturePieces.Feature {
        private boolean placedMainChest;
        private boolean placedHiddenChest;
        private boolean placedTrap1;
        private boolean placedTrap2;
        private static final List<WeightedRandomChestContent> field_175816_i = Lists.newArrayList(new WeightedRandomChestContent(Items.DIAMOND, 0, 1, 3, 3), new WeightedRandomChestContent(Items.IRON_INGOT, 0, 1, 5, 10), new WeightedRandomChestContent(Items.GOLD_INGOT, 0, 2, 7, 15), new WeightedRandomChestContent(Items.EMERALD, 0, 1, 3, 2), new WeightedRandomChestContent(Items.BONE, 0, 4, 6, 20), new WeightedRandomChestContent(Items.ROTTEN_FLESH, 0, 3, 7, 16), new WeightedRandomChestContent(Items.SADDLE, 0, 1, 1, 3), new WeightedRandomChestContent(Items.IRON_HORSE_ARMOR, 0, 1, 1, 1), new WeightedRandomChestContent(Items.GOLDEN_HORSE_ARMOR, 0, 1, 1, 1), new WeightedRandomChestContent(Items.DIAMOND_HORSE_ARMOR, 0, 1, 1, 1));
        private static final List<WeightedRandomChestContent> field_175815_j = Lists.newArrayList(new WeightedRandomChestContent(Items.ARROW, 0, 2, 7, 30));
        private static final ComponentScatteredFeaturePieces.JunglePyramid.Stones junglePyramidsRandomScatteredStones = new ComponentScatteredFeaturePieces.JunglePyramid.Stones();

        public JunglePyramid() {
        }

        public JunglePyramid(Random p_i2064_1_, int p_i2064_2_, int p_i2064_3_) {
            super(p_i2064_1_, p_i2064_2_, 64, p_i2064_3_, 12, 10, 15);
        }

        protected void writeStructureToNBT(NBTTagCompound tagCompound) {
            super.writeStructureToNBT(tagCompound);
            tagCompound.setBoolean("placedMainChest", this.placedMainChest);
            tagCompound.setBoolean("placedHiddenChest", this.placedHiddenChest);
            tagCompound.setBoolean("placedTrap1", this.placedTrap1);
            tagCompound.setBoolean("placedTrap2", this.placedTrap2);
        }

        protected void readStructureFromNBT(NBTTagCompound tagCompound) {
            super.readStructureFromNBT(tagCompound);
            this.placedMainChest = tagCompound.getBoolean("placedMainChest");
            this.placedHiddenChest = tagCompound.getBoolean("placedHiddenChest");
            this.placedTrap1 = tagCompound.getBoolean("placedTrap1");
            this.placedTrap2 = tagCompound.getBoolean("placedTrap2");
        }

        public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox structureBoundingBoxIn) {
            if (!this.func_74935_a(worldIn, structureBoundingBoxIn, 0)) {
                return false;
            } else {
                int i = this.getMetadataWithOffset(Blocks.STONE_STAIRS, 3);
                int j = this.getMetadataWithOffset(Blocks.STONE_STAIRS, 2);
                int k = this.getMetadataWithOffset(Blocks.STONE_STAIRS, 0);
                int l = this.getMetadataWithOffset(Blocks.STONE_STAIRS, 1);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 0, -4, 0, this.scatteredFeatureSizeX - 1, 0, this.scatteredFeatureSizeZ - 1, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 2, 1, 2, 9, 2, 2, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 2, 1, 12, 9, 2, 12, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 2, 1, 3, 2, 2, 11, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 9, 1, 3, 9, 2, 11, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 1, 3, 1, 10, 6, 1, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 1, 3, 13, 10, 6, 13, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 1, 3, 2, 1, 6, 12, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 10, 3, 2, 10, 6, 12, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 2, 3, 2, 9, 3, 12, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 2, 6, 2, 9, 6, 12, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 3, 7, 3, 8, 7, 11, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 4, 8, 4, 7, 8, 10, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithAir(worldIn, structureBoundingBoxIn, 3, 1, 3, 8, 2, 11);
                this.fillWithAir(worldIn, structureBoundingBoxIn, 4, 3, 6, 7, 3, 9);
                this.fillWithAir(worldIn, structureBoundingBoxIn, 2, 4, 2, 9, 5, 12);
                this.fillWithAir(worldIn, structureBoundingBoxIn, 4, 6, 5, 7, 6, 9);
                this.fillWithAir(worldIn, structureBoundingBoxIn, 5, 7, 6, 6, 7, 8);
                this.fillWithAir(worldIn, structureBoundingBoxIn, 5, 1, 2, 6, 2, 2);
                this.fillWithAir(worldIn, structureBoundingBoxIn, 5, 2, 12, 6, 2, 12);
                this.fillWithAir(worldIn, structureBoundingBoxIn, 5, 5, 1, 6, 5, 1);
                this.fillWithAir(worldIn, structureBoundingBoxIn, 5, 5, 13, 6, 5, 13);
                this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 1, 5, 5, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 10, 5, 5, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 1, 5, 9, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 10, 5, 9, structureBoundingBoxIn);

                for (int i1 = 0; i1 <= 14; i1 += 14) {
                    this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 2, 4, i1, 2, 5, i1, false, randomIn, junglePyramidsRandomScatteredStones);
                    this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 4, 4, i1, 4, 5, i1, false, randomIn, junglePyramidsRandomScatteredStones);
                    this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 7, 4, i1, 7, 5, i1, false, randomIn, junglePyramidsRandomScatteredStones);
                    this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 9, 4, i1, 9, 5, i1, false, randomIn, junglePyramidsRandomScatteredStones);
                }

                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 5, 6, 0, 6, 6, 0, false, randomIn, junglePyramidsRandomScatteredStones);

                for (int k1 = 0; k1 <= 11; k1 += 11) {
                    for (int j1 = 2; j1 <= 12; j1 += 2) {
                        this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, k1, 4, j1, k1, 5, j1, false, randomIn, junglePyramidsRandomScatteredStones);
                    }

                    this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, k1, 6, 5, k1, 6, 5, false, randomIn, junglePyramidsRandomScatteredStones);
                    this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, k1, 6, 9, k1, 6, 9, false, randomIn, junglePyramidsRandomScatteredStones);
                }

                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 2, 7, 2, 2, 9, 2, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 9, 7, 2, 9, 9, 2, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 2, 7, 12, 2, 9, 12, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 9, 7, 12, 9, 9, 12, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 4, 9, 4, 4, 9, 4, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 7, 9, 4, 7, 9, 4, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 4, 9, 10, 4, 9, 10, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 7, 9, 10, 7, 9, 10, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 5, 9, 7, 6, 9, 7, false, randomIn, junglePyramidsRandomScatteredStones);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(i), 5, 9, 6, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(i), 6, 9, 6, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(j), 5, 9, 8, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(j), 6, 9, 8, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(i), 4, 0, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(i), 5, 0, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(i), 6, 0, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(i), 7, 0, 0, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(i), 4, 1, 8, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(i), 4, 2, 9, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(i), 4, 3, 10, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(i), 7, 1, 8, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(i), 7, 2, 9, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(i), 7, 3, 10, structureBoundingBoxIn);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 4, 1, 9, 4, 1, 9, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 7, 1, 9, 7, 1, 9, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 4, 1, 10, 7, 2, 10, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 5, 4, 5, 6, 4, 5, false, randomIn, junglePyramidsRandomScatteredStones);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(k), 4, 4, 5, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(l), 7, 4, 5, structureBoundingBoxIn);

                for (int l1 = 0; l1 < 4; ++l1) {
                    this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(j), 5, -l1, 6 + l1, structureBoundingBoxIn);
                    this.setBlockState(worldIn, Blocks.STONE_STAIRS.getStateFromMeta(j), 6, -l1, 6 + l1, structureBoundingBoxIn);
                    this.fillWithAir(worldIn, structureBoundingBoxIn, 5, -l1, 7 + l1, 6, -l1, 9 + l1);
                }

                this.fillWithAir(worldIn, structureBoundingBoxIn, 1, -3, 12, 10, -1, 13);
                this.fillWithAir(worldIn, structureBoundingBoxIn, 1, -3, 1, 3, -1, 13);
                this.fillWithAir(worldIn, structureBoundingBoxIn, 1, -3, 1, 9, -1, 5);

                for (int i2 = 1; i2 <= 13; i2 += 2) {
                    this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 1, -3, i2, 1, -2, i2, false, randomIn, junglePyramidsRandomScatteredStones);
                }

                for (int j2 = 2; j2 <= 12; j2 += 2) {
                    this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 1, -1, j2, 3, -1, j2, false, randomIn, junglePyramidsRandomScatteredStones);
                }

                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 2, -2, 1, 5, -2, 1, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 7, -2, 1, 9, -2, 1, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 6, -3, 1, 6, -3, 1, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 6, -1, 1, 6, -1, 1, false, randomIn, junglePyramidsRandomScatteredStones);
                this.setBlockState(worldIn, Blocks.TRIPWIRE_HOOK.getStateFromMeta(this.getMetadataWithOffset(Blocks.TRIPWIRE_HOOK, Direction.EAST.getHorizontalIndex())).withProperty(BlockTripWireHook.ATTACHED, Boolean.TRUE), 1, -3, 8, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.TRIPWIRE_HOOK.getStateFromMeta(this.getMetadataWithOffset(Blocks.TRIPWIRE_HOOK, Direction.WEST.getHorizontalIndex())).withProperty(BlockTripWireHook.ATTACHED, Boolean.TRUE), 4, -3, 8, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.TRIPWIRE.getDefaultState().withProperty(BlockTripWire.ATTACHED, Boolean.TRUE), 2, -3, 8, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.TRIPWIRE.getDefaultState().withProperty(BlockTripWire.ATTACHED, Boolean.TRUE), 3, -3, 8, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.REDSTONE_WIRE.getDefaultState(), 5, -3, 7, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.REDSTONE_WIRE.getDefaultState(), 5, -3, 6, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.REDSTONE_WIRE.getDefaultState(), 5, -3, 5, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.REDSTONE_WIRE.getDefaultState(), 5, -3, 4, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.REDSTONE_WIRE.getDefaultState(), 5, -3, 3, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.REDSTONE_WIRE.getDefaultState(), 5, -3, 2, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.REDSTONE_WIRE.getDefaultState(), 5, -3, 1, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.REDSTONE_WIRE.getDefaultState(), 4, -3, 1, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 3, -3, 1, structureBoundingBoxIn);

                if (!this.placedTrap1) {
                    this.placedTrap1 = this.generateDispenserContents(worldIn, structureBoundingBoxIn, randomIn, 3, -2, 1, Direction.NORTH.getIndex(), field_175815_j, 2);
                }

                this.setBlockState(worldIn, Blocks.VINE.getStateFromMeta(15), 3, -2, 2, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.TRIPWIRE_HOOK.getStateFromMeta(this.getMetadataWithOffset(Blocks.TRIPWIRE_HOOK, Direction.NORTH.getHorizontalIndex())).withProperty(BlockTripWireHook.ATTACHED, Boolean.TRUE), 7, -3, 1, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.TRIPWIRE_HOOK.getStateFromMeta(this.getMetadataWithOffset(Blocks.TRIPWIRE_HOOK, Direction.SOUTH.getHorizontalIndex())).withProperty(BlockTripWireHook.ATTACHED, Boolean.TRUE), 7, -3, 5, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.TRIPWIRE.getDefaultState().withProperty(BlockTripWire.ATTACHED, Boolean.TRUE), 7, -3, 2, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.TRIPWIRE.getDefaultState().withProperty(BlockTripWire.ATTACHED, Boolean.TRUE), 7, -3, 3, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.TRIPWIRE.getDefaultState().withProperty(BlockTripWire.ATTACHED, Boolean.TRUE), 7, -3, 4, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.REDSTONE_WIRE.getDefaultState(), 8, -3, 6, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.REDSTONE_WIRE.getDefaultState(), 9, -3, 6, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.REDSTONE_WIRE.getDefaultState(), 9, -3, 5, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 9, -3, 4, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.REDSTONE_WIRE.getDefaultState(), 9, -2, 4, structureBoundingBoxIn);

                if (!this.placedTrap2) {
                    this.placedTrap2 = this.generateDispenserContents(worldIn, structureBoundingBoxIn, randomIn, 9, -2, 3, Direction.WEST.getIndex(), field_175815_j, 2);
                }

                this.setBlockState(worldIn, Blocks.VINE.getStateFromMeta(15), 8, -1, 3, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.VINE.getStateFromMeta(15), 8, -2, 3, structureBoundingBoxIn);

                if (!this.placedMainChest) {
                    this.placedMainChest = this.generateChestContents(worldIn, structureBoundingBoxIn, randomIn, 8, -3, 3, WeightedRandomChestContent.func_177629_a(field_175816_i, Items.ENCHANTED_BOOK.getRandom(randomIn)), 2 + randomIn.nextInt(5));
                }

                this.setBlockState(worldIn, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 9, -3, 2, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 8, -3, 1, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 4, -3, 5, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 5, -2, 5, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 5, -1, 5, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 6, -3, 5, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 7, -2, 5, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 7, -1, 5, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 8, -3, 5, structureBoundingBoxIn);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 9, -1, 1, 9, -1, 5, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithAir(worldIn, structureBoundingBoxIn, 8, -3, 8, 10, -1, 10);
                this.setBlockState(worldIn, Blocks.STONEBRICK.getStateFromMeta(BlockStoneBrick.CHISELED_META), 8, -2, 11, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONEBRICK.getStateFromMeta(BlockStoneBrick.CHISELED_META), 9, -2, 11, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STONEBRICK.getStateFromMeta(BlockStoneBrick.CHISELED_META), 10, -2, 11, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.LEVER.getStateFromMeta(BlockLever.getMetadataForFacing(Direction.getFront(this.getMetadataWithOffset(Blocks.LEVER, Direction.NORTH.getIndex())))), 8, -2, 12, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.LEVER.getStateFromMeta(BlockLever.getMetadataForFacing(Direction.getFront(this.getMetadataWithOffset(Blocks.LEVER, Direction.NORTH.getIndex())))), 9, -2, 12, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.LEVER.getStateFromMeta(BlockLever.getMetadataForFacing(Direction.getFront(this.getMetadataWithOffset(Blocks.LEVER, Direction.NORTH.getIndex())))), 10, -2, 12, structureBoundingBoxIn);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 8, -3, 8, 8, -3, 10, false, randomIn, junglePyramidsRandomScatteredStones);
                this.fillWithRandomizedBlocks(worldIn, structureBoundingBoxIn, 10, -3, 8, 10, -3, 10, false, randomIn, junglePyramidsRandomScatteredStones);
                this.setBlockState(worldIn, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 10, -2, 9, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.REDSTONE_WIRE.getDefaultState(), 8, -2, 9, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.REDSTONE_WIRE.getDefaultState(), 8, -2, 10, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.REDSTONE_WIRE.getDefaultState(), 10, -1, 9, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STICKY_PISTON.getStateFromMeta(Direction.UP.getIndex()), 9, -2, 8, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STICKY_PISTON.getStateFromMeta(this.getMetadataWithOffset(Blocks.STICKY_PISTON, Direction.WEST.getIndex())), 10, -2, 8, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.STICKY_PISTON.getStateFromMeta(this.getMetadataWithOffset(Blocks.STICKY_PISTON, Direction.WEST.getIndex())), 10, -1, 8, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.UNPOWERED_REPEATER.getStateFromMeta(this.getMetadataWithOffset(Blocks.UNPOWERED_REPEATER, Direction.NORTH.getHorizontalIndex())), 10, -2, 10, structureBoundingBoxIn);

                if (!this.placedHiddenChest) {
                    this.placedHiddenChest = this.generateChestContents(worldIn, structureBoundingBoxIn, randomIn, 9, -3, 10, WeightedRandomChestContent.func_177629_a(field_175816_i, Items.ENCHANTED_BOOK.getRandom(randomIn)), 2 + randomIn.nextInt(5));
                }

                return true;
            }
        }

        static class Stones extends StructureComponent.BlockSelector {
            private Stones() {
            }

            public void selectBlocks(Random rand, int x, int y, int z, boolean p_75062_5_) {
                if (rand.nextFloat() < 0.4F) {
                    this.blockstate = Blocks.COBBLESTONE.getDefaultState();
                } else {
                    this.blockstate = Blocks.MOSSY_COBBLESTONE.getDefaultState();
                }
            }
        }
    }

    public static class SwampHut extends ComponentScatteredFeaturePieces.Feature {
        private boolean hasWitch;

        public SwampHut() {
        }

        public SwampHut(Random p_i2066_1_, int p_i2066_2_, int p_i2066_3_) {
            super(p_i2066_1_, p_i2066_2_, 64, p_i2066_3_, 7, 7, 9);
        }

        protected void writeStructureToNBT(NBTTagCompound tagCompound) {
            super.writeStructureToNBT(tagCompound);
            tagCompound.setBoolean("Witch", this.hasWitch);
        }

        protected void readStructureFromNBT(NBTTagCompound tagCompound) {
            super.readStructureFromNBT(tagCompound);
            this.hasWitch = tagCompound.getBoolean("Witch");
        }

        public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox structureBoundingBoxIn) {
            if (!this.func_74935_a(worldIn, structureBoundingBoxIn, 0)) {
                return false;
            } else {
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, 1, 1, 1, 5, 1, 7, Blocks.PLANKS.getStateFromMeta(BlockPlanks.WoodType.SPRUCE.getMetadata()), Blocks.PLANKS.getStateFromMeta(BlockPlanks.WoodType.SPRUCE.getMetadata()), false);
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, 1, 4, 2, 5, 4, 7, Blocks.PLANKS.getStateFromMeta(BlockPlanks.WoodType.SPRUCE.getMetadata()), Blocks.PLANKS.getStateFromMeta(BlockPlanks.WoodType.SPRUCE.getMetadata()), false);
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, 2, 1, 0, 4, 1, 0, Blocks.PLANKS.getStateFromMeta(BlockPlanks.WoodType.SPRUCE.getMetadata()), Blocks.PLANKS.getStateFromMeta(BlockPlanks.WoodType.SPRUCE.getMetadata()), false);
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, 2, 2, 2, 3, 3, 2, Blocks.PLANKS.getStateFromMeta(BlockPlanks.WoodType.SPRUCE.getMetadata()), Blocks.PLANKS.getStateFromMeta(BlockPlanks.WoodType.SPRUCE.getMetadata()), false);
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, 1, 2, 3, 1, 3, 6, Blocks.PLANKS.getStateFromMeta(BlockPlanks.WoodType.SPRUCE.getMetadata()), Blocks.PLANKS.getStateFromMeta(BlockPlanks.WoodType.SPRUCE.getMetadata()), false);
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, 5, 2, 3, 5, 3, 6, Blocks.PLANKS.getStateFromMeta(BlockPlanks.WoodType.SPRUCE.getMetadata()), Blocks.PLANKS.getStateFromMeta(BlockPlanks.WoodType.SPRUCE.getMetadata()), false);
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, 2, 2, 7, 4, 3, 7, Blocks.PLANKS.getStateFromMeta(BlockPlanks.WoodType.SPRUCE.getMetadata()), Blocks.PLANKS.getStateFromMeta(BlockPlanks.WoodType.SPRUCE.getMetadata()), false);
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, 1, 0, 2, 1, 3, 2, Blocks.LOG.getDefaultState(), Blocks.LOG.getDefaultState(), false);
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, 5, 0, 2, 5, 3, 2, Blocks.LOG.getDefaultState(), Blocks.LOG.getDefaultState(), false);
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, 1, 0, 7, 1, 3, 7, Blocks.LOG.getDefaultState(), Blocks.LOG.getDefaultState(), false);
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, 5, 0, 7, 5, 3, 7, Blocks.LOG.getDefaultState(), Blocks.LOG.getDefaultState(), false);
                this.setBlockState(worldIn, Blocks.OAK_FENCE.getDefaultState(), 2, 3, 2, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.OAK_FENCE.getDefaultState(), 3, 3, 7, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 1, 3, 4, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 5, 3, 4, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), 5, 3, 5, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.FLOWER_POT.getDefaultState().withProperty(BlockFlowerPot.CONTENTS, BlockFlowerPot.FlowerType.MUSHROOM_RED), 1, 3, 5, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.CRAFTING_TABLE.getDefaultState(), 3, 2, 6, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.CAULDRON.getDefaultState(), 4, 2, 6, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.OAK_FENCE.getDefaultState(), 1, 2, 1, structureBoundingBoxIn);
                this.setBlockState(worldIn, Blocks.OAK_FENCE.getDefaultState(), 5, 2, 1, structureBoundingBoxIn);
                int i = this.getMetadataWithOffset(Blocks.OAK_STAIRS, 3);
                int j = this.getMetadataWithOffset(Blocks.OAK_STAIRS, 1);
                int k = this.getMetadataWithOffset(Blocks.OAK_STAIRS, 0);
                int l = this.getMetadataWithOffset(Blocks.OAK_STAIRS, 2);
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, 0, 4, 1, 6, 4, 1, Blocks.SPRUCE_STAIRS.getStateFromMeta(i), Blocks.SPRUCE_STAIRS.getStateFromMeta(i), false);
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, 0, 4, 2, 0, 4, 7, Blocks.SPRUCE_STAIRS.getStateFromMeta(k), Blocks.SPRUCE_STAIRS.getStateFromMeta(k), false);
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, 6, 4, 2, 6, 4, 7, Blocks.SPRUCE_STAIRS.getStateFromMeta(j), Blocks.SPRUCE_STAIRS.getStateFromMeta(j), false);
                this.fillWithBlocks(worldIn, structureBoundingBoxIn, 0, 4, 8, 6, 4, 8, Blocks.SPRUCE_STAIRS.getStateFromMeta(l), Blocks.SPRUCE_STAIRS.getStateFromMeta(l), false);

                for (int i1 = 2; i1 <= 7; i1 += 5) {
                    for (int j1 = 1; j1 <= 5; j1 += 4) {
                        this.replaceAirAndLiquidDownwards(worldIn, Blocks.LOG.getDefaultState(), j1, -1, i1, structureBoundingBoxIn);
                    }
                }

                if (!this.hasWitch) {
                    int l1 = this.getXWithOffset(2, 5);
                    int i2 = this.getYWithOffset(2);
                    int k1 = this.getZWithOffset(2, 5);

                    if (structureBoundingBoxIn.isVecInside(new BlockPos(l1, i2, k1))) {
                        this.hasWitch = true;
                        EntityWitch entitywitch = new EntityWitch(worldIn);
                        entitywitch.setLocationAndAngles(l1 + 0.5D, i2, k1 + 0.5D, 0.0F, 0.0F);
                        entitywitch.onInitialSpawn(worldIn.getDifficultyForLocation(new BlockPos(l1, i2, k1)), null);
                        worldIn.spawnEntityInWorld(entitywitch);
                    }
                }

                return true;
            }
        }
    }
}
