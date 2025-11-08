package net.minecraft.item.crafting;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;

public class RecipesFood {
    public void addRecipes(CraftingManager p_77608_1_) {
        p_77608_1_.addShapelessRecipe(new ItemStack(Items.MUSHROOM_STEW), Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Items.BOWL);
        p_77608_1_.addRecipe(new ItemStack(Items.COOKIE, 8), "#X#", 'X', new ItemStack(Items.DYE, 1, DyeColor.BROWN.getDyeDamage()), '#', Items.WHEAT);
        p_77608_1_.addRecipe(new ItemStack(Items.RABBIT_STEW), " R ", "CPM", " B ", 'R', new ItemStack(Items.COOKED_RABBIT), 'C', Items.CARROT, 'P', Items.BAKED_POTATO, 'M', Blocks.BROWN_MUSHROOM, 'B', Items.BOWL);
        p_77608_1_.addRecipe(new ItemStack(Items.RABBIT_STEW), " R ", "CPD", " B ", 'R', new ItemStack(Items.COOKED_RABBIT), 'C', Items.CARROT, 'P', Items.BAKED_POTATO, 'D', Blocks.RED_MUSHROOM, 'B', Items.BOWL);
        p_77608_1_.addRecipe(new ItemStack(Blocks.MELON_BLOCK), "MMM", "MMM", "MMM", 'M', Items.MELON);
        p_77608_1_.addRecipe(new ItemStack(Items.MELON_SEEDS), "M", 'M', Items.MELON);
        p_77608_1_.addRecipe(new ItemStack(Items.PUMPKIN_SEEDS, 4), "M", 'M', Blocks.PUMPKIN);
        p_77608_1_.addShapelessRecipe(new ItemStack(Items.PUMPKIN_PIE), Blocks.PUMPKIN, Items.SUGAR, Items.EGG);
        p_77608_1_.addShapelessRecipe(new ItemStack(Items.FERMENTED_SPIDER_EYE), Items.SPIDER_EYE, Blocks.BROWN_MUSHROOM, Items.SUGAR);
        p_77608_1_.addShapelessRecipe(new ItemStack(Items.BLAZE_POWDER, 2), Items.BLAZE_ROD);
        p_77608_1_.addShapelessRecipe(new ItemStack(Items.MAGMA_CREAM), Items.BLAZE_POWDER, Items.SLIME_BALL);
    }
}
