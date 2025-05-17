package net.minecraft.block;

import java.util.Random;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class BlockDeadBush extends BlockBush {
    protected BlockDeadBush() {
        super(Material.VINE);
        float f = 0.4F;
        this.setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, 0.8F, 0.5F + f);
    }

    public MapColor getMapColor(IBlockState state) {
        return MapColor.WOOD_COLOR;
    }

    protected boolean canPlaceBlockOn(Block ground) {
        return ground == Blocks.SAND || ground == Blocks.HARDENED_CLAY || ground == Blocks.STAINED_HARDENED_CLAY || ground == Blocks.DIRT;
    }

    public boolean isReplaceable(World worldIn, BlockPos pos) {
        return true;
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return null;
    }

    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te) {
        if (!worldIn.isRemote && player.getCurrentEquippedItem() != null && player.getCurrentEquippedItem().getItem() == Items.SHEARS) {
            player.triggerAchievement(StatList.MINE_BLOCK_STAT_ARRAY[Block.getIdFromBlock(this)]);
            spawnAsEntity(worldIn, pos, new ItemStack(Blocks.DEAD_BUSH, 1, 0));
        } else {
            super.harvestBlock(worldIn, player, pos, state, te);
        }
    }
}
