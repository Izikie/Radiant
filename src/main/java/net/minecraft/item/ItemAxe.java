package net.minecraft.item;

import com.google.common.collect.Sets;

import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

public class ItemAxe extends ItemTool {
    private static final Set<Block> EFFECTIVE_ON = Sets.newHashSet(Blocks.PLANKS, Blocks.BOOKSHELF, Blocks.LOG, Blocks.LOG_2, Blocks.CHEST, Blocks.PUMPKIN, Blocks.LIT_PUMPKIN, Blocks.MELON_BLOCK, Blocks.LADDER);

    protected ItemAxe(Item.ToolMaterial material) {
        super(3.0F, material, EFFECTIVE_ON);
    }

    public float getStrVsBlock(ItemStack stack, Block state) {
        return state.getMaterial() != Material.WOOD && state.getMaterial() != Material.PLANTS && state.getMaterial() != Material.VINE ? super.getStrVsBlock(stack, state) : this.efficiencyOnProperMaterial;
    }
}
