package net.minecraft.enchantment;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityGroup;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

public class EnchantmentDamage extends Enchantment {
    private static final String[] PROTECTION_NAME = new String[]{"all", "undead", "arthropods"};
    private static final int[] BASE_ENCHANTABILITY = new int[]{1, 5, 5};
    private static final int[] LEVEL_ENCHANTABILITY = new int[]{11, 8, 8};
    private static final int[] THRESHOLD_ENCHANTABILITY = new int[]{20, 20, 20};
    public final int damageType;

    public EnchantmentDamage(int enchID, ResourceLocation enchName, int enchWeight, int classification) {
        super(enchID, enchName, enchWeight, EnchantmentTarget.WEAPON);
        this.damageType = classification;
    }

    public int getMinEnchantability(int enchantmentLevel) {
        return BASE_ENCHANTABILITY[this.damageType] + (enchantmentLevel - 1) * LEVEL_ENCHANTABILITY[this.damageType];
    }

    public int getMaxEnchantability(int enchantmentLevel) {
        return this.getMinEnchantability(enchantmentLevel) + THRESHOLD_ENCHANTABILITY[this.damageType];
    }

    public int getMaxLevel() {
        return 5;
    }

    public float calcDamageByCreature(int level, EntityGroup creatureType) {
        return this.damageType == 0 ? level * 1.25F : (this.damageType == 1 && creatureType == EntityGroup.UNDEAD ? level * 2.5F : (this.damageType == 2 && creatureType == EntityGroup.ARTHROPOD ? level * 2.5F : 0.0F));
    }

    public String getName() {
        return "enchantment.damage." + PROTECTION_NAME[this.damageType];
    }

    public boolean canApplyTogether(Enchantment ench) {
        return !(ench instanceof EnchantmentDamage);
    }

    public boolean canApply(ItemStack stack) {
        return stack.getItem() instanceof ItemAxe ? true : super.canApply(stack);
    }

    public void onEntityDamaged(EntityLivingBase user, Entity target, int level) {
        if (target instanceof EntityLivingBase entitylivingbase) {

            if (this.damageType == 2 && entitylivingbase.getCreatureAttribute() == EntityGroup.ARTHROPOD) {
                int i = 20 + user.getRNG().nextInt(10 * level);
                entitylivingbase.addPotionEffect(new PotionEffect(Potion.MOVE_SLOWDOWN.id, i, 3));
            }
        }
    }
}
