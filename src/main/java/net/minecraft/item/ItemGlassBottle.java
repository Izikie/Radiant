package net.minecraft.item;

import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.stats.StatList;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class ItemGlassBottle extends Item {
    public ItemGlassBottle() {
        this.setCreativeTab(CreativeTabs.tabBrewing);
    }

    public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn) {
        MovingObjectPosition movingobjectposition = this.getMovingObjectPositionFromPlayer(worldIn, playerIn, true);

        if (movingobjectposition != null) {
            if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                BlockPos blockpos = movingobjectposition.getBlockPos();

                if (!worldIn.isBlockModifiable(playerIn, blockpos)) {
                    return itemStackIn;
                }

                if (!playerIn.canPlayerEdit(blockpos.offset(movingobjectposition.sideHit), movingobjectposition.sideHit, itemStackIn)) {
                    return itemStackIn;
                }

                if (worldIn.getBlockState(blockpos).getBlock().getMaterial() == Material.WATER) {
                    --itemStackIn.stackSize;
                    playerIn.triggerAchievement(StatList.OBJECT_USE_STATS[Item.getIdFromItem(this)]);

                    if (itemStackIn.stackSize <= 0) {
                        return new ItemStack(Items.POTION);
                    }

                    if (!playerIn.inventory.addItemStackToInventory(new ItemStack(Items.POTION))) {
                        playerIn.dropPlayerItemWithRandomChoice(new ItemStack(Items.POTION, 1, 0), false);
                    }
                }
            }

        }
        return itemStackIn;
    }
}
