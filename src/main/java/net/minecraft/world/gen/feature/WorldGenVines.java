package net.minecraft.world.gen.feature;

import java.util.Random;

import net.minecraft.block.BlockVine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

public class WorldGenVines extends WorldGenerator {
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        for (; position.getY() < 128; position = position.up()) {
            if (worldIn.isAirBlock(position)) {
                for (Direction enumfacing : Direction.Plane.HORIZONTAL.facings()) {
                    if (Blocks.VINE.canPlaceBlockOnSide(worldIn, position, enumfacing)) {
                        IBlockState iblockstate = Blocks.VINE.getDefaultState().withProperty(BlockVine.NORTH, enumfacing == Direction.NORTH).withProperty(BlockVine.EAST, enumfacing == Direction.EAST).withProperty(BlockVine.SOUTH, enumfacing == Direction.SOUTH).withProperty(BlockVine.WEST, enumfacing == Direction.WEST);
                        worldIn.setBlockState(position, iblockstate, 2);
                        break;
                    }
                }
            } else {
                position = position.add(rand.nextInt(4) - rand.nextInt(4), 0, rand.nextInt(4) - rand.nextInt(4));
            }
        }

        return true;
    }
}
