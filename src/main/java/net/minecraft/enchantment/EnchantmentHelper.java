package net.minecraft.enchantment;

import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.WeightedRandom;

import java.util.*;

public class EnchantmentHelper {
    private static final Random ENCHANTMENT_RAND = new Random();
    private static final ModifierDamage enchantmentModifierDamage = new ModifierDamage();
    private static final ModifierLiving enchantmentModifierLiving = new ModifierLiving();
    private static final HurtIterator ENCHANTMENT_ITERATOR_HURT = new HurtIterator();
    private static final DamageIterator ENCHANTMENT_ITERATOR_DAMAGE = new DamageIterator();

    public static int getEnchantmentLevel(int enchID, ItemStack stack) {
        if (stack != null) {
            NBTTagList nbttaglist = stack.getEnchantmentTagList();

            if (nbttaglist != null) {
                for (int i = 0; i < nbttaglist.tagCount(); ++i) {
                    int j = nbttaglist.getCompoundTagAt(i).getShort("id");
                    int k = nbttaglist.getCompoundTagAt(i).getShort("lvl");

                    if (j == enchID) {
                        return k;
                    }
                }

            }
        }
        return 0;
    }

    public static Int2IntLinkedOpenHashMap getEnchantments(ItemStack stack) {
        Int2IntLinkedOpenHashMap map = new Int2IntLinkedOpenHashMap();
        NBTTagList nbttaglist = stack.getItem() == Items.ENCHANTED_BOOK ? Items.ENCHANTED_BOOK.getEnchantments(stack) : stack.getEnchantmentTagList();

        if (nbttaglist != null) {
            for (int i = 0; i < nbttaglist.tagCount(); ++i) {
                int j = nbttaglist.getCompoundTagAt(i).getShort("id");
                int k = nbttaglist.getCompoundTagAt(i).getShort("lvl");
                map.put(j, k);
            }
        }

        return map;
    }

