package net.minecraft.entity.ai;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

public class EntityAILeapAtTarget extends EntityAIBase {
    final EntityLiving leaper;
    EntityLivingBase leapTarget;
    final float leapMotionY;

    public EntityAILeapAtTarget(EntityLiving leapingEntity, float leapMotionYIn) {
        this.leaper = leapingEntity;
        this.leapMotionY = leapMotionYIn;
        this.setMutexBits(5);
    }

    @Override
    public boolean shouldExecute() {
        this.leapTarget = this.leaper.getAttackTarget();

        if (this.leapTarget == null) {
            return false;
        } else {
            double d0 = this.leaper.getDistanceSqToEntity(this.leapTarget);
            return d0 >= 4.0D && d0 <= 16.0D && (this.leaper.onGround && this.leaper.getRNG().nextInt(5) == 0);
        }
    }

    @Override
    public boolean continueExecuting() {
        return !this.leaper.onGround;
    }

    @Override
    public void startExecuting() {
        double d0 = this.leapTarget.posX - this.leaper.posX;
        double d1 = this.leapTarget.posZ - this.leaper.posZ;
        float f = MathHelper.sqrt(d0 * d0 + d1 * d1);
        this.leaper.motionX += d0 / f * 0.5D * 0.800000011920929D + this.leaper.motionX * 0.20000000298023224D;
        this.leaper.motionZ += d1 / f * 0.5D * 0.800000011920929D + this.leaper.motionZ * 0.20000000298023224D;
        this.leaper.motionY = this.leapMotionY;
    }
}
