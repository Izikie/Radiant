package net.minecraft.item;

import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;

public class ItemBow extends Item {
    public static final String[] BOW_PULL_ICON_NAMES = new String[]{"pulling_0", "pulling_1", "pulling_2"};

    public ItemBow() {
        this.maxStackSize = 1;
        this.setMaxDamage(384);
        this.setCreativeTab(CreativeTabs.TAB_COMBAT);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityPlayer playerIn, int timeLeft) {
        boolean flag = playerIn.capabilities.isCreativeMode || EnchantmentHelper.getEnchantmentLevel(Enchantment.INFINITY.effectId, stack) > 0;

        if (flag || playerIn.inventory.hasItem(Items.ARROW)) {
            int i = this.getMaxItemUseDuration(stack) - timeLeft;
            float f = i / 20.0F;
            f = (f * f + f * 2.0F) / 3.0F;

            if (f < 0.1D) {
                return;
            }

            if (f > 1.0F) {
                f = 1.0F;
            }

            EntityArrow entityarrow = new EntityArrow(worldIn, playerIn, f * 2.0F);

            if (f == 1.0F) {
                entityarrow.setIsCritical(true);
            }

            int j = EnchantmentHelper.getEnchantmentLevel(Enchantment.POWER.effectId, stack);

            if (j > 0) {
                entityarrow.setDamage(entityarrow.getDamage() + j * 0.5D + 0.5D);
            }

            int k = EnchantmentHelper.getEnchantmentLevel(Enchantment.PUNCH.effectId, stack);

            if (k > 0) {
                entityarrow.setKnockbackStrength(k);
            }

            if (EnchantmentHelper.getEnchantmentLevel(Enchantment.FLAME.effectId, stack) > 0) {
                entityarrow.setFire(100);
            }

            stack.damageItem(1, playerIn);
            worldIn.playSoundAtEntity(playerIn, "random.bow", 1.0F, 1.0F / (ITEM_RAND.nextFloat() * 0.4F + 1.2F) + f * 0.5F);

            if (flag) {
                entityarrow.canBePickedUp = 2;
            } else {
                playerIn.inventory.consumeInventoryItem(Items.ARROW);
            }

            playerIn.triggerAchievement(StatList.OBJECT_USE_STATS[Item.getIdFromItem(this)]);

            if (!worldIn.isRemote) {
                worldIn.spawnEntityInWorld(entityarrow);
            }
        }
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityPlayer playerIn) {
        return stack;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAction getItemUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn) {
        if (playerIn.capabilities.isCreativeMode || playerIn.inventory.hasItem(Items.ARROW)) {
            playerIn.setItemInUse(itemStackIn, this.getMaxItemUseDuration(itemStackIn));
        }

        return itemStackIn;
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }
}
