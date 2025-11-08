package net.minecraft.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.chat.IChatComponent;
import net.minecraft.world.village.MerchantRecipe;
import net.minecraft.world.village.MerchantRecipeList;

public interface IMerchant {
    void setCustomer(EntityPlayer p_70932_1_);

    EntityPlayer getCustomer();

    MerchantRecipeList getRecipes(EntityPlayer p_70934_1_);

    void setRecipes(MerchantRecipeList recipeList);

    void useRecipe(MerchantRecipe recipe);

    void verifySellingItem(ItemStack stack);

    IChatComponent getDisplayName();
}
