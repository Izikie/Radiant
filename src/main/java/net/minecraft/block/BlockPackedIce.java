package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.item.creativetab.CreativeTabs;

import java.util.Random;

public class BlockPackedIce extends Block {
    public BlockPackedIce() {
        super(Material.PACKED_ICE);
        this.slipperiness = 0.98F;
        this.setCreativeTab(CreativeTabs.TAB_BLOCK);
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }
}
