package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;

public class BlockNetherBrick extends Block {
    public BlockNetherBrick() {
        super(Material.ROCK);
        this.setCreativeTab(CreativeTabs.TAB_BLOCK);
    }

    @Override
    public MapColor getMapColor(IBlockState state) {
        return MapColor.NETHERRACK_COLOR;
    }
}
