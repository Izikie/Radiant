package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.RenderLayer;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class BlockStainedGlass extends BlockBreakable {
    public static final PropertyEnum<DyeColor> COLOR = PropertyEnum.create("color", DyeColor.class);

    public BlockStainedGlass(Material materialIn) {
        super(materialIn, false);
        this.setDefaultState(this.blockState.getBaseState().withProperty(COLOR, DyeColor.WHITE));
        this.setCreativeTab(CreativeTabs.TAB_BLOCK);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(COLOR).getMetadata();
    }

    @Override
    public void getSubBlocks(Item itemIn, CreativeTabs tab, List<ItemStack> list) {
        for (DyeColor enumdyecolor : DyeColor.values()) {
            list.add(new ItemStack(itemIn, 1, enumdyecolor.getMetadata()));
        }
    }

    @Override
    public MapColor getMapColor(IBlockState state) {
        return state.getValue(COLOR).getMapColor();
    }

    @Override
    public RenderLayer getBlockLayer() {
        return RenderLayer.TRANSLUCENT;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    protected boolean canSilkHarvest() {
        return true;
    }

    @Override
    public boolean isFullCube() {
        return false;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(COLOR, DyeColor.byMetadata(meta));
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            BlockBeacon.updateColorAsync(worldIn, pos);
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            BlockBeacon.updateColorAsync(worldIn, pos);
        }
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(COLOR).getMetadata();
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, COLOR);
    }
}
