package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public class BlockGlowstone extends Block {
    public BlockGlowstone(Material materialIn) {
        super(materialIn);
        this.setCreativeTab(CreativeTabs.TAB_BLOCK);
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        return MathHelper.clamp(this.quantityDropped(random) + random.nextInt(fortune + 1), 1, 4);
    }

    @Override
    public int quantityDropped(Random random) {
        return 2 + random.nextInt(3);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.GLOWSTONE_DUST;
    }

    @Override
    public MapColor getMapColor(IBlockState state) {
        return MapColor.SAND_COLOR;
    }
}
