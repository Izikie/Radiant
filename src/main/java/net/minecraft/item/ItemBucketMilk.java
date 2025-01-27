package net.minecraft.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;

public class ItemBucketMilk extends Item {
    public ItemBucketMilk() {
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabs.TAB_MISC);
    }

    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityPlayer playerIn) {
        if (!playerIn.capabilities.isCreativeMode) {
            --stack.stackSize;
        }

        if (!worldIn.isRemote) {
            playerIn.clearActivePotions();
        }

        playerIn.triggerAchievement(StatList.OBJECT_USE_STATS[Item.getIdFromItem(this)]);
        return stack.stackSize <= 0 ? new ItemStack(Items.BUCKET) : stack;
    }

    public int getMaxItemUseDuration(ItemStack stack) {
        return 32;
    }

    public UseAction getItemUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn) {
        playerIn.setItemInUse(itemStackIn, this.getMaxItemUseDuration(itemStackIn));
        return itemStackIn;
    }
}
