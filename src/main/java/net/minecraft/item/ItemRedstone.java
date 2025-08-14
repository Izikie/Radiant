package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

public class ItemRedstone extends Item {
    public ItemRedstone() {
        this.setCreativeTab(CreativeTabs.TAB_REDSTONE);
    }

    public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, Direction side, float hitX, float hitY, float hitZ) {
        boolean flag = worldIn.getBlockState(pos).getBlock().isReplaceable(worldIn, pos);
        BlockPos blockpos = flag ? pos : pos.offset(side);

        if (!playerIn.canPlayerEdit(blockpos, side, stack)) {
            return false;
        } else {
            Block block = worldIn.getBlockState(blockpos).getBlock();

            if (!worldIn.canBlockBePlaced(block, blockpos, false, side, null, stack)) {
                return false;
            } else if (Blocks.REDSTONE_WIRE.canPlaceBlockAt(worldIn, blockpos)) {
                --stack.stackSize;
                worldIn.setBlockState(blockpos, Blocks.REDSTONE_WIRE.getDefaultState());
                return true;
            } else {
                return false;
            }
        }
    }
}
