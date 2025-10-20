package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class ItemShears extends Item {
    public ItemShears() {
        this.setMaxStackSize(1);
        this.setMaxDamage(238);
        this.setCreativeTab(CreativeTabs.TAB_TOOLS);
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World worldIn, Block blockIn, BlockPos pos, EntityLivingBase playerIn) {
        if (blockIn.getMaterial() != Material.LEAVES && blockIn != Blocks.WEB && blockIn != Blocks.TALL_GRASS && blockIn != Blocks.VINE && blockIn != Blocks.TRIPWIRE && blockIn != Blocks.WOOL) {
            return super.onBlockDestroyed(stack, worldIn, blockIn, pos, playerIn);
        } else {
            stack.damageItem(1, playerIn);
            return true;
        }
    }

    @Override
    public boolean canHarvestBlock(Block blockIn) {
        return blockIn == Blocks.WEB || blockIn == Blocks.REDSTONE_WIRE || blockIn == Blocks.TRIPWIRE;
    }

    @Override
    public float getStrVsBlock(ItemStack stack, Block state) {
        return state != Blocks.WEB && state.getMaterial() != Material.LEAVES ? (state == Blocks.WOOL ? 5.0F : super.getStrVsBlock(stack, state)) : 15.0F;
    }
}
