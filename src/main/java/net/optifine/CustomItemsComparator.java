package net.optifine;

import net.minecraft.src.Config;

import java.util.Comparator;

public class CustomItemsComparator implements Comparator<CustomItemProperties> {
    public int compare(CustomItemProperties o1, CustomItemProperties o2) {
        return o1.weight != o2.weight ? o2.weight - o1.weight : (!Config.equals(o1.basePath, o2.basePath) ? o1.basePath.compareTo(o2.basePath) : o1.name.compareTo(o2.name));
    }
}
