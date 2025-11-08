package net.minecraft.world.gen.feature;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class WorldGenHellLava extends WorldGenerator {
    private final Block field_150553_a;
    private final boolean field_94524_b;

    public WorldGenHellLava(Block p_i45453_1_, boolean p_i45453_2_) {
        this.field_150553_a = p_i45453_1_;
        this.field_94524_b = p_i45453_2_;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        if (worldIn.getBlockState(position.up()).getBlock() != Blocks.NETHERRACK) {
            return false;
        } else if (worldIn.getBlockState(position).getBlock().getMaterial() != Material.AIR && worldIn.getBlockState(position).getBlock() != Blocks.NETHERRACK) {
            return false;
        } else {
            int i = 0;

            if (worldIn.getBlockState(position.west()).getBlock() == Blocks.NETHERRACK) {
                ++i;
            }

            if (worldIn.getBlockState(position.east()).getBlock() == Blocks.NETHERRACK) {
                ++i;
            }

            if (worldIn.getBlockState(position.north()).getBlock() == Blocks.NETHERRACK) {
                ++i;
            }

            if (worldIn.getBlockState(position.south()).getBlock() == Blocks.NETHERRACK) {
                ++i;
            }

            if (worldIn.getBlockState(position.down()).getBlock() == Blocks.NETHERRACK) {
                ++i;
            }

            int j = 0;

            if (worldIn.isAirBlock(position.west())) {
                ++j;
            }

            if (worldIn.isAirBlock(position.east())) {
                ++j;
            }

            if (worldIn.isAirBlock(position.north())) {
                ++j;
            }

            if (worldIn.isAirBlock(position.south())) {
                ++j;
            }

            if (worldIn.isAirBlock(position.down())) {
                ++j;
            }

            if (!this.field_94524_b && i == 4 && j == 1 || i == 5) {
                worldIn.setBlockState(position, this.field_150553_a.getDefaultState(), 2);
                worldIn.forceBlockUpdateTick(this.field_150553_a, position, rand);
            }

            return true;
        }
    }
}
