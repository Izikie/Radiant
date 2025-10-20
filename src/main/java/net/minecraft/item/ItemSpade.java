package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.Set;

public class ItemSpade extends ItemTool {
    private static final Set<Block> EFFECTIVE_ON = Set.of(Blocks.CLAY, Blocks.DIRT, Blocks.FARMLAND, Blocks.GRASS, Blocks.GRAVEL, Blocks.MYCELIUM, Blocks.SAND, Blocks.SNOW, Blocks.SNOW_LAYER, Blocks.SOUL_SAND);

    public ItemSpade(ToolMaterial material) {
        super(1.0F, material, EFFECTIVE_ON);
    }

    @Override
    public boolean canHarvestBlock(Block blockIn) {
        return blockIn == Blocks.SNOW_LAYER || blockIn == Blocks.SNOW;
    }
}
