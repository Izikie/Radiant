package net.minecraft.item;

import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;

public class ItemCarrotOnAStick extends Item {
    public ItemCarrotOnAStick() {
        this.setCreativeTab(CreativeTabs.TAB_TRANSPORT);
        this.setMaxStackSize(1);
        this.setMaxDamage(25);
    }

    @Override
    public boolean isFull3D() {
        return true;
    }

    @Override
    public boolean shouldRotateAroundWhenRendering() {
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn) {
        if (playerIn.isRiding() && playerIn.ridingEntity instanceof EntityPig entitypig) {

            if (entitypig.getAIControlledByPlayer().isControlledByPlayer() && itemStackIn.getMaxDamage() - itemStackIn.getMetadata() >= 7) {
                entitypig.getAIControlledByPlayer().boostSpeed();
                itemStackIn.damageItem(7, playerIn);

                if (itemStackIn.stackSize == 0) {
                    ItemStack itemstack = new ItemStack(Items.FISHING_ROD);
                    itemstack.setTagCompound(itemStackIn.getTagCompound());
                    return itemstack;
                }
            }
        }

        playerIn.triggerAchievement(StatList.OBJECT_USE_STATS[Item.getIdFromItem(this)]);
        return itemStackIn;
    }
}
