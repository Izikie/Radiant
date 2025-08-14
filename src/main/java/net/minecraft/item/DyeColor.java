package net.minecraft.item;

import net.minecraft.block.material.MapColor;
import net.minecraft.util.chat.Formatting;
import net.minecraft.util.IStringSerializable;

public enum DyeColor implements IStringSerializable {
    WHITE(0, 15, "white", "white", MapColor.SNOW_COLOR, Formatting.WHITE),
    ORANGE(1, 14, "orange", "orange", MapColor.ADOBE_COLOR, Formatting.GOLD),
    MAGENTA(2, 13, "magenta", "magenta", MapColor.MAGENTA_COLOR, Formatting.AQUA),
    LIGHT_BLUE(3, 12, "light_blue", "lightBlue", MapColor.LIGHT_BLUE_COLOR, Formatting.BLUE),
    YELLOW(4, 11, "yellow", "yellow", MapColor.YELLOW_COLOR, Formatting.YELLOW),
    LIME(5, 10, "lime", "lime", MapColor.LIME_COLOR, Formatting.GREEN),
    PINK(6, 9, "pink", "pink", MapColor.PINK_COLOR, Formatting.LIGHT_PURPLE),
    GRAY(7, 8, "gray", "gray", MapColor.GRAY_COLOR, Formatting.DARK_GRAY),
    SILVER(8, 7, "silver", "silver", MapColor.SILVER_COLOR, Formatting.GRAY),
    CYAN(9, 6, "cyan", "cyan", MapColor.CYAN_COLOR, Formatting.DARK_AQUA),
    PURPLE(10, 5, "purple", "purple", MapColor.PURPLE_COLOR, Formatting.DARK_PURPLE),
    BLUE(11, 4, "blue", "blue", MapColor.BLUE_COLOR, Formatting.DARK_BLUE),
    BROWN(12, 3, "brown", "brown", MapColor.BROWN_COLOR, Formatting.GOLD),
    GREEN(13, 2, "green", "green", MapColor.GREEN_COLOR, Formatting.DARK_GREEN),
    RED(14, 1, "red", "red", MapColor.RED_COLOR, Formatting.DARK_RED),
    BLACK(15, 0, "black", "black", MapColor.BLACK_COLOR, Formatting.BLACK);

    private static final DyeColor[] META_LOOKUP = new DyeColor[values().length];
    private static final DyeColor[] DYE_DMG_LOOKUP = new DyeColor[values().length];
    private final int meta;
    private final int dyeDamage;
    private final String name;
    private final String unlocalizedName;
    private final MapColor mapColor;
    private final Formatting chatColor;

    DyeColor(int meta, int dyeDamage, String name, String unlocalizedName, MapColor mapColorIn, Formatting chatColor) {
        this.meta = meta;
        this.dyeDamage = dyeDamage;
        this.name = name;
        this.unlocalizedName = unlocalizedName;
        this.mapColor = mapColorIn;
        this.chatColor = chatColor;
    }

    public int getMetadata() {
        return this.meta;
    }

    public int getDyeDamage() {
        return this.dyeDamage;
    }

    public String getUnlocalizedName() {
        return this.unlocalizedName;
    }

    public MapColor getMapColor() {
        return this.mapColor;
    }

    public static DyeColor byDyeDamage(int damage) {
        if (damage < 0 || damage >= DYE_DMG_LOOKUP.length) {
            damage = 0;
        }

        return DYE_DMG_LOOKUP[damage];
    }

    public static DyeColor byMetadata(int meta) {
        if (meta < 0 || meta >= META_LOOKUP.length) {
            meta = 0;
        }

        return META_LOOKUP[meta];
    }

    public String toString() {
        return this.unlocalizedName;
    }

    public String getName() {
        return this.name;
    }

    static {
        for (DyeColor enumdyecolor : values()) {
            META_LOOKUP[enumdyecolor.getMetadata()] = enumdyecolor;
            DYE_DMG_LOOKUP[enumdyecolor.getDyeDamage()] = enumdyecolor;
        }
    }
}
