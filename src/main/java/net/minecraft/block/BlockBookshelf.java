package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

import java.util.Random;

public class BlockBookshelf extends Block {
    public BlockBookshelf() {
        super(Material.WOOD);
        this.setCreativeTab(CreativeTabs.TAB_BLOCK);
    }

    public int quantityDropped(Random random) {
        return 3;
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.BOOK;
    }
}
