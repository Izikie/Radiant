package net.minecraft.entity.ai;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.pathfinder.WalkNodeProcessor;

public class EntityAIControlledByPlayer extends EntityAIBase {
    private final EntityLiving thisEntity;
    private final float maxSpeed;
    private float currentSpeed;
    private boolean speedBoosted;
    private int speedBoostTime;
    private int maxSpeedBoostTime;

    public EntityAIControlledByPlayer(EntityLiving entitylivingIn, float maxspeed) {
        this.thisEntity = entitylivingIn;
        this.maxSpeed = maxspeed;
        this.setMutexBits(7);
    }

    public void startExecuting() {
        this.currentSpeed = 0.0F;
    }

    public void resetTask() {
        this.speedBoosted = false;
        this.currentSpeed = 0.0F;
    }

    public boolean shouldExecute() {
        return this.thisEntity.isEntityAlive() && this.thisEntity.riddenByEntity != null && this.thisEntity.riddenByEntity instanceof EntityPlayer && (this.speedBoosted || this.thisEntity.canBeSteered());
    }

    public void updateTask() {
        EntityPlayer entityplayer = (EntityPlayer) this.thisEntity.riddenByEntity;
        EntityCreature entitycreature = (EntityCreature) this.thisEntity;
        float f = MathHelper.wrapAngle(entityplayer.rotationYaw - this.thisEntity.rotationYaw) * 0.5F;

        if (f > 5.0F) {
            f = 5.0F;
        }

        if (f < -5.0F) {
            f = -5.0F;
        }

        this.thisEntity.rotationYaw = MathHelper.wrapAngle(this.thisEntity.rotationYaw + f);

        if (this.currentSpeed < this.maxSpeed) {
            this.currentSpeed += (this.maxSpeed - this.currentSpeed) * 0.01F;
        }

        if (this.currentSpeed > this.maxSpeed) {
            this.currentSpeed = this.maxSpeed;
        }

        int i = MathHelper.floor(this.thisEntity.posX);
        int j = MathHelper.floor(this.thisEntity.posY);
        int k = MathHelper.floor(this.thisEntity.posZ);
        float f1 = this.currentSpeed;

        if (this.speedBoosted) {
            if (this.speedBoostTime++ > this.maxSpeedBoostTime) {
                this.speedBoosted = false;
            }

            f1 += f1 * 1.15F * MathHelper.sin((float) this.speedBoostTime / this.maxSpeedBoostTime * (float) Math.PI);
        }

        float f2 = 0.91F;

        if (this.thisEntity.onGround) {
            f2 = this.thisEntity.worldObj.getBlockState(new BlockPos(MathHelper.floor(i), MathHelper.floor(j) - 1, MathHelper.floor(k))).getBlock().slipperiness * 0.91F;
        }

        float f3 = 0.16277136F / (f2 * f2 * f2);
        float f4 = MathHelper.sin(entitycreature.rotationYaw * (float) Math.PI / 180.0F);
        float f5 = MathHelper.cos(entitycreature.rotationYaw * (float) Math.PI / 180.0F);
        float f6 = entitycreature.getAIMoveSpeed() * f3;
        float f7 = Math.max(f1, 1.0F);
        f7 = f6 / f7;
        float f8 = f1 * f7;
        float f9 = -(f8 * f4);
        float f10 = f8 * f5;

        if (MathHelper.abs(f9) > MathHelper.abs(f10)) {
            if (f9 < 0.0F) {
                f9 -= this.thisEntity.width / 2.0F;
            }

            if (f9 > 0.0F) {
                f9 += this.thisEntity.width / 2.0F;
            }

            f10 = 0.0F;
        } else {
            f9 = 0.0F;

            if (f10 < 0.0F) {
                f10 -= this.thisEntity.width / 2.0F;
            }

            if (f10 > 0.0F) {
                f10 += this.thisEntity.width / 2.0F;
            }
        }

        int l = MathHelper.floor(this.thisEntity.posX + f9);
        int i1 = MathHelper.floor(this.thisEntity.posZ + f10);
        int j1 = MathHelper.floor(this.thisEntity.width + 1.0F);
        int k1 = MathHelper.floor(this.thisEntity.height + entityplayer.height + 1.0F);
        int l1 = MathHelper.floor(this.thisEntity.width + 1.0F);

        if (i != l || k != i1) {
            Block block = this.thisEntity.worldObj.getBlockState(new BlockPos(i, j, k)).getBlock();
            boolean flag = !this.isStairOrSlab(block) && (block.getMaterial() != Material.AIR || !this.isStairOrSlab(this.thisEntity.worldObj.getBlockState(new BlockPos(i, j - 1, k)).getBlock()));

            if (flag && 0 == WalkNodeProcessor.func_176170_a(this.thisEntity.worldObj, this.thisEntity, l, j, i1, j1, k1, l1, false, false, true) && 1 == WalkNodeProcessor.func_176170_a(this.thisEntity.worldObj, this.thisEntity, i, j + 1, k, j1, k1, l1, false, false, true) && 1 == WalkNodeProcessor.func_176170_a(this.thisEntity.worldObj, this.thisEntity, l, j + 1, i1, j1, k1, l1, false, false, true)) {
                entitycreature.getJumpHelper().setJumping();
            }
        }

        if (!entityplayer.capabilities.isCreativeMode && this.currentSpeed >= this.maxSpeed * 0.5F && this.thisEntity.getRNG().nextFloat() < 0.006F && !this.speedBoosted) {
            ItemStack itemstack = entityplayer.getHeldItem();

            if (itemstack != null && itemstack.getItem() == Items.CARROT_ON_A_STICK) {
                itemstack.damageItem(1, entityplayer);

                if (itemstack.stackSize == 0) {
                    ItemStack itemstack1 = new ItemStack(Items.FISHING_ROD);
                    itemstack1.setTagCompound(itemstack.getTagCompound());
                    entityplayer.inventory.mainInventory[entityplayer.inventory.currentItem] = itemstack1;
                }
            }
        }

        this.thisEntity.moveEntityWithHeading(0.0F, f1);
    }

    private boolean isStairOrSlab(Block blockIn) {
        return blockIn instanceof BlockStairs || blockIn instanceof BlockSlab;
    }

    public boolean isSpeedBoosted() {
        return this.speedBoosted;
    }

    public void boostSpeed() {
        this.speedBoosted = true;
        this.speedBoostTime = 0;
        this.maxSpeedBoostTime = this.thisEntity.getRNG().nextInt(841) + 140;
    }

    public boolean isControlledByPlayer() {
        return !this.isSpeedBoosted() && this.currentSpeed > this.maxSpeed * 0.3F;
    }
}
