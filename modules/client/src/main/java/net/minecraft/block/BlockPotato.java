package net.minecraft.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class BlockPotato extends BlockCrops {
    @Override
    protected Item getSeed() {
        return Items.POTATO;
    }

    @Override
    protected Item getCrop() {
        return Items.POTATO;
    }

    @Override
    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
        super.dropBlockAsItemWithChance(worldIn, pos, state, chance, fortune);

        if (!worldIn.isRemote) {
            if (state.getValue(AGE) >= 7 && worldIn.rand.nextInt(50) == 0) {
                spawnAsEntity(worldIn, pos, new ItemStack(Items.POISONOUS_POTATO));
            }
        }
    }
}
