package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;
import net.minecraft.world.World;

import java.util.List;

public class BlockQuartz extends Block {
    public static final PropertyEnum<QuartzType> VARIANT = PropertyEnum.create("variant", QuartzType.class);

    public BlockQuartz() {
        super(Material.ROCK);
        this.setDefaultState(this.blockState.getBaseState().withProperty(VARIANT, QuartzType.DEFAULT));
        this.setCreativeTab(CreativeTabs.TAB_BLOCK);
    }

    @Override
    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        if (meta == QuartzType.LINES_Y.getMetadata()) {
            return switch (facing.getAxis()) {
                case Z -> this.getDefaultState().withProperty(VARIANT, QuartzType.LINES_Z);
                case X -> this.getDefaultState().withProperty(VARIANT, QuartzType.LINES_X);
                default -> this.getDefaultState().withProperty(VARIANT, QuartzType.LINES_Y);
            };
        } else {
            return meta == QuartzType.CHISELED.getMetadata() ? this.getDefaultState().withProperty(VARIANT, QuartzType.CHISELED) : this.getDefaultState().withProperty(VARIANT, QuartzType.DEFAULT);
        }
    }

    @Override
    public int damageDropped(IBlockState state) {
        QuartzType blockquartz$enumtype = state.getValue(VARIANT);
        return blockquartz$enumtype != QuartzType.LINES_X && blockquartz$enumtype != QuartzType.LINES_Z ? blockquartz$enumtype.getMetadata() : QuartzType.LINES_Y.getMetadata();
    }

    @Override
    protected ItemStack createStackedBlock(IBlockState state) {
        QuartzType blockquartz$enumtype = state.getValue(VARIANT);
        return blockquartz$enumtype != QuartzType.LINES_X && blockquartz$enumtype != QuartzType.LINES_Z ? super.createStackedBlock(state) : new ItemStack(Item.getItemFromBlock(this), 1, QuartzType.LINES_Y.getMetadata());
    }

    @Override
    public void getSubBlocks(Item itemIn, CreativeTabs tab, List<ItemStack> list) {
        list.add(new ItemStack(itemIn, 1, QuartzType.DEFAULT.getMetadata()));
        list.add(new ItemStack(itemIn, 1, QuartzType.CHISELED.getMetadata()));
        list.add(new ItemStack(itemIn, 1, QuartzType.LINES_Y.getMetadata()));
    }

    @Override
    public MapColor getMapColor(IBlockState state) {
        return MapColor.QUARTZ_COLOR;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(VARIANT, QuartzType.byMetadata(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, VARIANT);
    }

    public enum QuartzType implements IStringSerializable {
        DEFAULT(0, "default", "default"),
        CHISELED(1, "chiseled", "chiseled"),
        LINES_Y(2, "lines_y", "lines"),
        LINES_X(3, "lines_x", "lines"),
        LINES_Z(4, "lines_z", "lines");

        private static final QuartzType[] META_LOOKUP = new QuartzType[values().length];
        private final int meta;
        private final String field_176805_h;
        private final String unlocalizedName;

        QuartzType(int meta, String name, String unlocalizedName) {
            this.meta = meta;
            this.field_176805_h = name;
            this.unlocalizedName = unlocalizedName;
        }

        public int getMetadata() {
            return this.meta;
        }

        public String toString() {
            return this.unlocalizedName;
        }

        public static QuartzType byMetadata(int meta) {
            if (meta < 0 || meta >= META_LOOKUP.length) {
                meta = 0;
            }

            return META_LOOKUP[meta];
        }

        @Override
        public String getName() {
            return this.field_176805_h;
        }

        static {
            for (QuartzType blockquartz$enumtype : values()) {
                META_LOOKUP[blockquartz$enumtype.getMetadata()] = blockquartz$enumtype;
            }
        }
    }
}
