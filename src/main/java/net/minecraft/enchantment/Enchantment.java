package net.minecraft.enchantment;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

public abstract class Enchantment {
    private static final Enchantment[] ENCHANTMENTS_LIST = new Enchantment[256];
    public static final Enchantment[] ENCHANTMENTS_BOOK_LIST;
    private static final Map<ResourceLocation, Enchantment> LOCATION_ENCHANTMENTS = new HashMap<>();
    public static final Enchantment PROTECTION = new EnchantmentProtection(0, new ResourceLocation("protection"), 10, 0);
    public static final Enchantment FIRE_PROTECTION = new EnchantmentProtection(1, new ResourceLocation("fire_protection"), 5, 1);
    public static final Enchantment FEATHER_FALLING = new EnchantmentProtection(2, new ResourceLocation("feather_falling"), 5, 2);
    public static final Enchantment BLAST_PROTECTION = new EnchantmentProtection(3, new ResourceLocation("blast_protection"), 2, 3);
    public static final Enchantment PROJECTILE_PROTECTION = new EnchantmentProtection(4, new ResourceLocation("projectile_protection"), 5, 4);
    public static final Enchantment RESPIRATION = new EnchantmentOxygen(5, new ResourceLocation("respiration"), 2);
    public static final Enchantment AQUA_AFFINITY = new EnchantmentWaterWorker(6, new ResourceLocation("aqua_affinity"), 2);
    public static final Enchantment THORNS = new EnchantmentThorns(7, new ResourceLocation("thorns"), 1);
    public static final Enchantment DEPTH_STRIDER = new EnchantmentWaterWalker(8, new ResourceLocation("depth_strider"), 2);
    public static final Enchantment SHARPNESS = new EnchantmentDamage(16, new ResourceLocation("sharpness"), 10, 0);
    public static final Enchantment SMITE = new EnchantmentDamage(17, new ResourceLocation("smite"), 5, 1);
    public static final Enchantment BANE_OF_ARTHROPODS = new EnchantmentDamage(18, new ResourceLocation("bane_of_arthropods"), 5, 2);
    public static final Enchantment KNOCKBACK = new EnchantmentKnockback(19, new ResourceLocation("knockback"), 5);
    public static final Enchantment FIRE_ASPECT = new EnchantmentFireAspect(20, new ResourceLocation("fire_aspect"), 2);
    public static final Enchantment LOOTING = new EnchantmentLootBonus(21, new ResourceLocation("looting"), 2, EnchantmentTarget.WEAPON);
    public static final Enchantment EFFICIENCY = new EnchantmentDigging(32, new ResourceLocation("efficiency"), 10);
    public static final Enchantment SILK_TOUCH = new EnchantmentUntouching(33, new ResourceLocation("silk_touch"), 1);
    public static final Enchantment UNBREAKING = new EnchantmentDurability(34, new ResourceLocation("unbreaking"), 5);
    public static final Enchantment FORTUNE = new EnchantmentLootBonus(35, new ResourceLocation("fortune"), 2, EnchantmentTarget.DIGGER);
    public static final Enchantment POWER = new EnchantmentArrowDamage(48, new ResourceLocation("power"), 10);
    public static final Enchantment PUNCH = new EnchantmentArrowKnockback(49, new ResourceLocation("punch"), 2);
    public static final Enchantment FLAME = new EnchantmentArrowFire(50, new ResourceLocation("flame"), 2);
    public static final Enchantment INFINITY = new EnchantmentArrowInfinite(51, new ResourceLocation("infinity"), 1);
    public static final Enchantment LUCK_OF_THE_SEA = new EnchantmentLootBonus(61, new ResourceLocation("luck_of_the_sea"), 2, EnchantmentTarget.FISHING_ROD);
    public static final Enchantment LURE = new EnchantmentFishingSpeed(62, new ResourceLocation("lure"), 2, EnchantmentTarget.FISHING_ROD);
    public final int effectId;
    private final int weight;
    public EnchantmentTarget type;
    protected String name;

    public static Enchantment getEnchantmentById(int enchID) {
        return enchID >= 0 && enchID < ENCHANTMENTS_LIST.length ? ENCHANTMENTS_LIST[enchID] : null;
    }

    protected Enchantment(int enchID, ResourceLocation enchName, int enchWeight, EnchantmentTarget enchType) {
        this.effectId = enchID;
        this.weight = enchWeight;
        this.type = enchType;

        if (ENCHANTMENTS_LIST[enchID] != null) {
            throw new IllegalArgumentException("Duplicate enchantment id!");
        } else {
            ENCHANTMENTS_LIST[enchID] = this;
            LOCATION_ENCHANTMENTS.put(enchName, this);
        }
    }

    public static Enchantment getEnchantmentByLocation(String location) {
        return LOCATION_ENCHANTMENTS.get(new ResourceLocation(location));
    }

    public static Set<ResourceLocation> func_181077_c() {
        return LOCATION_ENCHANTMENTS.keySet();
    }

    public int getWeight() {
        return this.weight;
    }

    public int getMinLevel() {
        return 1;
    }

    public int getMaxLevel() {
        return 1;
    }

    public int getMinEnchantability(int enchantmentLevel) {
        return 1 + enchantmentLevel * 10;
    }

    public int getMaxEnchantability(int enchantmentLevel) {
        return this.getMinEnchantability(enchantmentLevel) + 5;
    }

    public int calcModifierDamage(int level, DamageSource source) {
        return 0;
    }

    public float calcDamageByCreature(int level, EntityGroup creatureType) {
        return 0.0F;
    }

    public boolean canApplyTogether(Enchantment ench) {
        return this != ench;
    }

    public Enchantment setName(String enchName) {
        this.name = enchName;
        return this;
    }

    public String getName() {
        return "enchantment." + this.name;
    }

    public String getTranslatedName(int level) {
        String s = StatCollector.translateToLocal(this.getName());
        return s + " " + StatCollector.translateToLocal("enchantment.level." + level);
    }

    public boolean canApply(ItemStack stack) {
        return this.type.canEnchantItem(stack.getItem());
    }

    public void onEntityDamaged(EntityLivingBase user, Entity target, int level) {
    }

    public void onUserHurt(EntityLivingBase user, Entity attacker, int level) {
    }

    static {
        List<Enchantment> list = Lists.newArrayList();

        for (Enchantment enchantment : ENCHANTMENTS_LIST) {
            if (enchantment != null) {
                list.add(enchantment);
            }
        }

        ENCHANTMENTS_BOOK_LIST = list.toArray(new Enchantment[0]);
    }
}
