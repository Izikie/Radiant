package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.stats.StatList;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ParticleTypes;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class ItemBucket extends Item {
    private final Block isFull;

    public ItemBucket(Block containedBlock) {
        this.maxStackSize = 1;
        this.isFull = containedBlock;
        this.setCreativeTab(CreativeTabs.tabMisc);
    }

    public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn) {
        boolean flag = this.isFull == Blocks.AIR;
        MovingObjectPosition movingobjectposition = this.getMovingObjectPositionFromPlayer(worldIn, playerIn, flag);

        if (movingobjectposition != null) {
            if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                BlockPos blockpos = movingobjectposition.getBlockPos();

                if (!worldIn.isBlockModifiable(playerIn, blockpos)) {
                    return itemStackIn;
                }

                if (flag) {
                    if (!playerIn.canPlayerEdit(blockpos.offset(movingobjectposition.sideHit), movingobjectposition.sideHit, itemStackIn)) {
                        return itemStackIn;
                    }

                    IBlockState iblockstate = worldIn.getBlockState(blockpos);
                    Material material = iblockstate.getBlock().getMaterial();

                    if (material == Material.WATER && iblockstate.getValue(BlockLiquid.LEVEL) == 0) {
                        worldIn.setBlockToAir(blockpos);
                        playerIn.triggerAchievement(StatList.OBJECT_USE_STATS[Item.getIdFromItem(this)]);
                        return this.fillBucket(itemStackIn, playerIn, Items.WATER_BUCKET);
                    }

                    if (material == Material.LAVA && iblockstate.getValue(BlockLiquid.LEVEL) == 0) {
                        worldIn.setBlockToAir(blockpos);
                        playerIn.triggerAchievement(StatList.OBJECT_USE_STATS[Item.getIdFromItem(this)]);
                        return this.fillBucket(itemStackIn, playerIn, Items.LAVA_BUCKET);
                    }
                } else {
                    if (this.isFull == Blocks.AIR) {
                        return new ItemStack(Items.BUCKET);
                    }

                    BlockPos blockpos1 = blockpos.offset(movingobjectposition.sideHit);

                    if (!playerIn.canPlayerEdit(blockpos1, movingobjectposition.sideHit, itemStackIn)) {
                        return itemStackIn;
                    }

                    if (this.tryPlaceContainedLiquid(worldIn, blockpos1) && !playerIn.capabilities.isCreativeMode) {
                        playerIn.triggerAchievement(StatList.OBJECT_USE_STATS[Item.getIdFromItem(this)]);
                        return new ItemStack(Items.BUCKET);
                    }
                }
            }

        }
        return itemStackIn;
    }

    private ItemStack fillBucket(ItemStack emptyBuckets, EntityPlayer player, Item fullBucket) {
        if (player.capabilities.isCreativeMode) {
            return emptyBuckets;
        } else if (--emptyBuckets.stackSize <= 0) {
            return new ItemStack(fullBucket);
        } else {
            if (!player.inventory.addItemStackToInventory(new ItemStack(fullBucket))) {
                player.dropPlayerItemWithRandomChoice(new ItemStack(fullBucket, 1, 0), false);
            }

            return emptyBuckets;
        }
    }

    public boolean tryPlaceContainedLiquid(World worldIn, BlockPos pos) {
        if (this.isFull == Blocks.AIR) {
            return false;
        } else {
            Material material = worldIn.getBlockState(pos).getBlock().getMaterial();
            boolean flag = !material.isSolid();

            if (!worldIn.isAirBlock(pos) && !flag) {
                return false;
            } else {
                if (worldIn.provider.doesWaterVaporize() && this.isFull == Blocks.FLOWING_WATER) {
                    int i = pos.getX();
                    int j = pos.getY();
                    int k = pos.getZ();
                    worldIn.playSoundEffect((i + 0.5F), (j + 0.5F), (k + 0.5F), "random.fizz", 0.5F, 2.6F + (worldIn.rand.nextFloat() - worldIn.rand.nextFloat()) * 0.8F);

                    for (int l = 0; l < 8; ++l) {
                        worldIn.spawnParticle(ParticleTypes.SMOKE_LARGE, i + Math.random(), j + Math.random(), k + Math.random(), 0.0D, 0.0D, 0.0D);
                    }
                } else {
                    if (!worldIn.isRemote && flag && !material.isLiquid()) {
                        worldIn.destroyBlock(pos, true);
                    }

                    worldIn.setBlockState(pos, this.isFull.getDefaultState(), 3);
                }

                return true;
            }
        }
    }
}
