package net.minecraft.item.crafting;

import net.minecraft.block.*;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CraftingManager {
    private static final CraftingManager INSTANCE = new CraftingManager();
    private final List<IRecipe> recipes = new ArrayList<>();

    public static CraftingManager getInstance() {
        return INSTANCE;
    }

    private CraftingManager() {
        (new RecipesTools()).addRecipes(this);
        (new RecipesWeapons()).addRecipes(this);
        (new RecipesIngots()).addRecipes(this);
        (new RecipesFood()).addRecipes(this);
        (new RecipesCrafting()).addRecipes(this);
        (new RecipesArmor()).addRecipes(this);
        (new RecipesDyes()).addRecipes(this);
        this.recipes.add(new RecipesArmorDyes());
        this.recipes.add(new RecipeBookCloning());
        this.recipes.add(new RecipesMapCloning());
        this.recipes.add(new RecipesMapExtending());
        this.recipes.add(new RecipeFireworks());
        this.recipes.add(new RecipeRepairItem());
        (new RecipesBanners()).addRecipes(this);
        this.addRecipe(new ItemStack(Items.PAPER, 3), "###", '#', Items.REEDS);
        this.addShapelessRecipe(new ItemStack(Items.BOOK, 1), Items.PAPER, Items.PAPER, Items.PAPER, Items.LEATHER);
        this.addShapelessRecipe(new ItemStack(Items.WRITABLE_BOOK, 1), Items.BOOK, new ItemStack(Items.DYE, 1, DyeColor.BLACK.getDyeDamage()), Items.FEATHER);
        this.addRecipe(new ItemStack(Blocks.OAK_FENCE, 3), "W#W", "W#W", '#', Items.STICK, 'W', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.OAK.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.BIRCH_FENCE, 3), "W#W", "W#W", '#', Items.STICK, 'W', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.BIRCH.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.SPRUCE_FENCE, 3), "W#W", "W#W", '#', Items.STICK, 'W', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.SPRUCE.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.JUNGLE_FENCE, 3), "W#W", "W#W", '#', Items.STICK, 'W', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.JUNGLE.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.ACACIA_FENCE, 3), "W#W", "W#W", '#', Items.STICK, 'W', new ItemStack(Blocks.PLANKS, 1, 4 + BlockPlanks.WoodType.ACACIA.getMetadata() - 4));
        this.addRecipe(new ItemStack(Blocks.DARK_OAK_FENCE, 3), "W#W", "W#W", '#', Items.STICK, 'W', new ItemStack(Blocks.PLANKS, 1, 4 + BlockPlanks.WoodType.DARK_OAK.getMetadata() - 4));
        this.addRecipe(new ItemStack(Blocks.COBBLESTONE_WALL, 6, BlockWall.WallType.NORMAL.getMetadata()), "###", "###", '#', Blocks.COBBLESTONE);
        this.addRecipe(new ItemStack(Blocks.COBBLESTONE_WALL, 6, BlockWall.WallType.MOSSY.getMetadata()), "###", "###", '#', Blocks.MOSSY_COBBLESTONE);
        this.addRecipe(new ItemStack(Blocks.NETHER_BRICK_FENCE, 6), "###", "###", '#', Blocks.NETHER_BRICK);
        this.addRecipe(new ItemStack(Blocks.OAK_FENCE_GATE, 1), "#W#", "#W#", '#', Items.STICK, 'W', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.OAK.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.BIRCH_FENCE_GATE, 1), "#W#", "#W#", '#', Items.STICK, 'W', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.BIRCH.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.SPRUCE_FENCE_GATE, 1), "#W#", "#W#", '#', Items.STICK, 'W', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.SPRUCE.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.JUNGLE_FENCE_GATE, 1), "#W#", "#W#", '#', Items.STICK, 'W', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.JUNGLE.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.ACACIA_FENCE_GATE, 1), "#W#", "#W#", '#', Items.STICK, 'W', new ItemStack(Blocks.PLANKS, 1, 4 + BlockPlanks.WoodType.ACACIA.getMetadata() - 4));
        this.addRecipe(new ItemStack(Blocks.DARK_OAK_FENCE_GATE, 1), "#W#", "#W#", '#', Items.STICK, 'W', new ItemStack(Blocks.PLANKS, 1, 4 + BlockPlanks.WoodType.DARK_OAK.getMetadata() - 4));
        this.addRecipe(new ItemStack(Blocks.JUKEBOX, 1), "###", "#X#", "###", '#', Blocks.PLANKS, 'X', Items.DIAMOND);
        this.addRecipe(new ItemStack(Items.LEAD, 2), "~~ ", "~O ", "  ~", '~', Items.STRING, 'O', Items.SLIME_BALL);
        this.addRecipe(new ItemStack(Blocks.NOTEBLOCK, 1), "###", "#X#", "###", '#', Blocks.PLANKS, 'X', Items.REDSTONE);
        this.addRecipe(new ItemStack(Blocks.BOOKSHELF, 1), "###", "XXX", "###", '#', Blocks.PLANKS, 'X', Items.BOOK);
        this.addRecipe(new ItemStack(Blocks.SNOW, 1), "##", "##", '#', Items.SNOWBALL);
        this.addRecipe(new ItemStack(Blocks.SNOW_LAYER, 6), "###", '#', Blocks.SNOW);
        this.addRecipe(new ItemStack(Blocks.CLAY, 1), "##", "##", '#', Items.CLAY_BALL);
        this.addRecipe(new ItemStack(Blocks.BRICK_BLOCK, 1), "##", "##", '#', Items.BRICK);
        this.addRecipe(new ItemStack(Blocks.GLOWSTONE, 1), "##", "##", '#', Items.GLOWSTONE_DUST);
        this.addRecipe(new ItemStack(Blocks.QUARTZ_BLOCK, 1), "##", "##", '#', Items.QUARTZ);
        this.addRecipe(new ItemStack(Blocks.WOOL, 1), "##", "##", '#', Items.STRING);
        this.addRecipe(new ItemStack(Blocks.TNT, 1), "X#X", "#X#", "X#X", 'X', Items.GUNPOWDER, '#', Blocks.SAND);
        this.addRecipe(new ItemStack(Blocks.STONE_SLAB, 6, BlockStoneSlab.EnumType.COBBLESTONE.getMetadata()), "###", '#', Blocks.COBBLESTONE);
        this.addRecipe(new ItemStack(Blocks.STONE_SLAB, 6, BlockStoneSlab.EnumType.STONE.getMetadata()), "###", '#', new ItemStack(Blocks.STONE, BlockStone.StoneType.STONE.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.STONE_SLAB, 6, BlockStoneSlab.EnumType.SAND.getMetadata()), "###", '#', Blocks.SANDSTONE);
        this.addRecipe(new ItemStack(Blocks.STONE_SLAB, 6, BlockStoneSlab.EnumType.BRICK.getMetadata()), "###", '#', Blocks.BRICK_BLOCK);
        this.addRecipe(new ItemStack(Blocks.STONE_SLAB, 6, BlockStoneSlab.EnumType.SMOOTHBRICK.getMetadata()), "###", '#', Blocks.STONEBRICK);
        this.addRecipe(new ItemStack(Blocks.STONE_SLAB, 6, BlockStoneSlab.EnumType.NETHERBRICK.getMetadata()), "###", '#', Blocks.NETHER_BRICK);
        this.addRecipe(new ItemStack(Blocks.STONE_SLAB, 6, BlockStoneSlab.EnumType.QUARTZ.getMetadata()), "###", '#', Blocks.QUARTZ_BLOCK);
        this.addRecipe(new ItemStack(Blocks.BLOCK_SLAB, 6, BlockStoneSlabNew.EnumType.RED_SANDSTONE.getMetadata()), "###", '#', Blocks.RED_SANDSTONE);
        this.addRecipe(new ItemStack(Blocks.WOODEN_SLAB, 6, 0), "###", '#', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.OAK.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.WOODEN_SLAB, 6, BlockPlanks.WoodType.BIRCH.getMetadata()), "###", '#', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.BIRCH.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.WOODEN_SLAB, 6, BlockPlanks.WoodType.SPRUCE.getMetadata()), "###", '#', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.SPRUCE.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.WOODEN_SLAB, 6, BlockPlanks.WoodType.JUNGLE.getMetadata()), "###", '#', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.JUNGLE.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.WOODEN_SLAB, 6, 4 + BlockPlanks.WoodType.ACACIA.getMetadata() - 4), "###", '#', new ItemStack(Blocks.PLANKS, 1, 4 + BlockPlanks.WoodType.ACACIA.getMetadata() - 4));
        this.addRecipe(new ItemStack(Blocks.WOODEN_SLAB, 6, 4 + BlockPlanks.WoodType.DARK_OAK.getMetadata() - 4), "###", '#', new ItemStack(Blocks.PLANKS, 1, 4 + BlockPlanks.WoodType.DARK_OAK.getMetadata() - 4));
        this.addRecipe(new ItemStack(Blocks.LADDER, 3), "# #", "###", "# #", '#', Items.STICK);
        this.addRecipe(new ItemStack(Items.OAK_DOOR, 3), "##", "##", "##", '#', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.OAK.getMetadata()));
        this.addRecipe(new ItemStack(Items.SPRUCE_DOOR, 3), "##", "##", "##", '#', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.SPRUCE.getMetadata()));
        this.addRecipe(new ItemStack(Items.BIRCH_DOOR, 3), "##", "##", "##", '#', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.BIRCH.getMetadata()));
        this.addRecipe(new ItemStack(Items.JUNGLE_DOOR, 3), "##", "##", "##", '#', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.JUNGLE.getMetadata()));
        this.addRecipe(new ItemStack(Items.ACACIA_DOOR, 3), "##", "##", "##", '#', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.ACACIA.getMetadata()));
        this.addRecipe(new ItemStack(Items.DARK_OAK_DOOR, 3), "##", "##", "##", '#', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.DARK_OAK.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.TRAPDOOR, 2), "###", "###", '#', Blocks.PLANKS);
        this.addRecipe(new ItemStack(Items.IRON_DOOR, 3), "##", "##", "##", '#', Items.IRON_INGOT);
        this.addRecipe(new ItemStack(Blocks.IRON_TRAPDOOR, 1), "##", "##", '#', Items.IRON_INGOT);
        this.addRecipe(new ItemStack(Items.SIGN, 3), "###", "###", " X ", '#', Blocks.PLANKS, 'X', Items.STICK);
        this.addRecipe(new ItemStack(Items.CAKE, 1), "AAA", "BEB", "CCC", 'A', Items.MILK_BUCKET, 'B', Items.SUGAR, 'C', Items.WHEAT, 'E', Items.EGG);
        this.addRecipe(new ItemStack(Items.SUGAR, 1), "#", '#', Items.REEDS);
        this.addRecipe(new ItemStack(Blocks.PLANKS, 4, BlockPlanks.WoodType.OAK.getMetadata()), "#", '#', new ItemStack(Blocks.LOG, 1, BlockPlanks.WoodType.OAK.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.PLANKS, 4, BlockPlanks.WoodType.SPRUCE.getMetadata()), "#", '#', new ItemStack(Blocks.LOG, 1, BlockPlanks.WoodType.SPRUCE.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.PLANKS, 4, BlockPlanks.WoodType.BIRCH.getMetadata()), "#", '#', new ItemStack(Blocks.LOG, 1, BlockPlanks.WoodType.BIRCH.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.PLANKS, 4, BlockPlanks.WoodType.JUNGLE.getMetadata()), "#", '#', new ItemStack(Blocks.LOG, 1, BlockPlanks.WoodType.JUNGLE.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.PLANKS, 4, 4 + BlockPlanks.WoodType.ACACIA.getMetadata() - 4), "#", '#', new ItemStack(Blocks.LOG_2, 1, BlockPlanks.WoodType.ACACIA.getMetadata() - 4));
        this.addRecipe(new ItemStack(Blocks.PLANKS, 4, 4 + BlockPlanks.WoodType.DARK_OAK.getMetadata() - 4), "#", '#', new ItemStack(Blocks.LOG_2, 1, BlockPlanks.WoodType.DARK_OAK.getMetadata() - 4));
        this.addRecipe(new ItemStack(Items.STICK, 4), "#", "#", '#', Blocks.PLANKS);
        this.addRecipe(new ItemStack(Blocks.TORCH, 4), "X", "#", 'X', Items.COAL, '#', Items.STICK);
        this.addRecipe(new ItemStack(Blocks.TORCH, 4), "X", "#", 'X', new ItemStack(Items.COAL, 1, 1), '#', Items.STICK);
        this.addRecipe(new ItemStack(Items.BOWL, 4), "# #", " # ", '#', Blocks.PLANKS);
        this.addRecipe(new ItemStack(Items.GLASS_BOTTLE, 3), "# #", " # ", '#', Blocks.GLASS);
        this.addRecipe(new ItemStack(Blocks.RAIL, 16), "X X", "X#X", "X X", 'X', Items.IRON_INGOT, '#', Items.STICK);
        this.addRecipe(new ItemStack(Blocks.GOLDEN_RAIL, 6), "X X", "X#X", "XRX", 'X', Items.GOLD_INGOT, 'R', Items.REDSTONE, '#', Items.STICK);
        this.addRecipe(new ItemStack(Blocks.ACTIVATOR_RAIL, 6), "XSX", "X#X", "XSX", 'X', Items.IRON_INGOT, '#', Blocks.REDSTONE_TORCH, 'S', Items.STICK);
        this.addRecipe(new ItemStack(Blocks.DETECTOR_RAIL, 6), "X X", "X#X", "XRX", 'X', Items.IRON_INGOT, 'R', Items.REDSTONE, '#', Blocks.STONE_PRESSURE_PLATE);
        this.addRecipe(new ItemStack(Items.MINECART, 1), "# #", "###", '#', Items.IRON_INGOT);
        this.addRecipe(new ItemStack(Items.CAULDRON, 1), "# #", "# #", "###", '#', Items.IRON_INGOT);
        this.addRecipe(new ItemStack(Items.BREWING_STAND, 1), " B ", "###", '#', Blocks.COBBLESTONE, 'B', Items.BLAZE_ROD);
        this.addRecipe(new ItemStack(Blocks.LIT_PUMPKIN, 1), "A", "B", 'A', Blocks.PUMPKIN, 'B', Blocks.TORCH);
        this.addRecipe(new ItemStack(Items.CHEST_MINECART, 1), "A", "B", 'A', Blocks.CHEST, 'B', Items.MINECART);
        this.addRecipe(new ItemStack(Items.FURNACE_MINECART, 1), "A", "B", 'A', Blocks.FURNACE, 'B', Items.MINECART);
        this.addRecipe(new ItemStack(Items.TNT_MINECART, 1), "A", "B", 'A', Blocks.TNT, 'B', Items.MINECART);
        this.addRecipe(new ItemStack(Items.HOPPER_MINECART, 1), "A", "B", 'A', Blocks.HOPPER, 'B', Items.MINECART);
        this.addRecipe(new ItemStack(Items.BOAT, 1), "# #", "###", '#', Blocks.PLANKS);
        this.addRecipe(new ItemStack(Items.BUCKET, 1), "# #", " # ", '#', Items.IRON_INGOT);
        this.addRecipe(new ItemStack(Items.FLOWER_POT, 1), "# #", " # ", '#', Items.BRICK);
        this.addShapelessRecipe(new ItemStack(Items.FLINT_AND_STEEL, 1), new ItemStack(Items.IRON_INGOT, 1), new ItemStack(Items.FLINT, 1));
        this.addRecipe(new ItemStack(Items.BREAD, 1), "###", '#', Items.WHEAT);
        this.addRecipe(new ItemStack(Blocks.OAK_STAIRS, 4), "#  ", "## ", "###", '#', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.OAK.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.BIRCH_STAIRS, 4), "#  ", "## ", "###", '#', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.BIRCH.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.SPRUCE_STAIRS, 4), "#  ", "## ", "###", '#', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.SPRUCE.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.JUNGLE_STAIRS, 4), "#  ", "## ", "###", '#', new ItemStack(Blocks.PLANKS, 1, BlockPlanks.WoodType.JUNGLE.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.ACACIA_STAIRS, 4), "#  ", "## ", "###", '#', new ItemStack(Blocks.PLANKS, 1, 4 + BlockPlanks.WoodType.ACACIA.getMetadata() - 4));
        this.addRecipe(new ItemStack(Blocks.DARK_OAK_STAIRS, 4), "#  ", "## ", "###", '#', new ItemStack(Blocks.PLANKS, 1, 4 + BlockPlanks.WoodType.DARK_OAK.getMetadata() - 4));
        this.addRecipe(new ItemStack(Items.FISHING_ROD, 1), "  #", " #X", "# X", '#', Items.STICK, 'X', Items.STRING);
        this.addRecipe(new ItemStack(Items.CARROT_ON_A_STICK, 1), "# ", " X", '#', Items.FISHING_ROD, 'X', Items.CARROT);
        this.addRecipe(new ItemStack(Blocks.STONE_STAIRS, 4), "#  ", "## ", "###", '#', Blocks.COBBLESTONE);
        this.addRecipe(new ItemStack(Blocks.BRICK_STAIRS, 4), "#  ", "## ", "###", '#', Blocks.BRICK_BLOCK);
        this.addRecipe(new ItemStack(Blocks.STONE_BRICK_STAIRS, 4), "#  ", "## ", "###", '#', Blocks.STONEBRICK);
        this.addRecipe(new ItemStack(Blocks.NETHER_BRICK_STAIRS, 4), "#  ", "## ", "###", '#', Blocks.NETHER_BRICK);
        this.addRecipe(new ItemStack(Blocks.SANDSTONE_STAIRS, 4), "#  ", "## ", "###", '#', Blocks.SANDSTONE);
        this.addRecipe(new ItemStack(Blocks.RED_SANDSTONE_STAIRS, 4), "#  ", "## ", "###", '#', Blocks.RED_SANDSTONE);
        this.addRecipe(new ItemStack(Blocks.QUARTZ_STAIRS, 4), "#  ", "## ", "###", '#', Blocks.QUARTZ_BLOCK);
        this.addRecipe(new ItemStack(Items.PAINTING, 1), "###", "#X#", "###", '#', Items.STICK, 'X', Blocks.WOOL);
        this.addRecipe(new ItemStack(Items.ITEM_FRAME, 1), "###", "#X#", "###", '#', Items.STICK, 'X', Items.LEATHER);
        this.addRecipe(new ItemStack(Items.GOLDEN_APPLE, 1, 0), "###", "#X#", "###", '#', Items.GOLD_INGOT, 'X', Items.APPLE);
        this.addRecipe(new ItemStack(Items.GOLDEN_APPLE, 1, 1), "###", "#X#", "###", '#', Blocks.GOLD_BLOCK, 'X', Items.APPLE);
        this.addRecipe(new ItemStack(Items.GOLDEN_CARROT, 1, 0), "###", "#X#", "###", '#', Items.GOLD_NUGGET, 'X', Items.CARROT);
        this.addRecipe(new ItemStack(Items.SPECKLED_MELON, 1), "###", "#X#", "###", '#', Items.GOLD_NUGGET, 'X', Items.MELON);
        this.addRecipe(new ItemStack(Blocks.LEVER, 1), "X", "#", '#', Blocks.COBBLESTONE, 'X', Items.STICK);
        this.addRecipe(new ItemStack(Blocks.TRIPWIRE_HOOK, 2), "I", "S", "#", '#', Blocks.PLANKS, 'S', Items.STICK, 'I', Items.IRON_INGOT);
        this.addRecipe(new ItemStack(Blocks.REDSTONE_TORCH, 1), "X", "#", '#', Items.STICK, 'X', Items.REDSTONE);
        this.addRecipe(new ItemStack(Items.REPEATER, 1), "#X#", "III", '#', Blocks.REDSTONE_TORCH, 'X', Items.REDSTONE, 'I', new ItemStack(Blocks.STONE, 1, BlockStone.StoneType.STONE.getMetadata()));
        this.addRecipe(new ItemStack(Items.COMPARATOR, 1), " # ", "#X#", "III", '#', Blocks.REDSTONE_TORCH, 'X', Items.QUARTZ, 'I', new ItemStack(Blocks.STONE, 1, BlockStone.StoneType.STONE.getMetadata()));
        this.addRecipe(new ItemStack(Items.CLOCK, 1), " # ", "#X#", " # ", '#', Items.GOLD_INGOT, 'X', Items.REDSTONE);
        this.addRecipe(new ItemStack(Items.COMPASS, 1), " # ", "#X#", " # ", '#', Items.IRON_INGOT, 'X', Items.REDSTONE);
        this.addRecipe(new ItemStack(Items.MAP, 1), "###", "#X#", "###", '#', Items.PAPER, 'X', Items.COMPASS);
        this.addRecipe(new ItemStack(Blocks.STONE_BUTTON, 1), "#", '#', new ItemStack(Blocks.STONE, 1, BlockStone.StoneType.STONE.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.WOODEN_BUTTON, 1), "#", '#', Blocks.PLANKS);
        this.addRecipe(new ItemStack(Blocks.STONE_PRESSURE_PLATE, 1), "##", '#', new ItemStack(Blocks.STONE, 1, BlockStone.StoneType.STONE.getMetadata()));
        this.addRecipe(new ItemStack(Blocks.WOODEN_PRESSURE_PLATE, 1), "##", '#', Blocks.PLANKS);
        this.addRecipe(new ItemStack(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, 1), "##", '#', Items.IRON_INGOT);
        this.addRecipe(new ItemStack(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, 1), "##", '#', Items.GOLD_INGOT);
        this.addRecipe(new ItemStack(Blocks.DISPENSER, 1), "###", "#X#", "#R#", '#', Blocks.COBBLESTONE, 'X', Items.BOW, 'R', Items.REDSTONE);
        this.addRecipe(new ItemStack(Blocks.DROPPER, 1), "###", "# #", "#R#", '#', Blocks.COBBLESTONE, 'R', Items.REDSTONE);
        this.addRecipe(new ItemStack(Blocks.PISTON, 1), "TTT", "#X#", "#R#", '#', Blocks.COBBLESTONE, 'X', Items.IRON_INGOT, 'R', Items.REDSTONE, 'T', Blocks.PLANKS);
        this.addRecipe(new ItemStack(Blocks.STICKY_PISTON, 1), "S", "P", 'S', Items.SLIME_BALL, 'P', Blocks.PISTON);
        this.addRecipe(new ItemStack(Items.BED, 1), "###", "XXX", '#', Blocks.WOOL, 'X', Blocks.PLANKS);
        this.addRecipe(new ItemStack(Blocks.ENCHANTING_TABLE, 1), " B ", "D#D", "###", '#', Blocks.OBSIDIAN, 'B', Items.BOOK, 'D', Items.DIAMOND);
        this.addRecipe(new ItemStack(Blocks.ANVIL, 1), "III", " i ", "iii", 'I', Blocks.IRON_BLOCK, 'i', Items.IRON_INGOT);
        this.addRecipe(new ItemStack(Items.LEATHER), "##", "##", '#', Items.RABBIT_HIDE);
        this.addShapelessRecipe(new ItemStack(Items.ENDER_EYE, 1), Items.ENDER_PEARL, Items.BLAZE_POWDER);
        this.addShapelessRecipe(new ItemStack(Items.FIRE_CHARGE, 3), Items.GUNPOWDER, Items.BLAZE_POWDER, Items.COAL);
        this.addShapelessRecipe(new ItemStack(Items.FIRE_CHARGE, 3), Items.GUNPOWDER, Items.BLAZE_POWDER, new ItemStack(Items.COAL, 1, 1));
        this.addRecipe(new ItemStack(Blocks.DAYLIGHT_DETECTOR), "GGG", "QQQ", "WWW", 'G', Blocks.GLASS, 'Q', Items.QUARTZ, 'W', Blocks.WOODEN_SLAB);
        this.addRecipe(new ItemStack(Blocks.HOPPER), "I I", "ICI", " I ", 'I', Items.IRON_INGOT, 'C', Blocks.CHEST);
        this.addRecipe(new ItemStack(Items.ARMOR_STAND, 1), "///", " / ", "/_/", '/', Items.STICK, '_', new ItemStack(Blocks.STONE_SLAB, 1, BlockStoneSlab.EnumType.STONE.getMetadata()));
        this.recipes.sort((p_compare_1_, p_compare_2_) -> p_compare_1_ instanceof ShapelessRecipes && p_compare_2_ instanceof ShapedRecipes ? 1 : (p_compare_2_ instanceof ShapelessRecipes && p_compare_1_ instanceof ShapedRecipes ? -1 : (Integer.compare(p_compare_2_.getRecipeSize(), p_compare_1_.getRecipeSize()))));
    }

    public ShapedRecipes addRecipe(ItemStack stack, Object... recipeComponents) {
        String s = "";
        int i = 0;
        int j = 0;
        int k = 0;

        if (recipeComponents[i] instanceof String[]) {
            String[] astring = (String[]) recipeComponents[i++];

            for (String s2 : astring) {
                ++k;
                j = s2.length();
                s = s + s2;
            }
        } else {
            while (recipeComponents[i] instanceof String) {
                String s1 = (String) recipeComponents[i++];
                ++k;
                j = s1.length();
                s = s + s1;
            }
        }

        Map<Character, ItemStack> map;

        for (map = new HashMap<>(); i < recipeComponents.length; i += 2) {
            Character character = (Character) recipeComponents[i];
            ItemStack itemstack = null;

            if (recipeComponents[i + 1] instanceof Item item) {
                itemstack = new ItemStack(item);
            } else if (recipeComponents[i + 1] instanceof Block block) {
                itemstack = new ItemStack(block, 1, 32767);
            } else if (recipeComponents[i + 1] instanceof ItemStack itemStack) {
                itemstack = itemStack;
            }

            map.put(character, itemstack);
        }

        ItemStack[] aitemstack = new ItemStack[j * k];

        for (int i1 = 0; i1 < j * k; ++i1) {
            char c0 = s.charAt(i1);

            if (map.containsKey(c0)) {
                aitemstack[i1] = map.get(c0).copy();
            } else {
                aitemstack[i1] = null;
            }
        }

        ShapedRecipes shapedrecipes = new ShapedRecipes(j, k, aitemstack, stack);
        this.recipes.add(shapedrecipes);
        return shapedrecipes;
    }

    public void addShapelessRecipe(ItemStack stack, Object... recipeComponents) {
        List<ItemStack> list = new ArrayList<>();

        for (Object object : recipeComponents) {
            if (object instanceof ItemStack itemStack) {
                list.add(itemStack.copy());
            } else if (object instanceof Item item) {
                list.add(new ItemStack(item));
            } else {
                if (!(object instanceof Block block)) {
                    throw new IllegalArgumentException("Invalid shapeless recipe: unknown type " + object.getClass().getName() + "!");
                }

                list.add(new ItemStack(block));
            }
        }

        this.recipes.add(new ShapelessRecipes(stack, list));
    }

    public void addRecipe(IRecipe recipe) {
        this.recipes.add(recipe);
    }

    public ItemStack findMatchingRecipe(InventoryCrafting p_82787_1_, World worldIn) {
        for (IRecipe irecipe : this.recipes) {
            if (irecipe.matches(p_82787_1_, worldIn)) {
                return irecipe.getCraftingResult(p_82787_1_);
            }
        }

        return null;
    }

    public ItemStack[] func_180303_b(InventoryCrafting p_180303_1_, World worldIn) {
        for (IRecipe irecipe : this.recipes) {
            if (irecipe.matches(p_180303_1_, worldIn)) {
                return irecipe.getRemainingItems(p_180303_1_);
            }
        }

        ItemStack[] aitemstack = new ItemStack[p_180303_1_.getSizeInventory()];

        for (int i = 0; i < aitemstack.length; ++i) {
            aitemstack[i] = p_180303_1_.getStackInSlot(i);
        }

        return aitemstack;
    }

    public List<IRecipe> getRecipeList() {
        return this.recipes;
    }
}
