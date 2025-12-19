package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.StatCollector;

import java.util.List;

public class BlockPrismarine extends Block {
    public static final PropertyEnum<PrismarineType> VARIANT = PropertyEnum.create("variant", PrismarineType.class);
    public static final int ROUGH_META = PrismarineType.ROUGH.getMetadata();
    public static final int BRICKS_META = PrismarineType.BRICKS.getMetadata();
    public static final int DARK_META = PrismarineType.DARK.getMetadata();

    public BlockPrismarine() {
        super(Material.ROCK);
        this.setDefaultState(this.blockState.getBaseState().withProperty(VARIANT, PrismarineType.ROUGH));
        this.setCreativeTab(CreativeTabs.TAB_BLOCK);
    }

    @Override
    public String getLocalizedName() {
        return StatCollector.translateToLocal(this.getUnlocalizedName() + "." + PrismarineType.ROUGH.getUnlocalizedName() + ".name");
    }

    @Override
    public MapColor getMapColor(IBlockState state) {
        return state.getValue(VARIANT) == PrismarineType.ROUGH ? MapColor.CYAN_COLOR : MapColor.DIAMOND_COLOR;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, VARIANT);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(VARIANT, PrismarineType.byMetadata(meta));
    }

    @Override
    public void getSubBlocks(Item itemIn, CreativeTabs tab, List<ItemStack> list) {
        list.add(new ItemStack(itemIn, 1, ROUGH_META));
        list.add(new ItemStack(itemIn, 1, BRICKS_META));
        list.add(new ItemStack(itemIn, 1, DARK_META));
    }

    public enum PrismarineType implements IStringSerializable {
        ROUGH(0, "prismarine", "rough"),
        BRICKS(1, "prismarine_bricks", "bricks"),
        DARK(2, "dark_prismarine", "dark");

        private static final PrismarineType[] META_LOOKUP = new PrismarineType[values().length];
        private final int meta;
        private final String name;
        private final String unlocalizedName;

        PrismarineType(int meta, String name, String unlocalizedName) {
            this.meta = meta;
            this.name = name;
            this.unlocalizedName = unlocalizedName;
        }

        public int getMetadata() {
            return this.meta;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public static PrismarineType byMetadata(int meta) {
            if (meta < 0 || meta >= META_LOOKUP.length) {
                meta = 0;
            }

            return META_LOOKUP[meta];
        }

        @Override
        public String getName() {
            return this.name;
        }

        public String getUnlocalizedName() {
            return this.unlocalizedName;
        }

        static {
            for (PrismarineType blockprismarine$enumtype : values()) {
                META_LOOKUP[blockprismarine$enumtype.getMetadata()] = blockprismarine$enumtype;
            }
        }
    }
}