    public static void setEnchantments(Int2IntLinkedOpenHashMap enchMap, ItemStack stack) {
        NBTTagList nbttaglist = new NBTTagList();

        for (int i : enchMap.keySet()) {
            Enchantment enchantment = Enchantment.getEnchantmentById(i);

            if (enchantment != null) {
                NBTTagCompound nbttagcompound = new NBTTagCompound();
                nbttagcompound.setShort("id", (short) i);
                nbttagcompound.setShort("lvl", (short) enchMap.get(i));
                nbttaglist.appendTag(nbttagcompound);

                if (stack.getItem() == Items.ENCHANTED_BOOK) {
                    Items.ENCHANTED_BOOK.addEnchantment(stack, new EnchantmentData(enchantment, enchMap.get(i)));
                }
            }
        }

        if (nbttaglist.tagCount() > 0) {
            if (stack.getItem() != Items.ENCHANTED_BOOK) {
                stack.setTagInfo("ench", nbttaglist);
            }
        } else if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag("ench");
        }
    }

    public static int getMaxEnchantmentLevel(int enchID, ItemStack[] stacks) {
        if (stacks == null) {
            return 0;
        } else {
            int i = 0;

            for (ItemStack itemstack : stacks) {
                int j = getEnchantmentLevel(enchID, itemstack);

                if (j > i) {
                    i = j;
                }
            }

            return i;
        }
    }

    private static void applyEnchantmentModifier(IModifier modifier, ItemStack stack) {
        if (stack != null) {
            NBTTagList nbttaglist = stack.getEnchantmentTagList();

            if (nbttaglist != null) {
                for (int i = 0; i < nbttaglist.tagCount(); ++i) {
                    int j = nbttaglist.getCompoundTagAt(i).getShort("id");
                    int k = nbttaglist.getCompoundTagAt(i).getShort("lvl");

                    if (Enchantment.getEnchantmentById(j) != null) {
                        modifier.calculateModifier(Enchantment.getEnchantmentById(j), k);
                    }
                }
            }
        }
    }

    private static void applyEnchantmentModifierArray(IModifier modifier, ItemStack[] stacks) {
        for (ItemStack itemstack : stacks) {
            applyEnchantmentModifier(modifier, itemstack);
        }
    }

    public static int getEnchantmentModifierDamage(ItemStack[] stacks, DamageSource source) {
        enchantmentModifierDamage.damageModifier = 0;
        enchantmentModifierDamage.source = source;
        applyEnchantmentModifierArray(enchantmentModifierDamage, stacks);

        if (enchantmentModifierDamage.damageModifier > 25) {
            enchantmentModifierDamage.damageModifier = 25;
        } else if (enchantmentModifierDamage.damageModifier < 0) {
            enchantmentModifierDamage.damageModifier = 0;
        }

        return (enchantmentModifierDamage.damageModifier + 1 >> 1) + ENCHANTMENT_RAND.nextInt((enchantmentModifierDamage.damageModifier >> 1) + 1);
    }

    public static float getModifierForCreature(ItemStack p_152377_0_, EntityGroup p_152377_1_) {
        enchantmentModifierLiving.livingModifier = 0.0F;
        enchantmentModifierLiving.entityLiving = p_152377_1_;
        applyEnchantmentModifier(enchantmentModifierLiving, p_152377_0_);
        return enchantmentModifierLiving.livingModifier;
    }

    public static void applyThornEnchantments(EntityLivingBase p_151384_0_, Entity p_151384_1_) {
        ENCHANTMENT_ITERATOR_HURT.attacker = p_151384_1_;
        ENCHANTMENT_ITERATOR_HURT.user = p_151384_0_;

        if (p_151384_0_ != null) {
            applyEnchantmentModifierArray(ENCHANTMENT_ITERATOR_HURT, p_151384_0_.getInventory());
        }

        if (p_151384_1_ instanceof EntityPlayer) {
            applyEnchantmentModifier(ENCHANTMENT_ITERATOR_HURT, p_151384_0_.getHeldItem());
        }
    }

    public static void applyArthropodEnchantments(EntityLivingBase p_151385_0_, Entity p_151385_1_) {
        ENCHANTMENT_ITERATOR_DAMAGE.user = p_151385_0_;
        ENCHANTMENT_ITERATOR_DAMAGE.target = p_151385_1_;

        if (p_151385_0_ != null) {
            applyEnchantmentModifierArray(ENCHANTMENT_ITERATOR_DAMAGE, p_151385_0_.getInventory());
        }

        if (p_151385_0_ instanceof EntityPlayer) {
            applyEnchantmentModifier(ENCHANTMENT_ITERATOR_DAMAGE, p_151385_0_.getHeldItem());
        }
    }

    public static int getKnockbackModifier(EntityLivingBase player) {
        return getEnchantmentLevel(Enchantment.KNOCKBACK.effectId, player.getHeldItem());
    }

    public static int getFireAspectModifier(EntityLivingBase player) {
        return getEnchantmentLevel(Enchantment.FIRE_ASPECT.effectId, player.getHeldItem());
    }

    public static int getRespiration(Entity player) {
        return getMaxEnchantmentLevel(Enchantment.RESPIRATION.effectId, player.getInventory());
    }

    public static int getDepthStriderModifier(Entity player) {
        return getMaxEnchantmentLevel(Enchantment.DEPTH_STRIDER.effectId, player.getInventory());
    }

    public static int getEfficiencyModifier(EntityLivingBase player) {
        return getEnchantmentLevel(Enchantment.EFFICIENCY.effectId, player.getHeldItem());
    }

    public static boolean getSilkTouchModifier(EntityLivingBase player) {
        return getEnchantmentLevel(Enchantment.SILK_TOUCH.effectId, player.getHeldItem()) > 0;
    }

    public static int getFortuneModifier(EntityLivingBase player) {
        return getEnchantmentLevel(Enchantment.FORTUNE.effectId, player.getHeldItem());
    }

    public static int getLuckOfSeaModifier(EntityLivingBase player) {
        return getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA.effectId, player.getHeldItem());
    }

    public static int getLureModifier(EntityLivingBase player) {
        return getEnchantmentLevel(Enchantment.LURE.effectId, player.getHeldItem());
    }

    public static int getLootingModifier(EntityLivingBase player) {
        return getEnchantmentLevel(Enchantment.LOOTING.effectId, player.getHeldItem());
    }

    public static boolean getAquaAffinityModifier(EntityLivingBase player) {
        return getMaxEnchantmentLevel(Enchantment.AQUA_AFFINITY.effectId, player.getInventory()) > 0;
    }

    public static ItemStack getEnchantedItem(Enchantment p_92099_0_, EntityLivingBase p_92099_1_) {
        for (ItemStack itemstack : p_92099_1_.getInventory()) {
            if (itemstack != null && getEnchantmentLevel(p_92099_0_.effectId, itemstack) > 0) {
                return itemstack;
            }
        }

        return null;
    }

    public static int calcItemStackEnchantability(Random rand, int enchantNum, int power, ItemStack stack) {
        Item item = stack.getItem();
        int i = item.getItemEnchantability();

        if (i <= 0) {
            return 0;
        } else {
            if (power > 15) {
                power = 15;
            }

            int j = rand.nextInt(8) + 1 + (power >> 1) + rand.nextInt(power + 1);
            return enchantNum == 0 ? Math.max(j / 3, 1) : (enchantNum == 1 ? j * 2 / 3 + 1 : Math.max(j, power * 2));
        }
    }

    public static ItemStack addRandomEnchantment(Random p_77504_0_, ItemStack p_77504_1_, int p_77504_2_) {
        List<EnchantmentData> list = buildEnchantmentList(p_77504_0_, p_77504_1_, p_77504_2_);
        boolean flag = p_77504_1_.getItem() == Items.BOOK;

        if (flag) {
            p_77504_1_.setItem(Items.ENCHANTED_BOOK);
        }

        if (list != null) {
            for (EnchantmentData enchantmentdata : list) {
                if (flag) {
                    Items.ENCHANTED_BOOK.addEnchantment(p_77504_1_, enchantmentdata);
                } else {
                    p_77504_1_.addEnchantment(enchantmentdata.enchantmentobj, enchantmentdata.enchantmentLevel);
                }
            }
        }

        return p_77504_1_;
    }

    public static List<EnchantmentData> buildEnchantmentList(Random randomIn, ItemStack itemStackIn, int p_77513_2_) {
        Item item = itemStackIn.getItem();
        int i = item.getItemEnchantability();

        if (i <= 0) {
            return null;
        } else {
            i = i / 2;
            i = 1 + randomIn.nextInt((i >> 1) + 1) + randomIn.nextInt((i >> 1) + 1);
            int j = i + p_77513_2_;
            float f = (randomIn.nextFloat() + randomIn.nextFloat() - 1.0F) * 0.15F;
            int k = (int) (j * (1.0F + f) + 0.5F);

            if (k < 1) {
                k = 1;
            }

            List<EnchantmentData> list = null;
            Map<Integer, EnchantmentData> map = mapEnchantmentData(k, itemStackIn);

            if (map != null && !map.isEmpty()) {
                EnchantmentData enchantmentdata = WeightedRandom.getRandomItem(randomIn, map.values());

                if (enchantmentdata != null) {
                    list = new ArrayList<>();
                    list.add(enchantmentdata);

                    for (int l = k; randomIn.nextInt(50) <= l; l >>= 1) {
                        Iterator<Integer> iterator = map.keySet().iterator();

                        while (iterator.hasNext()) {
                            Integer integer = iterator.next();
                            boolean flag = true;

                            for (EnchantmentData enchantmentdata1 : list) {
                                if (!enchantmentdata1.enchantmentobj.canApplyTogether(Enchantment.getEnchantmentById(integer))) {
                                    flag = false;
                                    break;
                                }
                            }

                            if (!flag) {
                                iterator.remove();
                            }
                        }

                        if (!map.isEmpty()) {
                            EnchantmentData enchantmentdata2 = WeightedRandom.getRandomItem(randomIn, map.values());
                            list.add(enchantmentdata2);
                        }
                    }
                }
            }

            return list;
        }
    }

    public static Map<Integer, EnchantmentData> mapEnchantmentData(int p_77505_0_, ItemStack p_77505_1_) {
        Item item = p_77505_1_.getItem();
        Map<Integer, EnchantmentData> map = null;
        boolean flag = p_77505_1_.getItem() == Items.BOOK;

        for (Enchantment enchantment : Enchantment.ENCHANTMENTS_BOOK_LIST) {
            if (enchantment != null && (enchantment.type.canEnchantItem(item) || flag)) {
                for (int i = enchantment.getMinLevel(); i <= enchantment.getMaxLevel(); ++i) {
                    if (p_77505_0_ >= enchantment.getMinEnchantability(i) && p_77505_0_ <= enchantment.getMaxEnchantability(i)) {
                        if (map == null) {
                            map = new HashMap<>();
                        }

                        map.put(enchantment.effectId, new EnchantmentData(enchantment, i));
                    }
                }
            }
        }

        return map;
    }

    static final class DamageIterator implements IModifier {
        public EntityLivingBase user;
        public Entity target;

        private DamageIterator() {
        }

        public void calculateModifier(Enchantment enchantmentIn, int enchantmentLevel) {
            enchantmentIn.onEntityDamaged(this.user, this.target, enchantmentLevel);
        }
    }

    static final class HurtIterator implements IModifier {
        public EntityLivingBase user;
        public Entity attacker;

        private HurtIterator() {
        }

        public void calculateModifier(Enchantment enchantmentIn, int enchantmentLevel) {
            enchantmentIn.onUserHurt(this.user, this.attacker, enchantmentLevel);
        }
    }

    interface IModifier {
        void calculateModifier(Enchantment enchantmentIn, int enchantmentLevel);
    }

    static final class ModifierDamage implements IModifier {
        public int damageModifier;
        public DamageSource source;

        private ModifierDamage() {
        }

        public void calculateModifier(Enchantment enchantmentIn, int enchantmentLevel) {
            this.damageModifier += enchantmentIn.calcModifierDamage(enchantmentLevel, this.source);
        }
    }

    static final class ModifierLiving implements IModifier {
        public float livingModifier;
        public EntityGroup entityLiving;

        private ModifierLiving() {
        }

        public void calculateModifier(Enchantment enchantmentIn, int enchantmentLevel) {
            this.livingModifier += enchantmentIn.calcDamageByCreature(enchantmentLevel, this.entityLiving);
        }
    }
}
