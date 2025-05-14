package net.minecraft.entity.monster;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

public class EntityCaveSpider extends EntitySpider {
    public EntityCaveSpider(World worldIn) {
        super(worldIn);
        this.setSize(0.7F, 0.5F);
    }

    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(12.0D);
    }

    public boolean attackEntityAsMob(Entity entityIn) {
        if (super.attackEntityAsMob(entityIn)) {
            if (entityIn instanceof EntityLivingBase) {
                int i = 0;

                if (this.worldObj.getDifficulty() == Difficulty.NORMAL) {
                    i = 7;
                } else if (this.worldObj.getDifficulty() == Difficulty.HARD) {
                    i = 15;
                }

                if (i > 0) {
                    ((EntityLivingBase) entityIn).addPotionEffect(new PotionEffect(Potion.POISON.id, i * 20, 0));
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData livingdata) {
        return livingdata;
    }

    public float getEyeHeight() {
        return 0.45F;
    }
}
