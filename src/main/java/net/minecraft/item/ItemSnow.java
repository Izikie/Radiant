package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

public class ItemSnow extends ItemBlock {
    public ItemSnow(Block block) {
        super(block);
        this.setMaxDamage(0);
        this.setHasSubtypes(true);
    }

    public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, Direction side, float hitX, float hitY, float hitZ) {
        if (stack.stackSize == 0) {
            return false;
        } else if (!playerIn.canPlayerEdit(pos, side, stack)) {
            return false;
        } else {
            IBlockState iblockstate = worldIn.getBlockState(pos);
            Block block = iblockstate.getBlock();
            BlockPos blockpos = pos;

            if ((side != Direction.UP || block != this.block) && !block.isReplaceable(worldIn, pos)) {
                blockpos = pos.offset(side);
                iblockstate = worldIn.getBlockState(blockpos);
                block = iblockstate.getBlock();
            }

            if (block == this.block) {
                int i = iblockstate.getValue(BlockSnow.LAYERS);

                if (i <= 7) {
                    IBlockState iblockstate1 = iblockstate.withProperty(BlockSnow.LAYERS, i + 1);
                    AxisAlignedBB axisalignedbb = this.block.getCollisionBoundingBox(worldIn, blockpos, iblockstate1);

                    if (axisalignedbb != null && worldIn.checkNoEntityCollision(axisalignedbb) && worldIn.setBlockState(blockpos, iblockstate1, 2)) {
                        worldIn.playSoundEffect((blockpos.getX() + 0.5F), (blockpos.getY() + 0.5F), (blockpos.getZ() + 0.5F), this.block.stepSound.getPlaceSound(), (this.block.stepSound.getVolume() + 1.0F) / 2.0F, this.block.stepSound.getFrequency() * 0.8F);
                        --stack.stackSize;
                        return true;
                    }
                }
            }

            return super.onItemUse(stack, playerIn, worldIn, blockpos, side, hitX, hitY, hitZ);
        }
    }

    public int getMetadata(int damage) {
        return damage;
    }
}
