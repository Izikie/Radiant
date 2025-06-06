package net.minecraft.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public abstract class EntityHanging extends Entity {
    private int tickCounter1;
    protected BlockPos hangingPosition;
    public Direction facingDirection;

    public EntityHanging(World worldIn) {
        super(worldIn);
        this.setSize(0.5F, 0.5F);
    }

    public EntityHanging(World worldIn, BlockPos hangingPositionIn) {
        this(worldIn);
        this.hangingPosition = hangingPositionIn;
    }

    protected void entityInit() {
    }

    protected void updateFacingWithBoundingBox(Direction facingDirectionIn) {
        Objects.requireNonNull(facingDirectionIn);
        Validate.isTrue(facingDirectionIn.getAxis().isHorizontal());
        this.facingDirection = facingDirectionIn;
        this.prevRotationYaw = this.rotationYaw = (this.facingDirection.getHorizontalIndex() * 90);
        this.updateBoundingBox();
    }

    private void updateBoundingBox() {
        if (this.facingDirection != null) {
            double d0 = this.hangingPosition.getX() + 0.5D;
            double d1 = this.hangingPosition.getY() + 0.5D;
            double d2 = this.hangingPosition.getZ() + 0.5D;
            double d3 = 0.46875D;
            double d4 = this.func_174858_a(this.getWidthPixels());
            double d5 = this.func_174858_a(this.getHeightPixels());
            d0 = d0 - this.facingDirection.getFrontOffsetX() * 0.46875D;
            d2 = d2 - this.facingDirection.getFrontOffsetZ() * 0.46875D;
            d1 = d1 + d5;
            Direction enumfacing = this.facingDirection.rotateYCCW();
            d0 = d0 + d4 * enumfacing.getFrontOffsetX();
            d2 = d2 + d4 * enumfacing.getFrontOffsetZ();
            this.posX = d0;
            this.posY = d1;
            this.posZ = d2;
            double d6 = this.getWidthPixels();
            double d7 = this.getHeightPixels();
            double d8 = this.getWidthPixels();

            if (this.facingDirection.getAxis() == Direction.Axis.Z) {
                d8 = 1.0D;
            } else {
                d6 = 1.0D;
            }

            d6 = d6 / 32.0D;
            d7 = d7 / 32.0D;
            d8 = d8 / 32.0D;
            this.setEntityBoundingBox(new AxisAlignedBB(d0 - d6, d1 - d7, d2 - d8, d0 + d6, d1 + d7, d2 + d8));
        }
    }

    private double func_174858_a(int p_174858_1_) {
        return p_174858_1_ % 32 == 0 ? 0.5D : 0.0D;
    }

    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.tickCounter1++ == 100 && !this.worldObj.isRemote) {
            this.tickCounter1 = 0;

            if (!this.isDead && !this.onValidSurface()) {
                this.setDead();
                this.onBroken(null);
            }
        }
    }

    public boolean onValidSurface() {
        if (!this.worldObj.getCollidingBoundingBoxes(this, this.getEntityBoundingBox()).isEmpty()) {
            return false;
        } else {
            int i = Math.max(1, this.getWidthPixels() / 16);
            int j = Math.max(1, this.getHeightPixels() / 16);
            BlockPos blockpos = this.hangingPosition.offset(this.facingDirection.getOpposite());
            Direction enumfacing = this.facingDirection.rotateYCCW();

            for (int k = 0; k < i; ++k) {
                for (int l = 0; l < j; ++l) {
                    BlockPos blockpos1 = blockpos.offset(enumfacing, k).up(l);
                    Block block = this.worldObj.getBlockState(blockpos1).getBlock();

                    if (!block.getMaterial().isSolid() && !BlockRedstoneDiode.isRedstoneRepeaterBlockID(block)) {
                        return false;
                    }
                }
            }

            for (Entity entity : this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox())) {
                if (entity instanceof EntityHanging) {
                    return false;
                }
            }

            return true;
        }
    }

    public boolean canBeCollidedWith() {
        return true;
    }

    public boolean hitByEntity(Entity entityIn) {
        return entityIn instanceof EntityPlayer entityPlayer && this.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer), 0.0F);
    }

    public Direction getHorizontalFacing() {
        return this.facingDirection;
    }

    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.isEntityInvulnerable(source)) {
            return false;
        } else {
            if (!this.isDead && !this.worldObj.isRemote) {
                this.setDead();
                this.setBeenAttacked();
                this.onBroken(source.getEntity());
            }

            return true;
        }
    }

    public void moveEntity(double x, double y, double z) {
        if (!this.worldObj.isRemote && !this.isDead && x * x + y * y + z * z > 0.0D) {
            this.setDead();
            this.onBroken(null);
        }
    }

    public void addVelocity(double x, double y, double z) {
        if (!this.worldObj.isRemote && !this.isDead && x * x + y * y + z * z > 0.0D) {
            this.setDead();
            this.onBroken(null);
        }
    }

    public void writeEntityToNBT(NBTTagCompound tagCompound) {
        tagCompound.setByte("Facing", (byte) this.facingDirection.getHorizontalIndex());
        tagCompound.setInteger("TileX", this.getHangingPosition().getX());
        tagCompound.setInteger("TileY", this.getHangingPosition().getY());
        tagCompound.setInteger("TileZ", this.getHangingPosition().getZ());
    }

    public void readEntityFromNBT(NBTTagCompound tagCompund) {
        this.hangingPosition = new BlockPos(tagCompund.getInteger("TileX"), tagCompund.getInteger("TileY"), tagCompund.getInteger("TileZ"));
        Direction enumfacing;

        if (tagCompund.hasKey("Direction", 99)) {
            enumfacing = Direction.getHorizontal(tagCompund.getByte("Direction"));
            this.hangingPosition = this.hangingPosition.offset(enumfacing);
        } else if (tagCompund.hasKey("Facing", 99)) {
            enumfacing = Direction.getHorizontal(tagCompund.getByte("Facing"));
        } else {
            enumfacing = Direction.getHorizontal(tagCompund.getByte("Dir"));
        }

        this.updateFacingWithBoundingBox(enumfacing);
    }

    public abstract int getWidthPixels();

    public abstract int getHeightPixels();

    public abstract void onBroken(Entity brokenEntity);

    protected boolean shouldSetPosAfterLoading() {
        return false;
    }

    public void setPosition(double x, double y, double z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        BlockPos blockpos = this.hangingPosition;
        this.hangingPosition = new BlockPos(x, y, z);

        if (!this.hangingPosition.equals(blockpos)) {
            this.updateBoundingBox();
            this.isAirBorne = true;
        }
    }

    public BlockPos getHangingPosition() {
        return this.hangingPosition;
    }
}
