package net.minecraft.item.crafting;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;

public class RecipesIngots {
    private final Object[][] recipeItems = new Object[][]{{Blocks.GOLD_BLOCK, new ItemStack(Items.GOLD_INGOT, 9)}, {Blocks.IRON_BLOCK, new ItemStack(Items.IRON_INGOT, 9)}, {Blocks.DIAMOND_BLOCK, new ItemStack(Items.DIAMOND, 9)}, {Blocks.EMERALD_BLOCK, new ItemStack(Items.EMERALD, 9)}, {Blocks.LAPIS_BLOCK, new ItemStack(Items.DYE, 9, DyeColor.BLUE.getDyeDamage())}, {Blocks.REDSTONE_BLOCK, new ItemStack(Items.REDSTONE, 9)}, {Blocks.COAL_BLOCK, new ItemStack(Items.COAL, 9, 0)}, {Blocks.HAY_BLOCK, new ItemStack(Items.WHEAT, 9)}, {Blocks.SLIME_BLOCK, new ItemStack(Items.SLIME_BALL, 9)}};

    public void addRecipes(CraftingManager p_77590_1_) {
        for (Object[] recipeItem : this.recipeItems) {
            Block block = (Block) recipeItem[0];
            ItemStack itemstack = (ItemStack) recipeItem[1];
            p_77590_1_.addRecipe(new ItemStack(block), "###", "###", "###", '#', itemstack);
            p_77590_1_.addRecipe(itemstack, "#", '#', block);
        }

        p_77590_1_.addRecipe(new ItemStack(Items.GOLD_INGOT), "###", "###", "###", '#', Items.GOLD_NUGGET);
        p_77590_1_.addRecipe(new ItemStack(Items.GOLD_NUGGET, 9), "#", '#', Items.GOLD_INGOT);
    }
}
