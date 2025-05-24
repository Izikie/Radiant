package net.optifine.shaders;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.src.Config;
import net.optifine.Log;
import net.optifine.config.ConnectedParser;
import net.optifine.shaders.config.MacroProcessor;
import net.optifine.util.PropertiesOrdered;
import net.optifine.util.StrUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ItemAliases {
    private static int[] itemAliases = null;
    private static boolean updateOnResourcesReloaded;
    private static final int NO_ALIAS = Integer.MIN_VALUE;

    public static int getItemAliasId(int itemId) {
        if (itemAliases == null) {
            return itemId;
        } else if (itemId >= 0 && itemId < itemAliases.length) {
            int i = itemAliases[itemId];
            return i == Integer.MIN_VALUE ? itemId : i;
        } else {
            return itemId;
        }
    }

    public static void resourcesReloaded() {
        if (updateOnResourcesReloaded) {
            updateOnResourcesReloaded = false;
            update(Shaders.getShaderPack());
        }
    }

    public static void update(IShaderPack shaderPack) {
        reset();

        if (shaderPack != null) {
            IntList list = new IntArrayList();
            String s = "/shaders/item.properties";
            InputStream inputstream = shaderPack.getResourceAsStream(s);

            if (inputstream != null) {
                loadItemAliases(inputstream, s, list);
            }

            if (!list.isEmpty()) {
                itemAliases = toArray(list);
            }
        }
    }

    private static void loadItemAliases(InputStream in, String path, IntList listItemAliases) {
        if (in != null) {
            try {
                in = MacroProcessor.process(in, path);
                Properties properties = new PropertiesOrdered();
                properties.load(in);
                in.close();
                Log.info("[Shaders] Parsing item mappings: " + path);
                ConnectedParser connectedparser = new ConnectedParser("Shaders");

                for (Object o : properties.keySet()) {
                    String s = (String) o;
                    String s1 = properties.getProperty(s);
                    String s2 = "item.";

                    if (!s.startsWith(s2)) {
                        Log.error("[Shaders] Invalid item ID: " + s);
                    } else {
                        String s3 = StrUtils.removePrefix(s, s2);
                        int i = Config.parseInt(s3, -1);

                        if (i < 0) {
                            Log.error("[Shaders] Invalid item alias ID: " + i);
                        } else {
                            int[] aint = connectedparser.parseItems(s1);

                            if (aint != null && aint.length >= 1) {
                                for (int k : aint) {
                                    addToList(listItemAliases, k, i);
                                }
                            } else {
                                Log.error("[Shaders] Invalid item ID mapping: " + s + "=" + s1);
                            }
                        }
                    }
                }
            } catch (IOException var15) {
                Log.error("[Shaders] Error reading: " + path);
            }
        }
    }

    private static void addToList(IntList list, int index, int val) {
        while (list.size() <= index) {
            list.add(Integer.MIN_VALUE);
        }

        list.set(index, val);
    }

    private static int[] toArray(IntList list) {
        int[] aint = new int[list.size()];

        for (int i = 0; i < aint.length; ++i) {
            aint[i] = list.getInt(i);
        }

        return aint;
    }

    public static void reset() {
        itemAliases = null;
    }
}
