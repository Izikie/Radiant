package net.minecraft.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.stats.StatList;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.List;

public class ItemBoat extends Item {
    public ItemBoat() {
        this.maxStackSize = 1;
        this.setCreativeTab(CreativeTabs.TAB_TRANSPORT);
    }

    public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn) {
        float f = 1.0F;
        float f1 = playerIn.prevRotationPitch + (playerIn.rotationPitch - playerIn.prevRotationPitch) * f;
        float f2 = playerIn.prevRotationYaw + (playerIn.rotationYaw - playerIn.prevRotationYaw) * f;
        double d0 = playerIn.prevPosX + (playerIn.posX - playerIn.prevPosX) * f;
        double d1 = playerIn.prevPosY + (playerIn.posY - playerIn.prevPosY) * f + playerIn.getEyeHeight();
        double d2 = playerIn.prevPosZ + (playerIn.posZ - playerIn.prevPosZ) * f;
        Vec3 vec3 = new Vec3(d0, d1, d2);
        float f3 = MathHelper.cos(-f2 * 0.017453292F - (float) Math.PI);
        float f4 = MathHelper.sin(-f2 * 0.017453292F - (float) Math.PI);
        float f5 = -MathHelper.cos(-f1 * 0.017453292F);
        float f6 = MathHelper.sin(-f1 * 0.017453292F);
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d3 = 5.0D;
        Vec3 vec31 = vec3.addVector(f7 * d3, f6 * d3, f8 * d3);
        MovingObjectPosition movingobjectposition = worldIn.rayTraceBlocks(vec3, vec31, true);

        if (movingobjectposition != null) {
            Vec3 vec32 = playerIn.getLook(f);
            boolean flag = false;
            float f9 = 1.0F;
            List<Entity> list = worldIn.getEntitiesWithinAABBExcludingEntity(playerIn, playerIn.getEntityBoundingBox().addCoord(vec32.xCoord * d3, vec32.yCoord * d3, vec32.zCoord * d3).expand(f9, f9, f9));

            for (Entity value : list) {

                if (value.canBeCollidedWith()) {
                    float f10 = value.getCollisionBorderSize();
                    AxisAlignedBB axisalignedbb = value.getEntityBoundingBox().expand(f10, f10, f10);

                    if (axisalignedbb.isVecInside(vec3)) {
                        flag = true;
                    }
                }
            }

            if (!flag) {
                if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    BlockPos blockpos = movingobjectposition.getBlockPos();

                    if (worldIn.getBlockState(blockpos).getBlock() == Blocks.SNOW_LAYER) {
                        blockpos = blockpos.down();
                    }

                    EntityBoat entityboat = new EntityBoat(worldIn, (blockpos.getX() + 0.5F), (blockpos.getY() + 1.0F), (blockpos.getZ() + 0.5F));
                    entityboat.rotationYaw = (((MathHelper.floor((playerIn.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3) - 1) * 90);

                    if (!worldIn.getCollidingBoundingBoxes(entityboat, entityboat.getEntityBoundingBox().expand(-0.1D, -0.1D, -0.1D)).isEmpty()) {
                        return itemStackIn;
                    }

                    if (!worldIn.isRemote) {
                        worldIn.spawnEntityInWorld(entityboat);
                    }

                    if (!playerIn.capabilities.isCreativeMode) {
                        --itemStackIn.stackSize;
                    }

                    playerIn.triggerAchievement(StatList.OBJECT_USE_STATS[Item.getIdFromItem(this)]);
                }

            }
        }
        return itemStackIn;
    }
}
