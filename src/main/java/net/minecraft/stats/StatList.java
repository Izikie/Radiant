package net.minecraft.stats;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;

public class StatList {
    protected static final Map<String, StatBase> ONE_SHOT_STATS = new HashMap<>();
    public static final List<StatBase> ALL_STATS = Lists.newArrayList();
    public static final List<StatBase> GENERAL_STATS = Lists.newArrayList();
    public static final List<StatCrafting> ITEM_STATS = Lists.newArrayList();
    public static final List<StatCrafting> OBJECT_MINE_STATS = Lists.newArrayList();
    public static final StatBase LEAVE_GAME_STAT = (new StatBasic("stat.leaveGame", new ChatComponentTranslation("stat.leaveGame"))).initIndependentStat().registerStat();
    public static final StatBase MINUTES_PLAYED_STAT = (new StatBasic("stat.playOneMinute", new ChatComponentTranslation("stat.playOneMinute"), StatBase.TIME_STAT_TYPE)).initIndependentStat().registerStat();
    public static final StatBase TIME_SINCE_DEATH_STAT = (new StatBasic("stat.timeSinceDeath", new ChatComponentTranslation("stat.timeSinceDeath"), StatBase.TIME_STAT_TYPE)).initIndependentStat().registerStat();
    public static final StatBase DISTANCE_WALKED_STAT = (new StatBasic("stat.walkOneCm", new ChatComponentTranslation("stat.walkOneCm"), StatBase.DISTANCE_STAT_TYPE)).initIndependentStat().registerStat();
    public static final StatBase DISTANCE_CROUCHED_STAT = (new StatBasic("stat.crouchOneCm", new ChatComponentTranslation("stat.crouchOneCm"), StatBase.DISTANCE_STAT_TYPE)).initIndependentStat().registerStat();
    public static final StatBase DISTANCE_SPRINTED_STAT = (new StatBasic("stat.sprintOneCm", new ChatComponentTranslation("stat.sprintOneCm"), StatBase.DISTANCE_STAT_TYPE)).initIndependentStat().registerStat();
    public static final StatBase DISTANCE_SWUM_STAT = (new StatBasic("stat.swimOneCm", new ChatComponentTranslation("stat.swimOneCm"), StatBase.DISTANCE_STAT_TYPE)).initIndependentStat().registerStat();
    public static final StatBase DISTANCE_FALLEN_STAT = (new StatBasic("stat.fallOneCm", new ChatComponentTranslation("stat.fallOneCm"), StatBase.DISTANCE_STAT_TYPE)).initIndependentStat().registerStat();
    public static final StatBase DISTANCE_CLIMBED_STAT = (new StatBasic("stat.climbOneCm", new ChatComponentTranslation("stat.climbOneCm"), StatBase.DISTANCE_STAT_TYPE)).initIndependentStat().registerStat();
    public static final StatBase DISTANCE_FLOWN_STAT = (new StatBasic("stat.flyOneCm", new ChatComponentTranslation("stat.flyOneCm"), StatBase.DISTANCE_STAT_TYPE)).initIndependentStat().registerStat();
    public static final StatBase DISTANCE_DOVE_STAT = (new StatBasic("stat.diveOneCm", new ChatComponentTranslation("stat.diveOneCm"), StatBase.DISTANCE_STAT_TYPE)).initIndependentStat().registerStat();
    public static final StatBase DISTANCE_BY_MINECART_STAT = (new StatBasic("stat.minecartOneCm", new ChatComponentTranslation("stat.minecartOneCm"), StatBase.DISTANCE_STAT_TYPE)).initIndependentStat().registerStat();
    public static final StatBase DISTANCE_BY_BOAT_STAT = (new StatBasic("stat.boatOneCm", new ChatComponentTranslation("stat.boatOneCm"), StatBase.DISTANCE_STAT_TYPE)).initIndependentStat().registerStat();
    public static final StatBase DISTANCE_BY_PIG_STAT = (new StatBasic("stat.pigOneCm", new ChatComponentTranslation("stat.pigOneCm"), StatBase.DISTANCE_STAT_TYPE)).initIndependentStat().registerStat();
    public static final StatBase DISTANCE_BY_HORSE_STAT = (new StatBasic("stat.horseOneCm", new ChatComponentTranslation("stat.horseOneCm"), StatBase.DISTANCE_STAT_TYPE)).initIndependentStat().registerStat();
    public static final StatBase JUMP_STAT = (new StatBasic("stat.jump", new ChatComponentTranslation("stat.jump"))).initIndependentStat().registerStat();
    public static final StatBase DROP_STAT = (new StatBasic("stat.drop", new ChatComponentTranslation("stat.drop"))).initIndependentStat().registerStat();
    public static final StatBase DAMAGE_DEALT_STAT = (new StatBasic("stat.damageDealt", new ChatComponentTranslation("stat.damageDealt"), StatBase.field_111202_k)).registerStat();
    public static final StatBase DAMAGE_TAKEN_STAT = (new StatBasic("stat.damageTaken", new ChatComponentTranslation("stat.damageTaken"), StatBase.field_111202_k)).registerStat();
    public static final StatBase DEATHS_STAT = (new StatBasic("stat.deaths", new ChatComponentTranslation("stat.deaths"))).registerStat();
    public static final StatBase MOB_KILLS_STAT = (new StatBasic("stat.mobKills", new ChatComponentTranslation("stat.mobKills"))).registerStat();
    public static final StatBase ANIMALS_BRED_STAT = (new StatBasic("stat.animalsBred", new ChatComponentTranslation("stat.animalsBred"))).registerStat();
    public static final StatBase PLAYER_KILLS_STAT = (new StatBasic("stat.playerKills", new ChatComponentTranslation("stat.playerKills"))).registerStat();
    public static final StatBase FISH_CAUGHT_STAT = (new StatBasic("stat.fishCaught", new ChatComponentTranslation("stat.fishCaught"))).registerStat();
    public static final StatBase JUNK_FISHED_STAT = (new StatBasic("stat.junkFished", new ChatComponentTranslation("stat.junkFished"))).registerStat();
    public static final StatBase TREASURE_FISHED_STAT = (new StatBasic("stat.treasureFished", new ChatComponentTranslation("stat.treasureFished"))).registerStat();
    public static final StatBase TIMES_TALKED_TO_VILLAGER_STAT = (new StatBasic("stat.talkedToVillager", new ChatComponentTranslation("stat.talkedToVillager"))).registerStat();
    public static final StatBase TIMES_TRADED_WITH_VILLAGER_STAT = (new StatBasic("stat.tradedWithVillager", new ChatComponentTranslation("stat.tradedWithVillager"))).registerStat();
    public static final StatBase field_181724_H = (new StatBasic("stat.cakeSlicesEaten", new ChatComponentTranslation("stat.cakeSlicesEaten"))).registerStat();
    public static final StatBase field_181725_I = (new StatBasic("stat.cauldronFilled", new ChatComponentTranslation("stat.cauldronFilled"))).registerStat();
    public static final StatBase field_181726_J = (new StatBasic("stat.cauldronUsed", new ChatComponentTranslation("stat.cauldronUsed"))).registerStat();
    public static final StatBase field_181727_K = (new StatBasic("stat.armorCleaned", new ChatComponentTranslation("stat.armorCleaned"))).registerStat();
    public static final StatBase field_181728_L = (new StatBasic("stat.bannerCleaned", new ChatComponentTranslation("stat.bannerCleaned"))).registerStat();
    public static final StatBase field_181729_M = (new StatBasic("stat.brewingstandInteraction", new ChatComponentTranslation("stat.brewingstandInteraction"))).registerStat();
    public static final StatBase field_181730_N = (new StatBasic("stat.beaconInteraction", new ChatComponentTranslation("stat.beaconInteraction"))).registerStat();
    public static final StatBase field_181731_O = (new StatBasic("stat.dropperInspected", new ChatComponentTranslation("stat.dropperInspected"))).registerStat();
    public static final StatBase field_181732_P = (new StatBasic("stat.hopperInspected", new ChatComponentTranslation("stat.hopperInspected"))).registerStat();
    public static final StatBase field_181733_Q = (new StatBasic("stat.dispenserInspected", new ChatComponentTranslation("stat.dispenserInspected"))).registerStat();
    public static final StatBase field_181734_R = (new StatBasic("stat.noteblockPlayed", new ChatComponentTranslation("stat.noteblockPlayed"))).registerStat();
    public static final StatBase field_181735_S = (new StatBasic("stat.noteblockTuned", new ChatComponentTranslation("stat.noteblockTuned"))).registerStat();
    public static final StatBase field_181736_T = (new StatBasic("stat.flowerPotted", new ChatComponentTranslation("stat.flowerPotted"))).registerStat();
    public static final StatBase field_181737_U = (new StatBasic("stat.trappedChestTriggered", new ChatComponentTranslation("stat.trappedChestTriggered"))).registerStat();
    public static final StatBase field_181738_V = (new StatBasic("stat.enderchestOpened", new ChatComponentTranslation("stat.enderchestOpened"))).registerStat();
    public static final StatBase field_181739_W = (new StatBasic("stat.itemEnchanted", new ChatComponentTranslation("stat.itemEnchanted"))).registerStat();
    public static final StatBase field_181740_X = (new StatBasic("stat.recordPlayed", new ChatComponentTranslation("stat.recordPlayed"))).registerStat();
    public static final StatBase field_181741_Y = (new StatBasic("stat.furnaceInteraction", new ChatComponentTranslation("stat.furnaceInteraction"))).registerStat();
    public static final StatBase field_181742_Z = (new StatBasic("stat.craftingTableInteraction", new ChatComponentTranslation("stat.workbenchInteraction"))).registerStat();
    public static final StatBase field_181723_aa = (new StatBasic("stat.chestOpened", new ChatComponentTranslation("stat.chestOpened"))).registerStat();
    public static final StatBase[] MINE_BLOCK_STAT_ARRAY = new StatBase[4096];
    public static final StatBase[] OBJECT_CRAFT_STATS = new StatBase[32000];
    public static final StatBase[] OBJECT_USE_STATS = new StatBase[32000];
    public static final StatBase[] OBJECT_BREAK_STATS = new StatBase[32000];

