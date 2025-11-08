package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.util.RenderLayer;

import java.util.Random;

public class BlockGlass extends BlockBreakable {
    public BlockGlass(Material materialIn, boolean ignoreSimilarity) {
        super(materialIn, ignoreSimilarity);
        this.setCreativeTab(CreativeTabs.TAB_BLOCK);
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public RenderLayer getBlockLayer() {
        return RenderLayer.CUTOUT;
    }

    @Override
    public boolean isFullCube() {
        return false;
    }

    @Override
    protected boolean canSilkHarvest() {
        return true;
    }
}
