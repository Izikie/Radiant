package net.minecraft.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;

public interface ISidedInventory extends IInventory {
    int[] getSlotsForFace(Direction side);

    boolean canInsertItem(int index, ItemStack itemStackIn, Direction direction);

    boolean canExtractItem(int index, ItemStack stack, Direction direction);
}
