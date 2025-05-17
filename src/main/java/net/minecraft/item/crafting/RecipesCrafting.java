package net.minecraft.item.crafting;

import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockPrismarine;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.BlockRedSandstone;
import net.minecraft.block.BlockSand;
import net.minecraft.block.BlockSandStone;
import net.minecraft.block.BlockStone;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.block.BlockStoneSlab;
import net.minecraft.block.BlockStoneSlabNew;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;

public class RecipesCrafting {
    public void addRecipes(CraftingManager p_77589_1_) {
        p_77589_1_.addRecipe(new ItemStack(Blocks.CHEST), "###", "# #", "###", '#', Blocks.PLANKS);
        p_77589_1_.addRecipe(new ItemStack(Blocks.TRAPPED_CHEST), "#-", '#', Blocks.CHEST, '-', Blocks.TRIPWIRE_HOOK);
        p_77589_1_.addRecipe(new ItemStack(Blocks.ENDER_CHEST), "###", "#E#", "###", '#', Blocks.OBSIDIAN, 'E', Items.ENDER_EYE);
        p_77589_1_.addRecipe(new ItemStack(Blocks.FURNACE), "###", "# #", "###", '#', Blocks.COBBLESTONE);
        p_77589_1_.addRecipe(new ItemStack(Blocks.CRAFTING_TABLE), "##", "##", '#', Blocks.PLANKS);
        p_77589_1_.addRecipe(new ItemStack(Blocks.SANDSTONE), "##", "##", '#', new ItemStack(Blocks.SAND, 1, BlockSand.SandType.SAND.getMetadata()));
        p_77589_1_.addRecipe(new ItemStack(Blocks.RED_SANDSTONE), "##", "##", '#', new ItemStack(Blocks.SAND, 1, BlockSand.SandType.RED_SAND.getMetadata()));
        p_77589_1_.addRecipe(new ItemStack(Blocks.SANDSTONE, 4, BlockSandStone.SandStoneType.SMOOTH.getMetadata()), "##", "##", '#', new ItemStack(Blocks.SANDSTONE, 1, BlockSandStone.SandStoneType.DEFAULT.getMetadata()));
        p_77589_1_.addRecipe(new ItemStack(Blocks.RED_SANDSTONE, 4, BlockRedSandstone.RedSandStoneType.SMOOTH.getMetadata()), "##", "##", '#', new ItemStack(Blocks.RED_SANDSTONE, 1, BlockRedSandstone.RedSandStoneType.DEFAULT.getMetadata()));
        p_77589_1_.addRecipe(new ItemStack(Blocks.SANDSTONE, 1, BlockSandStone.SandStoneType.CHISELED.getMetadata()), "#", "#", '#', new ItemStack(Blocks.STONE_SLAB, 1, BlockStoneSlab.EnumType.SAND.getMetadata()));
        p_77589_1_.addRecipe(new ItemStack(Blocks.RED_SANDSTONE, 1, BlockRedSandstone.RedSandStoneType.CHISELED.getMetadata()), "#", "#", '#', new ItemStack(Blocks.BLOCK_SLAB, 1, BlockStoneSlabNew.EnumType.RED_SANDSTONE.getMetadata()));
        p_77589_1_.addRecipe(new ItemStack(Blocks.QUARTZ_BLOCK, 1, BlockQuartz.QuartzType.CHISELED.getMetadata()), "#", "#", '#', new ItemStack(Blocks.STONE_SLAB, 1, BlockStoneSlab.EnumType.QUARTZ.getMetadata()));
        p_77589_1_.addRecipe(new ItemStack(Blocks.QUARTZ_BLOCK, 2, BlockQuartz.QuartzType.LINES_Y.getMetadata()), "#", "#", '#', new ItemStack(Blocks.QUARTZ_BLOCK, 1, BlockQuartz.QuartzType.DEFAULT.getMetadata()));
        p_77589_1_.addRecipe(new ItemStack(Blocks.STONEBRICK, 4), "##", "##", '#', new ItemStack(Blocks.STONE, 1, BlockStone.StoneType.STONE.getMetadata()));
        p_77589_1_.addRecipe(new ItemStack(Blocks.STONEBRICK, 1, BlockStoneBrick.CHISELED_META), "#", "#", '#', new ItemStack(Blocks.STONE_SLAB, 1, BlockStoneSlab.EnumType.SMOOTHBRICK.getMetadata()));
        p_77589_1_.addShapelessRecipe(new ItemStack(Blocks.STONEBRICK, 1, BlockStoneBrick.MOSSY_META), Blocks.STONEBRICK, Blocks.VINE);
        p_77589_1_.addShapelessRecipe(new ItemStack(Blocks.MOSSY_COBBLESTONE, 1), Blocks.COBBLESTONE, Blocks.VINE);
        p_77589_1_.addRecipe(new ItemStack(Blocks.IRON_BARS, 16), "###", "###", '#', Items.IRON_INGOT);
        p_77589_1_.addRecipe(new ItemStack(Blocks.GLASS_PANE, 16), "###", "###", '#', Blocks.GLASS);
        p_77589_1_.addRecipe(new ItemStack(Blocks.REDSTONE_LAMP, 1), " R ", "RGR", " R ", 'R', Items.REDSTONE, 'G', Blocks.GLOWSTONE);
        p_77589_1_.addRecipe(new ItemStack(Blocks.BEACON, 1), "GGG", "GSG", "OOO", 'G', Blocks.GLASS, 'S', Items.NETHER_STAR, 'O', Blocks.OBSIDIAN);
        p_77589_1_.addRecipe(new ItemStack(Blocks.NETHER_BRICK, 1), "NN", "NN", 'N', Items.NETHERBRICK);
        p_77589_1_.addRecipe(new ItemStack(Blocks.STONE, 2, BlockStone.StoneType.DIORITE.getMetadata()), "CQ", "QC", 'C', Blocks.COBBLESTONE, 'Q', Items.QUARTZ);
        p_77589_1_.addShapelessRecipe(new ItemStack(Blocks.STONE, 1, BlockStone.StoneType.GRANITE.getMetadata()), new ItemStack(Blocks.STONE, 1, BlockStone.StoneType.DIORITE.getMetadata()), Items.QUARTZ);
        p_77589_1_.addShapelessRecipe(new ItemStack(Blocks.STONE, 2, BlockStone.StoneType.ANDESITE.getMetadata()), new ItemStack(Blocks.STONE, 1, BlockStone.StoneType.DIORITE.getMetadata()), Blocks.COBBLESTONE);
        p_77589_1_.addRecipe(new ItemStack(Blocks.DIRT, 4, BlockDirt.DirtType.COARSE_DIRT.getMetadata()), "DG", "GD", 'D', new ItemStack(Blocks.DIRT, 1, BlockDirt.DirtType.DIRT.getMetadata()), 'G', Blocks.GRAVEL);
        p_77589_1_.addRecipe(new ItemStack(Blocks.STONE, 4, BlockStone.StoneType.DIORITE_SMOOTH.getMetadata()), "SS", "SS", 'S', new ItemStack(Blocks.STONE, 1, BlockStone.StoneType.DIORITE.getMetadata()));
        p_77589_1_.addRecipe(new ItemStack(Blocks.STONE, 4, BlockStone.StoneType.GRANITE_SMOOTH.getMetadata()), "SS", "SS", 'S', new ItemStack(Blocks.STONE, 1, BlockStone.StoneType.GRANITE.getMetadata()));
        p_77589_1_.addRecipe(new ItemStack(Blocks.STONE, 4, BlockStone.StoneType.ANDESITE_SMOOTH.getMetadata()), "SS", "SS", 'S', new ItemStack(Blocks.STONE, 1, BlockStone.StoneType.ANDESITE.getMetadata()));
        p_77589_1_.addRecipe(new ItemStack(Blocks.PRISMARINE, 1, BlockPrismarine.ROUGH_META), "SS", "SS", 'S', Items.PRISMARINE_SHARD);
        p_77589_1_.addRecipe(new ItemStack(Blocks.PRISMARINE, 1, BlockPrismarine.BRICKS_META), "SSS", "SSS", "SSS", 'S', Items.PRISMARINE_SHARD);
        p_77589_1_.addRecipe(new ItemStack(Blocks.PRISMARINE, 1, BlockPrismarine.DARK_META), "SSS", "SIS", "SSS", 'S', Items.PRISMARINE_SHARD, 'I', new ItemStack(Items.DYE, 1, DyeColor.BLACK.getDyeDamage()));
        p_77589_1_.addRecipe(new ItemStack(Blocks.SEA_LANTERN, 1, 0), "SCS", "CCC", "SCS", 'S', Items.PRISMARINE_SHARD, 'C', Items.PRISMARINE_CRYSTALS);
    }
}