    public static void init() {
        initMiningStats();
        initStats();
        initItemDepleteStats();
        initCraftableStats();
        AchievementList.init();
        EntityList.func_151514_a();
    }

    private static void initCraftableStats() {
        Set<Item> set = Sets.newHashSet();

        for (IRecipe irecipe : CraftingManager.getInstance().getRecipeList()) {
            if (irecipe.getRecipeOutput() != null) {
                set.add(irecipe.getRecipeOutput().getItem());
            }
        }

        for (ItemStack itemstack : FurnaceRecipes.instance().getSmeltingList().values()) {
            set.add(itemstack.getItem());
        }

        for (Item item : set) {
            if (item != null) {
                int i = Item.getIdFromItem(item);
                String s = func_180204_a(item);

                if (s != null) {
                    OBJECT_CRAFT_STATS[i] = (new StatCrafting("stat.craftItem.", s, new ChatComponentTranslation("stat.craftItem", (new ItemStack(item)).getChatComponent()), item)).registerStat();
                }
            }
        }

        replaceAllSimilarBlocks(OBJECT_CRAFT_STATS);
    }

    private static void initMiningStats() {
        for (Block block : Block.blockRegistry) {
            Item item = Item.getItemFromBlock(block);

            if (item != null) {
                int i = Block.getIdFromBlock(block);
                String s = func_180204_a(item);

                if (s != null && block.getEnableStats()) {
                    MINE_BLOCK_STAT_ARRAY[i] = (new StatCrafting("stat.mineBlock.", s, new ChatComponentTranslation("stat.mineBlock", (new ItemStack(block)).getChatComponent()), item)).registerStat();
                    OBJECT_MINE_STATS.add((StatCrafting) MINE_BLOCK_STAT_ARRAY[i]);
                }
            }
        }

        replaceAllSimilarBlocks(MINE_BLOCK_STAT_ARRAY);
    }

