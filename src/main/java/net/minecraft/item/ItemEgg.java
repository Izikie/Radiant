package net.minecraft.item;

import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;

public class ItemEgg extends Item {
    public ItemEgg() {
        this.maxStackSize = 16;
        this.setCreativeTab(CreativeTabs.TAB_MATERIALS);
    }

    public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn) {
        if (!playerIn.capabilities.isCreativeMode) {
            --itemStackIn.stackSize;
        }

        worldIn.playSoundAtEntity(playerIn, "random.bow", 0.5F, 0.4F / (ITEM_RAND.nextFloat() * 0.4F + 0.8F));

        if (!worldIn.isRemote) {
            worldIn.spawnEntityInWorld(new EntityEgg(worldIn, playerIn));
        }

        playerIn.triggerAchievement(StatList.OBJECT_USE_STATS[Item.getIdFromItem(this)]);
        return itemStackIn;
    }
}
