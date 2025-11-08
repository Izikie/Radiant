package net.minecraft.item;

import net.minecraft.block.material.Material;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

public class ItemFlintAndSteel extends Item {
    public ItemFlintAndSteel() {
        this.maxStackSize = 1;
        this.setMaxDamage(64);
        this.setCreativeTab(CreativeTabs.TAB_TOOLS);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, Direction side, float hitX, float hitY, float hitZ) {
        pos = pos.offset(side);

        if (!playerIn.canPlayerEdit(pos, side, stack)) {
            return false;
        } else {
            if (worldIn.getBlockState(pos).getBlock().getMaterial() == Material.AIR) {
                worldIn.playSoundEffect(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, "fire.ignite", 1.0F, ITEM_RAND.nextFloat() * 0.4F + 0.8F);
                worldIn.setBlockState(pos, Blocks.FIRE.getDefaultState());
            }

            stack.damageItem(1, playerIn);
            return true;
        }
    }
}
