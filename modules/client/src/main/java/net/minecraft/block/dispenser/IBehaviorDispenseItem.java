package net.minecraft.block.dispenser;

import net.minecraft.item.ItemStack;

public interface IBehaviorDispenseItem {
    IBehaviorDispenseItem itemDispenseBehaviorProvider = (source, stack) -> stack;

    ItemStack dispense(IBlockSource source, ItemStack stack);
}
