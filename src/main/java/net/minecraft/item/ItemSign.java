package net.minecraft.item;

import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class ItemSign extends Item {
    public ItemSign() {
        this.maxStackSize = 16;
        this.setCreativeTab(CreativeTabs.TAB_DECORATIONS);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, Direction side, float hitX, float hitY, float hitZ) {
        if (side == Direction.DOWN) {
            return false;
        } else if (!worldIn.getBlockState(pos).getBlock().getMaterial().isSolid()) {
            return false;
        } else {
            pos = pos.offset(side);

            if (!playerIn.canPlayerEdit(pos, side, stack)) {
                return false;
            } else if (!Blocks.STANDING_SIGN.canPlaceBlockAt(worldIn, pos)) {
                return false;
            } else if (worldIn.isRemote) {
                return true;
            } else {
                if (side == Direction.UP) {
                    int i = MathHelper.floor(((playerIn.rotationYaw + 180.0F) * 16.0F / 360.0F) + 0.5D) & 15;
                    worldIn.setBlockState(pos, Blocks.STANDING_SIGN.getDefaultState().withProperty(BlockStandingSign.ROTATION, i), 3);
                } else {
                    worldIn.setBlockState(pos, Blocks.WALL_SIGN.getDefaultState().withProperty(BlockWallSign.FACING, side), 3);
                }

                --stack.stackSize;
                TileEntity tileentity = worldIn.getTileEntity(pos);

                if (tileentity instanceof TileEntitySign tileEntitySign && !ItemBlock.setTileEntityNBT(worldIn, playerIn, pos, stack)) {
                    playerIn.openEditSign(tileEntitySign);
                }

                return true;
            }
        }
    }
}
