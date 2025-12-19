package net.minecraft.potion;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.BaseAttributeMap;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

public class Potion {
    public static final Potion[] POTION_TYPES = new Potion[32];
    private static final Map<ResourceLocation, Potion> field_180150_I = new HashMap<>();
    public static final Potion MOVE_SPEED = (new Potion(1, new ResourceLocation("speed"), false, 8171462)).setPotionName("potion.moveSpeed").setIconIndex(0, 0).registerPotionAttributeModifier(SharedMonsterAttributes.MOVEMENT_SPEED, "91AEAA56-376B-4498-935B-2F7F68070635", 0.20000000298023224D, 2);
    public static final Potion MOVE_SLOWDOWN = (new Potion(2, new ResourceLocation("slowness"), true, 5926017)).setPotionName("potion.moveSlowdown").setIconIndex(1, 0).registerPotionAttributeModifier(SharedMonsterAttributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160890", -0.15000000596046448D, 2);
    public static final Potion DIG_SPEED = (new Potion(3, new ResourceLocation("haste"), false, 14270531)).setPotionName("potion.digSpeed").setIconIndex(2, 0).setEffectiveness(1.5D);
    public static final Potion DIG_SLOWDOWN = (new Potion(4, new ResourceLocation("mining_fatigue"), true, 4866583)).setPotionName("potion.digSlowDown").setIconIndex(3, 0);
    public static final Potion DAMAGE_BOOST = (new PotionAttackDamage(5, new ResourceLocation("strength"), false, 9643043)).setPotionName("potion.damageBoost").setIconIndex(4, 0).registerPotionAttributeModifier(SharedMonsterAttributes.ATTACK_DAMAGE, "648D7064-6A60-4F59-8ABE-C2C23A6DD7A9", 2.5D, 2);
    public static final Potion HEAL = (new PotionHealth(6, new ResourceLocation("instant_health"), false, 16262179)).setPotionName("potion.heal");
    public static final Potion HARM = (new PotionHealth(7, new ResourceLocation("instant_damage"), true, 4393481)).setPotionName("potion.harm");
    public static final Potion JUMP = (new Potion(8, new ResourceLocation("jump_boost"), false, 2293580)).setPotionName("potion.jump").setIconIndex(2, 1);
    public static final Potion CONFUSION = (new Potion(9, new ResourceLocation("nausea"), true, 5578058)).setPotionName("potion.confusion").setIconIndex(3, 1).setEffectiveness(0.25D);
    public static final Potion REGENERATION = (new Potion(10, new ResourceLocation("regeneration"), false, 13458603)).setPotionName("potion.regeneration").setIconIndex(7, 0).setEffectiveness(0.25D);
    public static final Potion RESISTANCE = (new Potion(11, new ResourceLocation("resistance"), false, 10044730)).setPotionName("potion.resistance").setIconIndex(6, 1);
    public static final Potion FIRE_RESISTANCE = (new Potion(12, new ResourceLocation("fire_resistance"), false, 14981690)).setPotionName("potion.fireResistance").setIconIndex(7, 1);
    public static final Potion WATER_BREATHING = (new Potion(13, new ResourceLocation("water_breathing"), false, 3035801)).setPotionName("potion.waterBreathing").setIconIndex(0, 2);
    public static final Potion INVISIBILITY = (new Potion(14, new ResourceLocation("invisibility"), false, 8356754)).setPotionName("potion.invisibility").setIconIndex(0, 1);
    public static final Potion BLINDNESS = (new Potion(15, new ResourceLocation("blindness"), true, 2039587)).setPotionName("potion.blindness").setIconIndex(5, 1).setEffectiveness(0.25D);
    public static final Potion NIGHT_VISION = (new Potion(16, new ResourceLocation("night_vision"), false, 2039713)).setPotionName("potion.nightVision").setIconIndex(4, 1);
    public static final Potion HUNGER = (new Potion(17, new ResourceLocation("hunger"), true, 5797459)).setPotionName("potion.hunger").setIconIndex(1, 1);
    public static final Potion WEAKNESS = (new PotionAttackDamage(18, new ResourceLocation("weakness"), true, 4738376)).setPotionName("potion.weakness").setIconIndex(5, 0).registerPotionAttributeModifier(SharedMonsterAttributes.ATTACK_DAMAGE, "22653B89-116E-49DC-9B6B-9971489B5BE5", 2.0D, 0);
    public static final Potion POISON = (new Potion(19, new ResourceLocation("poison"), true, 5149489)).setPotionName("potion.poison").setIconIndex(6, 0).setEffectiveness(0.25D);
    public static final Potion WITHER = (new Potion(20, new ResourceLocation("wither"), true, 3484199)).setPotionName("potion.wither").setIconIndex(1, 2).setEffectiveness(0.25D);
    public static final Potion HEALTH_BOOST = (new PotionHealthBoost(21, new ResourceLocation("health_boost"), false, 16284963)).setPotionName("potion.healthBoost").setIconIndex(2, 2).registerPotionAttributeModifier(SharedMonsterAttributes.MAX_HEALTH, "5D6F0BA2-1186-46AC-B896-C61C5CEE99CC", 4.0D, 0);
    public static final Potion ABSORPTION = (new PotionAbsorption(22, new ResourceLocation("absorption"), false, 2445989)).setPotionName("potion.absorption").setIconIndex(2, 2);
    public static final Potion SATURATION = (new PotionHealth(23, new ResourceLocation("saturation"), false, 16262179)).setPotionName("potion.saturation");
    public final int id;
    private final Map<IAttribute, AttributeModifier> attributeModifierMap = new HashMap<>();
    private final boolean isBadEffect;
    private final int liquidColor;
    private String name = "";
    private int statusIconIndex = -1;
    private double effectiveness;
    private boolean usable;

    protected Potion(int potionID, ResourceLocation location, boolean badEffect, int potionColor) {
        this.id = potionID;
        POTION_TYPES[potionID] = this;
        field_180150_I.put(location, this);
        this.isBadEffect = badEffect;

        if (badEffect) {
            this.effectiveness = 0.5D;
        } else {
            this.effectiveness = 1.0D;
        }

        this.liquidColor = potionColor;
    }

    public static Potion getPotionFromResourceLocation(String location) {
        return field_180150_I.get(new ResourceLocation(location));
    }

    public static Set<ResourceLocation> getPotionLocations() {
        return field_180150_I.keySet();
    }

    protected Potion setIconIndex(int p_76399_1_, int p_76399_2_) {
        this.statusIconIndex = p_76399_1_ + p_76399_2_ * 8;
        return this;
    }

    public int getId() {
        return this.id;
    }

    public void performEffect(EntityLivingBase entityLivingBaseIn, int p_76394_2_) {
        if (this.id == REGENERATION.id) {
            if (entityLivingBaseIn.getHealth() < entityLivingBaseIn.getMaxHealth()) {
                entityLivingBaseIn.heal(1.0F);
            }
        } else if (this.id == POISON.id) {
            if (entityLivingBaseIn.getHealth() > 1.0F) {
                entityLivingBaseIn.attackEntityFrom(DamageSource.MAGIC, 1.0F);
            }
        } else if (this.id == WITHER.id) {
            entityLivingBaseIn.attackEntityFrom(DamageSource.WITHER, 1.0F);
        } else if (this.id == HUNGER.id && entityLivingBaseIn instanceof EntityPlayer entityPlayer) {
            entityPlayer.addExhaustion(0.025F * (p_76394_2_ + 1));
        } else if (this.id == SATURATION.id && entityLivingBaseIn instanceof EntityPlayer) {
            if (!entityLivingBaseIn.worldObj.isRemote) {
                ((EntityPlayer) entityLivingBaseIn).getFoodStats().addStats(p_76394_2_ + 1, 1.0F);
            }
        } else if ((this.id != HEAL.id || entityLivingBaseIn.isEntityUndead()) && (this.id != HARM.id || !entityLivingBaseIn.isEntityUndead())) {
            if (this.id == HARM.id && !entityLivingBaseIn.isEntityUndead() || this.id == HEAL.id && entityLivingBaseIn.isEntityUndead()) {
                entityLivingBaseIn.attackEntityFrom(DamageSource.MAGIC, (6 << p_76394_2_));
            }
        } else {
            entityLivingBaseIn.heal(Math.max(4 << p_76394_2_, 0));
        }
    }

    public void affectEntity(Entity p_180793_1_, Entity p_180793_2_, EntityLivingBase entityLivingBaseIn, int p_180793_4_, double p_180793_5_) {
        if ((this.id != HEAL.id || entityLivingBaseIn.isEntityUndead()) && (this.id != HARM.id || !entityLivingBaseIn.isEntityUndead())) {
            if (this.id == HARM.id && !entityLivingBaseIn.isEntityUndead() || this.id == HEAL.id && entityLivingBaseIn.isEntityUndead()) {
                int j = (int) (p_180793_5_ * (6 << p_180793_4_) + 0.5D);

                if (p_180793_1_ == null) {
                    entityLivingBaseIn.attackEntityFrom(DamageSource.MAGIC, j);
                } else {
                    entityLivingBaseIn.attackEntityFrom(DamageSource.causeIndirectMagicDamage(p_180793_1_, p_180793_2_), j);
                }
            }
        } else {
            int i = (int) (p_180793_5_ * (4 << p_180793_4_) + 0.5D);
            entityLivingBaseIn.heal(i);
        }
    }

    public boolean isInstant() {
        return false;
    }

    public boolean isReady(int p_76397_1_, int p_76397_2_) {
        if (this.id == REGENERATION.id) {
            int k = 50 >> p_76397_2_;
            return k <= 0 || p_76397_1_ % k == 0;
        } else if (this.id == POISON.id) {
            int j = 25 >> p_76397_2_;
            return j <= 0 || p_76397_1_ % j == 0;
        } else if (this.id == WITHER.id) {
            int i = 40 >> p_76397_2_;
            return i <= 0 || p_76397_1_ % i == 0;
        } else {
            return this.id == HUNGER.id;
        }
    }

    public Potion setPotionName(String nameIn) {
        this.name = nameIn;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public boolean hasStatusIcon() {
        return this.statusIconIndex >= 0;
    }

    public int getStatusIconIndex() {
        return this.statusIconIndex;
    }

    public boolean isBadEffect() {
        return this.isBadEffect;
    }

    public static String getDurationString(PotionEffect effect) {
        if (effect.getIsPotionDurationMax()) {
            return "**:**";
        } else {
            int i = effect.getDuration();
            return StringUtils.ticksToElapsedTime(i);
        }
    }

    protected Potion setEffectiveness(double effectivenessIn) {
        this.effectiveness = effectivenessIn;
        return this;
    }

    public double getEffectiveness() {
        return this.effectiveness;
    }

    public boolean isUsable() {
        return this.usable;
    }

    public int getLiquidColor() {
        return this.liquidColor;
    }

    public Potion registerPotionAttributeModifier(IAttribute p_111184_1_, String p_111184_2_, double p_111184_3_, int p_111184_5_) {
        AttributeModifier attributemodifier = new AttributeModifier(UUID.fromString(p_111184_2_), this.getName(), p_111184_3_, p_111184_5_);
        this.attributeModifierMap.put(p_111184_1_, attributemodifier);
        return this;
    }

    public Map<IAttribute, AttributeModifier> getAttributeModifierMap() {
        return this.attributeModifierMap;
    }

    public void removeAttributesModifiersFromEntity(EntityLivingBase entityLivingBaseIn, BaseAttributeMap p_111187_2_, int amplifier) {
        for (Entry<IAttribute, AttributeModifier> entry : this.attributeModifierMap.entrySet()) {
            IAttributeInstance iattributeinstance = p_111187_2_.getAttributeInstance(entry.getKey());

            if (iattributeinstance != null) {
                iattributeinstance.removeModifier(entry.getValue());
            }
        }
    }

    public void applyAttributesModifiersToEntity(EntityLivingBase entityLivingBaseIn, BaseAttributeMap p_111185_2_, int amplifier) {
        for (Entry<IAttribute, AttributeModifier> entry : this.attributeModifierMap.entrySet()) {
            IAttributeInstance iattributeinstance = p_111185_2_.getAttributeInstance(entry.getKey());

            if (iattributeinstance != null) {
                AttributeModifier attributemodifier = entry.getValue();
                iattributeinstance.removeModifier(attributemodifier);
                iattributeinstance.applyModifier(new AttributeModifier(attributemodifier.getID(), this.getName() + " " + amplifier, this.getAttributeModifierAmount(amplifier, attributemodifier), attributemodifier.getOperation()));
            }
        }
    }

    public double getAttributeModifierAmount(int p_111183_1_, AttributeModifier modifier) {
        return modifier.getAmount() * (p_111183_1_ + 1);
    }
}
