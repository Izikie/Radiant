package net.minecraft.world.gen.feature;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class WorldGenIcePath extends WorldGenerator {
    private final Block block = Blocks.PACKED_ICE;
    private final int basePathWidth;

    public WorldGenIcePath(int p_i45454_1_) {
        this.basePathWidth = p_i45454_1_;
    }

    public boolean generate(World worldIn, Random rand, BlockPos position) {
        while (worldIn.isAirBlock(position) && position.getY() > 2) {
            position = position.down();
        }

        if (worldIn.getBlockState(position).getBlock() != Blocks.SNOW) {
            return false;
        } else {
            int i = rand.nextInt(this.basePathWidth - 2) + 2;
            int j = 1;

            for (int k = position.getX() - i; k <= position.getX() + i; ++k) {
                for (int l = position.getZ() - i; l <= position.getZ() + i; ++l) {
                    int i1 = k - position.getX();
                    int j1 = l - position.getZ();

                    if (i1 * i1 + j1 * j1 <= i * i) {
                        for (int k1 = position.getY() - j; k1 <= position.getY() + j; ++k1) {
                            BlockPos blockpos = new BlockPos(k, k1, l);
                            Block block = worldIn.getBlockState(blockpos).getBlock();

                            if (block == Blocks.DIRT || block == Blocks.SNOW || block == Blocks.ICE) {
                                worldIn.setBlockState(blockpos, this.block.getDefaultState(), 2);
                            }
                        }
                    }
                }
            }

            return true;
        }
    }
}
