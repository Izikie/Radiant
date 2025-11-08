package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.stats.StatList;
import net.minecraft.util.BlockPos;
import net.minecraft.util.input.MovingObjectPosition;
import net.minecraft.world.World;

public class ItemLilyPad extends ItemColored {
    public ItemLilyPad(Block block) {
        super(block, false);
    }

    @Override
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

                BlockPos blockpos1 = blockpos.up();
                IBlockState iblockstate = worldIn.getBlockState(blockpos);

                if (iblockstate.getBlock().getMaterial() == Material.WATER && iblockstate.getValue(BlockLiquid.LEVEL) == 0 && worldIn.isAirBlock(blockpos1)) {
                    worldIn.setBlockState(blockpos1, Blocks.WATERLILY.getDefaultState());

                    if (!playerIn.capabilities.isCreativeMode) {
                        --itemStackIn.stackSize;
                    }

                    playerIn.triggerAchievement(StatList.OBJECT_USE_STATS[Item.getIdFromItem(this)]);
                }
            }

        }
        return itemStackIn;
    }

    @Override
    public int getColorFromItemStack(ItemStack stack, int renderPass) {
        return Blocks.WATERLILY.getRenderColor(Blocks.WATERLILY.getStateFromMeta(stack.getMetadata()));
    }
}
