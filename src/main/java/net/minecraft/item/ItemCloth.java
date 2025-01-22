package net.minecraft.item;

import net.minecraft.block.Block;

public class ItemCloth extends ItemBlock {
    public ItemCloth(Block block) {
        super(block);
        this.setMaxDamage(0);
        this.setHasSubtypes(true);
    }

    public int getMetadata(int damage) {
        return damage;
    }

    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + DyeColor.byMetadata(stack.getMetadata()).getUnlocalizedName();
    }
}