    private static void initStats() {
        for (Item item : Item.itemRegistry) {
            if (item != null) {
                int i = Item.getIdFromItem(item);
                String s = func_180204_a(item);

                if (s != null) {
                    OBJECT_USE_STATS[i] = (new StatCrafting("stat.useItem.", s, new ChatComponentTranslation("stat.useItem", (new ItemStack(item)).getChatComponent()), item)).registerStat();

                    if (!(item instanceof ItemBlock)) {
                        ITEM_STATS.add((StatCrafting) OBJECT_USE_STATS[i]);
                    }
                }
            }
        }

        replaceAllSimilarBlocks(OBJECT_USE_STATS);
    }

    private static void initItemDepleteStats() {
        for (Item item : Item.itemRegistry) {
            if (item != null) {
                int i = Item.getIdFromItem(item);
                String s = func_180204_a(item);

                if (s != null && item.isDamageable()) {
                    OBJECT_BREAK_STATS[i] = (new StatCrafting("stat.breakItem.", s, new ChatComponentTranslation("stat.breakItem", (new ItemStack(item)).getChatComponent()), item)).registerStat();
                }
            }
        }

        replaceAllSimilarBlocks(OBJECT_BREAK_STATS);
    }

    private static String func_180204_a(Item p_180204_0_) {
        ResourceLocation resourcelocation = Item.itemRegistry.getNameForObject(p_180204_0_);
        return resourcelocation != null ? resourcelocation.toString().replace(':', '.') : null;
    }

