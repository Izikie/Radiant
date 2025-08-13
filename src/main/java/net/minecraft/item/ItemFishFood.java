package net.minecraft.item;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.world.World;

import java.util.List;

public class ItemFishFood extends ItemFood {
    private final boolean cooked;

    public ItemFishFood(boolean cooked) {
        super(0, 0.0F, false);
        this.cooked = cooked;
    }

    public int getHealAmount(ItemStack stack) {
        FishType itemfishfood$fishtype = FishType.byItemStack(stack);
        return this.cooked && itemfishfood$fishtype.canCook() ? itemfishfood$fishtype.getCookedHealAmount() : itemfishfood$fishtype.getUncookedHealAmount();
    }

    public float getSaturationModifier(ItemStack stack) {
        FishType itemfishfood$fishtype = FishType.byItemStack(stack);
        return this.cooked && itemfishfood$fishtype.canCook() ? itemfishfood$fishtype.getCookedSaturationModifier() : itemfishfood$fishtype.getUncookedSaturationModifier();
    }

    public String getPotionEffect(ItemStack stack) {
        return FishType.byItemStack(stack) == FishType.PUFFERFISH ? PotionHelper.PUFFERFISH_EFFECT : null;
    }

    protected void onFoodEaten(ItemStack stack, World worldIn, EntityPlayer player) {
        FishType itemfishfood$fishtype = FishType.byItemStack(stack);

        if (itemfishfood$fishtype == FishType.PUFFERFISH) {
            player.addPotionEffect(new PotionEffect(Potion.POISON.id, 1200, 3));
            player.addPotionEffect(new PotionEffect(Potion.HUNGER.id, 300, 2));
            player.addPotionEffect(new PotionEffect(Potion.CONFUSION.id, 300, 1));
        }

        super.onFoodEaten(stack, worldIn, player);
    }

    public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems) {
        for (FishType itemfishfood$fishtype : FishType.values()) {
            if (!this.cooked || itemfishfood$fishtype.canCook()) {
                subItems.add(new ItemStack(this, 1, itemfishfood$fishtype.getMetadata()));
            }
        }
    }

    public String getUnlocalizedName(ItemStack stack) {
        FishType itemfishfood$fishtype = FishType.byItemStack(stack);
        return this.getUnlocalizedName() + "." + itemfishfood$fishtype.getUnlocalizedName() + "." + (this.cooked && itemfishfood$fishtype.canCook() ? "cooked" : "raw");
    }

    public enum FishType {
        COD(0, "cod", 2, 0.1F, 5, 0.6F),
        SALMON(1, "salmon", 2, 0.1F, 6, 0.8F),
        CLOWNFISH(2, "clownfish", 1, 0.1F),
        PUFFERFISH(3, "pufferfish", 1, 0.1F);

        private static final Int2ObjectOpenHashMap<FishType> META_LOOKUP = new Int2ObjectOpenHashMap<>();
        private final int meta;
        private final String unlocalizedName;
        private final int uncookedHealAmount;
        private final float uncookedSaturationModifier;
        private final int cookedHealAmount;
        private final float cookedSaturationModifier;
        private final boolean cookable;

        FishType(int meta, String unlocalizedName, int uncookedHeal, float uncookedSaturation, int cookedHeal, float cookedSaturation) {
            this.meta = meta;
            this.unlocalizedName = unlocalizedName;
            this.uncookedHealAmount = uncookedHeal;
            this.uncookedSaturationModifier = uncookedSaturation;
            this.cookedHealAmount = cookedHeal;
            this.cookedSaturationModifier = cookedSaturation;
            this.cookable = true;
        }

        FishType(int meta, String unlocalizedName, int uncookedHeal, float uncookedSaturation) {
            this.meta = meta;
            this.unlocalizedName = unlocalizedName;
            this.uncookedHealAmount = uncookedHeal;
            this.uncookedSaturationModifier = uncookedSaturation;
            this.cookedHealAmount = 0;
            this.cookedSaturationModifier = 0.0F;
            this.cookable = false;
        }

        public int getMetadata() {
            return this.meta;
        }

        public String getUnlocalizedName() {
            return this.unlocalizedName;
        }

        public int getUncookedHealAmount() {
            return this.uncookedHealAmount;
        }

        public float getUncookedSaturationModifier() {
            return this.uncookedSaturationModifier;
        }

        public int getCookedHealAmount() {
            return this.cookedHealAmount;
        }

        public float getCookedSaturationModifier() {
            return this.cookedSaturationModifier;
        }

        public boolean canCook() {
            return this.cookable;
        }

        public static FishType byMetadata(int meta) {
            FishType itemfishfood$fishtype = META_LOOKUP.get(meta);
            return itemfishfood$fishtype == null ? COD : itemfishfood$fishtype;
        }

        public static FishType byItemStack(ItemStack stack) {
            return stack.getItem() instanceof ItemFishFood ? byMetadata(stack.getMetadata()) : COD;
        }

        static {
            for (FishType itemfishfood$fishtype : values()) {
                META_LOOKUP.put(itemfishfood$fishtype.getMetadata(), itemfishfood$fishtype);
            }
        }
    }
}
