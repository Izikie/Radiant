package net.optifine;

import net.minecraft.block.state.BlockStateBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.RenderLayer;
import net.optifine.config.ConnectedParser;
import net.optifine.config.MatchBlock;
import net.optifine.shaders.BlockAliases;
import net.optifine.util.collection.PropertiesOrdered;
import net.optifine.util.ResUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CustomBlockLayers {
    public static boolean active = false;
    private static RenderLayer[] renderLayers = null;

    public static RenderLayer getRenderLayer(IBlockState blockState) {
        if (renderLayers == null) {
            return null;
        } else if (blockState.getBlock().isOpaqueCube()) {
            return null;
        } else if (!(blockState instanceof BlockStateBase blockstatebase)) {
            return null;
        } else {
            int i = blockstatebase.getBlockId();
            return i > 0 && i < renderLayers.length ? renderLayers[i] : null;
        }
    }

    public static void update() {
        renderLayers = null;
        active = false;
        List<RenderLayer> list = new ArrayList<>();
        String s = "optifine/block.properties";
        Properties properties = ResUtils.readProperties(s, "CustomBlockLayers");

        if (properties != null) {
            readLayers(s, properties, list);
        }

        if (Config.isShaders()) {
            PropertiesOrdered propertiesordered = BlockAliases.getBlockLayerPropertes();

            if (propertiesordered != null) {
                String s1 = "shaders/block.properties";
                readLayers(s1, propertiesordered, list);
            }
        }

        if (!list.isEmpty()) {
            renderLayers = list.toArray(new RenderLayer[0]);
            active = true;
        }
    }

    private static void readLayers(String pathProps, Properties props, List<RenderLayer> list) {
        Log.info("CustomBlockLayers: " + pathProps);
        readLayer("solid", RenderLayer.SOLID, props, list);
        readLayer("cutout", RenderLayer.CUTOUT, props, list);
        readLayer("cutout_mipped", RenderLayer.CUTOUT_MIPPED, props, list);
        readLayer("translucent", RenderLayer.TRANSLUCENT, props, list);
    }

    private static void readLayer(String name, RenderLayer layer, Properties props, List<RenderLayer> listLayers) {
        String s = "layer." + name;
        String s1 = props.getProperty(s);

        if (s1 != null) {
            ConnectedParser connectedparser = new ConnectedParser("CustomBlockLayers");
            MatchBlock[] amatchblock = connectedparser.parseMatchBlocks(s1);

            if (amatchblock != null) {
                for (MatchBlock matchblock : amatchblock) {
                    int j = matchblock.getBlockId();

                    if (j > 0) {
                        while (listLayers.size() < j + 1) {
                            listLayers.add(null);
                        }

                        if (listLayers.get(j) != null) {
                            Log.error("CustomBlockLayers: Block layer is already set, block: " + j + ", layer: " + name);
                        }

                        listLayers.set(j, layer);
                    }
                }
            }
        }
    }

    public static boolean isActive() {
        return active;
    }
}
