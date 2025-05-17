package net.minecraft.world.gen.feature;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public abstract class WorldGenAbstractTree extends WorldGenerator {
    public WorldGenAbstractTree(boolean notify) {
        super(notify);
    }

    protected boolean func_150523_a(Block p_150523_1_) {
        Material material = p_150523_1_.getMaterial();
        return material == Material.AIR || material == Material.LEAVES || p_150523_1_ == Blocks.GRASS || p_150523_1_ == Blocks.DIRT || p_150523_1_ == Blocks.LOG || p_150523_1_ == Blocks.LOG_2 || p_150523_1_ == Blocks.SAPLING || p_150523_1_ == Blocks.VINE;
    }

    public void func_180711_a(World worldIn, Random p_180711_2_, BlockPos p_180711_3_) {
    }

    protected void func_175921_a(World worldIn, BlockPos pos) {
        if (worldIn.getBlockState(pos).getBlock() != Blocks.DIRT) {
            this.setBlockAndNotifyAdequately(worldIn, pos, Blocks.DIRT.getDefaultState());
        }
    }
}
