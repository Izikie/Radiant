package net.minecraft.world.gen.feature;

import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class WorldGenReed extends WorldGenerator {
    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        for (int i = 0; i < 20; ++i) {
            BlockPos blockpos = position.add(rand.nextInt(4) - rand.nextInt(4), 0, rand.nextInt(4) - rand.nextInt(4));

            if (worldIn.isAirBlock(blockpos)) {
                BlockPos blockpos1 = blockpos.down();

                if (worldIn.getBlockState(blockpos1.west()).getBlock().getMaterial() == Material.WATER || worldIn.getBlockState(blockpos1.east()).getBlock().getMaterial() == Material.WATER || worldIn.getBlockState(blockpos1.north()).getBlock().getMaterial() == Material.WATER || worldIn.getBlockState(blockpos1.south()).getBlock().getMaterial() == Material.WATER) {
                    int j = 2 + rand.nextInt(rand.nextInt(3) + 1);

                    for (int k = 0; k < j; ++k) {
                        if (Blocks.REEDS.canBlockStay(worldIn, blockpos)) {
                            worldIn.setBlockState(blockpos.up(k), Blocks.REEDS.getDefaultState(), 2);
                        }
                    }
                }
            }
        }

        return true;
    }
}
