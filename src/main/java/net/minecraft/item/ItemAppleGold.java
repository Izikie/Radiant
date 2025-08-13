package net.minecraft.item;

import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import java.util.List;

public class ItemAppleGold extends ItemFood {
    public ItemAppleGold(int amount, float saturation, boolean isWolfFood) {
        super(amount, saturation, isWolfFood);
        this.setHasSubtypes(true);
    }

    public boolean hasEffect(ItemStack stack) {
        return stack.getMetadata() > 0;
    }

    public Rarity getRarity(ItemStack stack) {
        return stack.getMetadata() == 0 ? Rarity.RARE : Rarity.EPIC;
    }

    protected void onFoodEaten(ItemStack stack, World worldIn, EntityPlayer player) {
        if (!worldIn.isRemote) {
            player.addPotionEffect(new PotionEffect(Potion.ABSORPTION.id, 2400, 0));
        }

        if (stack.getMetadata() > 0) {
            if (!worldIn.isRemote) {
                player.addPotionEffect(new PotionEffect(Potion.REGENERATION.id, 600, 4));
                player.addPotionEffect(new PotionEffect(Potion.RESISTANCE.id, 6000, 0));
                player.addPotionEffect(new PotionEffect(Potion.FIRE_RESISTANCE.id, 6000, 0));
            }
        } else {
            super.onFoodEaten(stack, worldIn, player);
        }
    }

    public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems) {
        subItems.add(new ItemStack(itemIn, 1, 0));
        subItems.add(new ItemStack(itemIn, 1, 1));
    }
}
