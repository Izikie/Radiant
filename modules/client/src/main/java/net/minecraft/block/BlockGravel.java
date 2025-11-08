package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

import java.util.Random;

public class BlockGravel extends BlockFalling {
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        if (fortune > 3) {
            fortune = 3;
        }

        return rand.nextInt(10 - fortune * 3) == 0 ? Items.FLINT : Item.getItemFromBlock(this);
    }

    @Override
    public MapColor getMapColor(IBlockState state) {
        return MapColor.STONE_COLOR;
    }
}
