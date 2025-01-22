package net.minecraft.block;

import net.minecraft.init.Items;
import net.minecraft.item.Item;

public class BlockCarrot extends BlockCrops {
    protected Item getSeed() {
        return Items.CARROT;
    }

    protected Item getCrop() {
        return Items.CARROT;
    }
}
