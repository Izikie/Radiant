package net.minecraft.enchantment;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.ResourceLocation;

public class EnchantmentProtection extends Enchantment {
    private static final String[] PROTECTION_NAME = new String[]{"all", "fire", "fall", "explosion", "projectile"};
    private static final int[] BASE_ENCHANTABILITY = new int[]{1, 10, 5, 5, 3};
    private static final int[] LEVEL_ENCHANTABILITY = new int[]{11, 8, 6, 8, 6};
    private static final int[] THRESHOLD_ENCHANTABILITY = new int[]{20, 12, 10, 12, 15};
    public final int protectionType;

    public EnchantmentProtection(int p_i45765_1_, ResourceLocation p_i45765_2_, int p_i45765_3_, int p_i45765_4_) {
        super(p_i45765_1_, p_i45765_2_, p_i45765_3_, EnchantmentTarget.ARMOR);
        this.protectionType = p_i45765_4_;

        if (p_i45765_4_ == 2) {
            this.type = EnchantmentTarget.ARMOR_FEET;
        }
    }

    @Override
    public int getMinEnchantability(int enchantmentLevel) {
        return BASE_ENCHANTABILITY[this.protectionType] + (enchantmentLevel - 1) * LEVEL_ENCHANTABILITY[this.protectionType];
    }

    @Override
    public int getMaxEnchantability(int enchantmentLevel) {
        return this.getMinEnchantability(enchantmentLevel) + THRESHOLD_ENCHANTABILITY[this.protectionType];
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public int calcModifierDamage(int level, DamageSource source) {
        if (source.canHarmInCreative()) {
            return 0;
        } else {
            float f = (6 + level * level) / 3.0F;
            return this.protectionType == 0 ? MathHelper.floor(f * 0.75F) : (this.protectionType == 1 && source.isFireDamage() ? MathHelper.floor(f * 1.25F) : (this.protectionType == 2 && source == DamageSource.FALL ? MathHelper.floor(f * 2.5F) : (this.protectionType == 3 && source.isExplosion() ? MathHelper.floor(f * 1.5F) : (this.protectionType == 4 && source.isProjectile() ? MathHelper.floor(f * 1.5F) : 0))));
        }
    }

    @Override
    public String getName() {
        return "enchantment.protect." + PROTECTION_NAME[this.protectionType];
    }

    @Override
    public boolean canApplyTogether(Enchantment ench) {
        if (ench instanceof EnchantmentProtection enchantmentprotection) {
            return enchantmentprotection.protectionType != this.protectionType && (this.protectionType == 2 || enchantmentprotection.protectionType == 2);
        } else {
            return super.canApplyTogether(ench);
        }
    }

    public static int getFireTimeForEntity(Entity p_92093_0_, int p_92093_1_) {
        int i = EnchantmentHelper.getMaxEnchantmentLevel(Enchantment.FIRE_PROTECTION.effectId, p_92093_0_.getInventory());

        if (i > 0) {
            p_92093_1_ -= MathHelper.floor((float) p_92093_1_ * i * 0.15F);
        }

        return p_92093_1_;
    }

    public static double func_92092_a(Entity p_92092_0_, double p_92092_1_) {
        int i = EnchantmentHelper.getMaxEnchantmentLevel(Enchantment.BLAST_PROTECTION.effectId, p_92092_0_.getInventory());

        if (i > 0) {
            p_92092_1_ -= MathHelper.floor(p_92092_1_ * (i * 0.15F));
        }

        return p_92092_1_;
    }
}
