package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;

public class BlockHardenedClay extends Block {
    public BlockHardenedClay() {
        super(Material.ROCK);
        this.setCreativeTab(CreativeTabs.TAB_BLOCK);
    }

    public MapColor getMapColor(IBlockState state) {
        return MapColor.ADOBE_COLOR;
    }
}
