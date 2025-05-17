package net.minecraft.block;

import java.util.List;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class BlockColored extends Block {
    public static final PropertyEnum<DyeColor> COLOR = PropertyEnum.create("color", DyeColor.class);

    public BlockColored(Material materialIn) {
        super(materialIn);
        this.setDefaultState(this.blockState.getBaseState().withProperty(COLOR, DyeColor.WHITE));
        this.setCreativeTab(CreativeTabs.TAB_BLOCK);
    }

    public int damageDropped(IBlockState state) {
        return state.getValue(COLOR).getMetadata();
    }

    public void getSubBlocks(Item itemIn, CreativeTabs tab, List<ItemStack> list) {
        for (DyeColor enumdyecolor : DyeColor.values()) {
            list.add(new ItemStack(itemIn, 1, enumdyecolor.getMetadata()));
        }
    }

    public MapColor getMapColor(IBlockState state) {
        return state.getValue(COLOR).getMapColor();
    }

    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(COLOR, DyeColor.byMetadata(meta));
    }

    public int getMetaFromState(IBlockState state) {
        return state.getValue(COLOR).getMetadata();
    }

    protected BlockState createBlockState() {
        return new BlockState(this, COLOR);
    }
}
