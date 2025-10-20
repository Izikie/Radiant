package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class ItemBed extends Item {
    public ItemBed() {
        this.setCreativeTab(CreativeTabs.TAB_DECORATIONS);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, Direction side, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        } else if (side != Direction.UP) {
            return false;
        } else {
            IBlockState iblockstate = worldIn.getBlockState(pos);
            Block block = iblockstate.getBlock();
            boolean flag = block.isReplaceable(worldIn, pos);

            if (!flag) {
                pos = pos.up();
            }

            int i = MathHelper.floor((playerIn.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
            Direction enumfacing = Direction.getHorizontal(i);
            BlockPos blockpos = pos.offset(enumfacing);

            if (playerIn.canPlayerEdit(pos, side, stack) && playerIn.canPlayerEdit(blockpos, side, stack)) {
                boolean flag1 = worldIn.getBlockState(blockpos).getBlock().isReplaceable(worldIn, blockpos);
                boolean flag2 = flag || worldIn.isAirBlock(pos);
                boolean flag3 = flag1 || worldIn.isAirBlock(blockpos);

                if (flag2 && flag3 && World.doesBlockHaveSolidTopSurface(worldIn, pos.down()) && World.doesBlockHaveSolidTopSurface(worldIn, blockpos.down())) {
                    IBlockState iblockstate1 = Blocks.BED.getDefaultState().withProperty(BlockBed.OCCUPIED, Boolean.FALSE).withProperty(BlockBed.FACING, enumfacing).withProperty(BlockBed.PART, BlockBed.EnumPartType.FOOT);

                    if (worldIn.setBlockState(pos, iblockstate1, 3)) {
                        IBlockState iblockstate2 = iblockstate1.withProperty(BlockBed.PART, BlockBed.EnumPartType.HEAD);
                        worldIn.setBlockState(blockpos, iblockstate2, 3);
                    }

                    --stack.stackSize;
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }
}
