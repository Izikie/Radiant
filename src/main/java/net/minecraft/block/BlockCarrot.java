package net.minecraft.block;

import net.minecraft.init.Items;
import net.minecraft.item.Item;

public class BlockCarrot extends BlockCrops {
    @Override
    protected Item getSeed() {
        return Items.CARROT;
    }

    @Override
    protected Item getCrop() {
        return Items.CARROT;
    }
}
