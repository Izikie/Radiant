package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.util.Random;

public class BlockSnowBlock extends Block {
    protected BlockSnowBlock() {
        super(Material.CRAFTED_SNOW);
        this.setTickRandomly(true);
        this.setCreativeTab(CreativeTabs.TAB_BLOCK);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.SNOWBALL;
    }

    @Override
    public int quantityDropped(Random random) {
        return 4;
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (worldIn.getLightFor(LightType.BLOCK, pos) > 11) {
            this.dropBlockAsItem(worldIn, pos, worldIn.getBlockState(pos), 0);
            worldIn.setBlockToAir(pos);
        }
    }
}
