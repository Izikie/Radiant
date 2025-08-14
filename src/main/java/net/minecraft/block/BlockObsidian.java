package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;

import java.util.Random;

public class BlockObsidian extends Block {
    public BlockObsidian() {
        super(Material.ROCK);
        this.setCreativeTab(CreativeTabs.TAB_BLOCK);
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(Blocks.OBSIDIAN);
    }

    public MapColor getMapColor(IBlockState state) {
        return MapColor.BLACK_COLOR;
    }
}
