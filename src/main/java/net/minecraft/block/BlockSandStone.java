package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;

import java.util.List;

public class BlockSandStone extends Block {
    public static final PropertyEnum<SandStoneType> TYPE = PropertyEnum.create("type", SandStoneType.class);

    public BlockSandStone() {
        super(Material.ROCK);
        this.setDefaultState(this.blockState.getBaseState().withProperty(TYPE, SandStoneType.DEFAULT));
        this.setCreativeTab(CreativeTabs.TAB_BLOCK);
    }

    public int damageDropped(IBlockState state) {
        return state.getValue(TYPE).getMetadata();
    }

    public void getSubBlocks(Item itemIn, CreativeTabs tab, List<ItemStack> list) {
        for (SandStoneType blocksandstone$enumtype : SandStoneType.values()) {
            list.add(new ItemStack(itemIn, 1, blocksandstone$enumtype.getMetadata()));
        }
    }

    public MapColor getMapColor(IBlockState state) {
        return MapColor.SAND_COLOR;
    }

    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(TYPE, SandStoneType.byMetadata(meta));
    }

    public int getMetaFromState(IBlockState state) {
        return state.getValue(TYPE).getMetadata();
    }

    protected BlockState createBlockState() {
        return new BlockState(this, TYPE);
    }

    public enum SandStoneType implements IStringSerializable {
        DEFAULT(0, "sandstone", "default"),
        CHISELED(1, "chiseled_sandstone", "chiseled"),
        SMOOTH(2, "smooth_sandstone", "smooth");

        private static final SandStoneType[] META_LOOKUP = new SandStoneType[values().length];
        private final int metadata;
        private final String name;
        private final String unlocalizedName;

        SandStoneType(int meta, String name, String unlocalizedName) {
            this.metadata = meta;
            this.name = name;
            this.unlocalizedName = unlocalizedName;
        }

        public int getMetadata() {
            return this.metadata;
        }

        public String toString() {
            return this.name;
        }

        public static SandStoneType byMetadata(int meta) {
            if (meta < 0 || meta >= META_LOOKUP.length) {
                meta = 0;
            }

            return META_LOOKUP[meta];
        }

        public String getName() {
            return this.name;
        }

        public String getUnlocalizedName() {
            return this.unlocalizedName;
        }

        static {
            for (SandStoneType blocksandstone$enumtype : values()) {
                META_LOOKUP[blocksandstone$enumtype.getMetadata()] = blocksandstone$enumtype;
            }
        }
    }
}
