package net.minecraft.stats;

import com.google.common.collect.Lists;

import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonSerializableSet;

public class AchievementList {
    public static int minDisplayColumn;
    public static int minDisplayRow;
    public static int maxDisplayColumn;
    public static int maxDisplayRow;
    public static final List<Achievement> ACHIEVEMENT_LIST = Lists.newArrayList();
    public static final Achievement OPEN_INVENTORY = (new Achievement("achievement.openInventory", "openInventory", 0, 0, Items.book, null)).initIndependentStat().registerStat();
    public static final Achievement MINE_WOOD = (new Achievement("achievement.mineWood", "mineWood", 2, 1, Blocks.log, OPEN_INVENTORY)).registerStat();
    public static final Achievement BUILD_WORK_BENCH = (new Achievement("achievement.buildWorkBench", "buildWorkBench", 4, -1, Blocks.crafting_table, MINE_WOOD)).registerStat();
    public static final Achievement BUILD_PICKAXE = (new Achievement("achievement.buildPickaxe", "buildPickaxe", 4, 2, Items.wooden_pickaxe, BUILD_WORK_BENCH)).registerStat();
    public static final Achievement BUILD_FURNACE = (new Achievement("achievement.buildFurnace", "buildFurnace", 3, 4, Blocks.furnace, BUILD_PICKAXE)).registerStat();
    public static final Achievement ACQUIRE_IRON = (new Achievement("achievement.acquireIron", "acquireIron", 1, 4, Items.iron_ingot, BUILD_FURNACE)).registerStat();
    public static final Achievement BUILD_HOE = (new Achievement("achievement.buildHoe", "buildHoe", 2, -3, Items.wooden_hoe, BUILD_WORK_BENCH)).registerStat();
    public static final Achievement MAKE_BREAD = (new Achievement("achievement.makeBread", "makeBread", -1, -3, Items.bread, BUILD_HOE)).registerStat();
    public static final Achievement BAKE_CAKE = (new Achievement("achievement.bakeCake", "bakeCake", 0, -5, Items.cake, BUILD_HOE)).registerStat();
    public static final Achievement BUILD_BETTER_PICKAXE = (new Achievement("achievement.buildBetterPickaxe", "buildBetterPickaxe", 6, 2, Items.stone_pickaxe, BUILD_PICKAXE)).registerStat();
    public static final Achievement COOK_FISH = (new Achievement("achievement.cookFish", "cookFish", 2, 6, Items.cooked_fish, BUILD_FURNACE)).registerStat();
    public static final Achievement ON_A_RAIL = (new Achievement("achievement.onARail", "onARail", 2, 3, Blocks.rail, ACQUIRE_IRON)).setSpecial().registerStat();
    public static final Achievement BUILD_SWORD = (new Achievement("achievement.buildSword", "buildSword", 6, -1, Items.wooden_sword, BUILD_WORK_BENCH)).registerStat();
    public static final Achievement KILL_ENEMY = (new Achievement("achievement.killEnemy", "killEnemy", 8, -1, Items.bone, BUILD_SWORD)).registerStat();
    public static final Achievement KILL_COW = (new Achievement("achievement.killCow", "killCow", 7, -3, Items.leather, BUILD_SWORD)).registerStat();
    public static final Achievement FLY_PIG = (new Achievement("achievement.flyPig", "flyPig", 9, -3, Items.saddle, KILL_COW)).setSpecial().registerStat();
    public static final Achievement SNIPE_SKELETON = (new Achievement("achievement.snipeSkeleton", "snipeSkeleton", 7, 0, Items.bow, KILL_ENEMY)).setSpecial().registerStat();
    public static final Achievement DIAMONDS = (new Achievement("achievement.diamonds", "diamonds", -1, 5, Blocks.diamond_ore, ACQUIRE_IRON)).registerStat();
    public static final Achievement DIAMONDS_TO_YOU = (new Achievement("achievement.diamondsToYou", "diamondsToYou", -1, 2, Items.diamond, DIAMONDS)).registerStat();
    public static final Achievement PORTAL = (new Achievement("achievement.portal", "portal", -1, 7, Blocks.obsidian, DIAMONDS)).registerStat();
    public static final Achievement GHAST = (new Achievement("achievement.ghast", "ghast", -4, 8, Items.ghast_tear, PORTAL)).setSpecial().registerStat();
    public static final Achievement BLAZE_ROD = (new Achievement("achievement.blazeRod", "blazeRod", 0, 9, Items.blaze_rod, PORTAL)).registerStat();
    public static final Achievement POTION = (new Achievement("achievement.potion", "potion", 2, 8, Items.potionitem, BLAZE_ROD)).registerStat();
    public static final Achievement THE_END = (new Achievement("achievement.theEnd", "theEnd", 3, 10, Items.ender_eye, BLAZE_ROD)).setSpecial().registerStat();
    public static final Achievement THE_END_2 = (new Achievement("achievement.theEnd2", "theEnd2", 4, 13, Blocks.dragon_egg, THE_END)).setSpecial().registerStat();
    public static final Achievement ENCHANTMENTS = (new Achievement("achievement.enchantments", "enchantments", -4, 4, Blocks.enchanting_table, DIAMONDS)).registerStat();
    public static final Achievement OVERKILL = (new Achievement("achievement.overkill", "overkill", -4, 1, Items.diamond_sword, ENCHANTMENTS)).setSpecial().registerStat();
    public static final Achievement BOOKCASE = (new Achievement("achievement.bookcase", "bookcase", -3, 6, Blocks.bookshelf, ENCHANTMENTS)).registerStat();
    public static final Achievement BREED_COW = (new Achievement("achievement.breedCow", "breedCow", 7, -5, Items.wheat, KILL_COW)).registerStat();
    public static final Achievement SPAWN_WITHER = (new Achievement("achievement.spawnWither", "spawnWither", 7, 12, new ItemStack(Items.skull, 1, 1), THE_END_2)).registerStat();
    public static final Achievement KILL_WITHER = (new Achievement("achievement.killWither", "killWither", 7, 10, Items.nether_star, SPAWN_WITHER)).registerStat();
    public static final Achievement FULL_BEACON = (new Achievement("achievement.fullBeacon", "fullBeacon", 7, 8, Blocks.beacon, KILL_WITHER)).setSpecial().registerStat();
    public static final Achievement EXPLORE_ALL_BIOMES = (new Achievement("achievement.exploreAllBiomes", "exploreAllBiomes", 4, 8, Items.diamond_boots, THE_END)).func_150953_b(JsonSerializableSet.class).setSpecial().registerStat();
    public static final Achievement OVERPOWERED = (new Achievement("achievement.overpowered", "overpowered", 6, 4, new ItemStack(Items.golden_apple, 1, 1), BUILD_BETTER_PICKAXE)).setSpecial().registerStat();

    public static void init() {
    }
}