    private static void replaceAllSimilarBlocks(StatBase[] p_75924_0_) {
        mergeStatBases(p_75924_0_, Blocks.WATER, Blocks.FLOWING_WATER);
        mergeStatBases(p_75924_0_, Blocks.LAVA, Blocks.FLOWING_LAVA);
        mergeStatBases(p_75924_0_, Blocks.LIT_PUMPKIN, Blocks.PUMPKIN);
        mergeStatBases(p_75924_0_, Blocks.LIT_FURNACE, Blocks.FURNACE);
        mergeStatBases(p_75924_0_, Blocks.LIT_REDSTONE_ORE, Blocks.REDSTONE_ORE);
        mergeStatBases(p_75924_0_, Blocks.POWERED_REPEATER, Blocks.UNPOWERED_REPEATER);
        mergeStatBases(p_75924_0_, Blocks.POWERED_COMPARATOR, Blocks.UNPOWERED_COMPARATOR);
        mergeStatBases(p_75924_0_, Blocks.REDSTONE_TORCH, Blocks.UNLIT_REDSTONE_TORCH);
        mergeStatBases(p_75924_0_, Blocks.LIT_REDSTONE_LAMP, Blocks.REDSTONE_LAMP);
        mergeStatBases(p_75924_0_, Blocks.DOUBLE_STONE_SLAB, Blocks.STONE_SLAB);
        mergeStatBases(p_75924_0_, Blocks.DOUBLE_WOODEN_SLAB, Blocks.WOODEN_SLAB);
        mergeStatBases(p_75924_0_, Blocks.DOUBLE_STONE_SLAB_2, Blocks.BLOCK_SLAB);
        mergeStatBases(p_75924_0_, Blocks.GRASS, Blocks.DIRT);
        mergeStatBases(p_75924_0_, Blocks.FARMLAND, Blocks.DIRT);
    }

    private static void mergeStatBases(StatBase[] statBaseIn, Block p_151180_1_, Block p_151180_2_) {
        int i = Block.getIdFromBlock(p_151180_1_);
        int j = Block.getIdFromBlock(p_151180_2_);

        if (statBaseIn[i] != null && statBaseIn[j] == null) {
            statBaseIn[j] = statBaseIn[i];
        } else {
            ALL_STATS.remove(statBaseIn[i]);
            OBJECT_MINE_STATS.remove(statBaseIn[i]);
            GENERAL_STATS.remove(statBaseIn[i]);
            statBaseIn[i] = statBaseIn[j];
        }
    }

    public static StatBase getStatKillEntity(EntityList.EntityEggInfo eggInfo) {
        String s = EntityList.getStringFromID(eggInfo.spawnedID);
        return s == null ? null : (new StatBase("stat.killEntity." + s, new ChatComponentTranslation("stat.entityKill", new ChatComponentTranslation("entity." + s + ".name")))).registerStat();
    }

    public static StatBase getStatEntityKilledBy(EntityList.EntityEggInfo eggInfo) {
        String s = EntityList.getStringFromID(eggInfo.spawnedID);
        return s == null ? null : (new StatBase("stat.entityKilledBy." + s, new ChatComponentTranslation("stat.entityKilledBy", new ChatComponentTranslation("entity." + s + ".name")))).registerStat();
    }

    public static StatBase getOneShotStat(String p_151177_0_) {
        return ONE_SHOT_STATS.get(p_151177_0_);
    }
}
