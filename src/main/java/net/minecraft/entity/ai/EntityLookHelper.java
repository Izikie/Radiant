package net.minecraft.entity.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

public class EntityLookHelper {
    private final EntityLiving entity;
    private float deltaLookYaw;
    private float deltaLookPitch;
    private boolean isLooking;
    private double posX;
    private double posY;
    private double posZ;

    public EntityLookHelper(EntityLiving entitylivingIn) {
        this.entity = entitylivingIn;
    }

    public void setLookPositionWithEntity(Entity entityIn, float deltaYaw, float deltaPitch) {
        this.posX = entityIn.posX;

        if (entityIn instanceof EntityLivingBase) {
            this.posY = entityIn.posY + entityIn.getEyeHeight();
        } else {
            this.posY = (entityIn.getEntityBoundingBox().minY + entityIn.getEntityBoundingBox().maxY) / 2.0D;
        }

        this.posZ = entityIn.posZ;
        this.deltaLookYaw = deltaYaw;
        this.deltaLookPitch = deltaPitch;
        this.isLooking = true;
    }

    public void setLookPosition(double x, double y, double z, float deltaYaw, float deltaPitch) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.deltaLookYaw = deltaYaw;
        this.deltaLookPitch = deltaPitch;
        this.isLooking = true;
    }

    public void onUpdateLook() {
        this.entity.rotationPitch = 0.0F;

        if (this.isLooking) {
            this.isLooking = false;
            double d0 = this.posX - this.entity.posX;
            double d1 = this.posY - (this.entity.posY + this.entity.getEyeHeight());
            double d2 = this.posZ - this.entity.posZ;
            double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
            float f = (float) (MathHelper.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F;
            float f1 = (float) (-(MathHelper.atan2(d1, d3) * 180.0D / Math.PI));
            this.entity.rotationPitch = this.updateRotation(this.entity.rotationPitch, f1, this.deltaLookPitch);
            this.entity.rotationYawHead = this.updateRotation(this.entity.rotationYawHead, f, this.deltaLookYaw);
        } else {
            this.entity.rotationYawHead = this.updateRotation(this.entity.rotationYawHead, this.entity.renderYawOffset, 10.0F);
        }

        float f2 = MathHelper.wrapAngle(this.entity.rotationYawHead - this.entity.renderYawOffset);

        if (!this.entity.getNavigator().noPath()) {
            if (f2 < -75.0F) {
                this.entity.rotationYawHead = this.entity.renderYawOffset - 75.0F;
            }

            if (f2 > 75.0F) {
                this.entity.rotationYawHead = this.entity.renderYawOffset + 75.0F;
            }
        }
    }

    private float updateRotation(float p_75652_1_, float p_75652_2_, float p_75652_3_) {
        float f = MathHelper.wrapAngle(p_75652_2_ - p_75652_1_);

        if (f > p_75652_3_) {
            f = p_75652_3_;
        }

        if (f < -p_75652_3_) {
            f = -p_75652_3_;
        }

        return p_75652_1_ + f;
    }

    public boolean getIsLooking() {
        return this.isLooking;
    }

    public double getLookPosX() {
        return this.posX;
    }

    public double getLookPosY() {
        return this.posY;
    }

    public double getLookPosZ() {
        return this.posZ;
    }
}
