package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IStringSerializable;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public abstract class BlockStoneSlab extends BlockSlab {
    public static final PropertyBool SEAMLESS = PropertyBool.create("seamless");
    public static final PropertyEnum<EnumType> VARIANT = PropertyEnum.create("variant", EnumType.class);

    public BlockStoneSlab() {
        super(Material.ROCK);
        IBlockState iblockstate = this.blockState.getBaseState();

        if (this.isDouble()) {
            iblockstate = iblockstate.withProperty(SEAMLESS, Boolean.FALSE);
        } else {
            iblockstate = iblockstate.withProperty(HALF, EnumBlockHalf.BOTTOM);
        }

        this.setDefaultState(iblockstate.withProperty(VARIANT, EnumType.STONE));
        this.setCreativeTab(CreativeTabs.TAB_BLOCK);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(Blocks.STONE_SLAB);
    }

    @Override
    public Item getItem(World worldIn, BlockPos pos) {
        return Item.getItemFromBlock(Blocks.STONE_SLAB);
    }

    @Override
    public String getUnlocalizedName(int meta) {
        return super.getUnlocalizedName() + "." + EnumType.byMetadata(meta).getUnlocalizedName();
    }

    @Override
    public IProperty<?> getVariantProperty() {
        return VARIANT;
    }

    @Override
    public Object getVariant(ItemStack stack) {
        return EnumType.byMetadata(stack.getMetadata() & 7);
    }

    @Override
    public void getSubBlocks(Item itemIn, CreativeTabs tab, List<ItemStack> list) {
        if (itemIn != Item.getItemFromBlock(Blocks.DOUBLE_STONE_SLAB)) {
            for (EnumType blockstoneslab$enumtype : EnumType.values()) {
                if (blockstoneslab$enumtype != EnumType.WOOD) {
                    list.add(new ItemStack(itemIn, 1, blockstoneslab$enumtype.getMetadata()));
                }
            }
        }
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        IBlockState iblockstate = this.getDefaultState().withProperty(VARIANT, EnumType.byMetadata(meta & 7));

        if (this.isDouble()) {
            iblockstate = iblockstate.withProperty(SEAMLESS, (meta & 8) != 0);
        } else {
            iblockstate = iblockstate.withProperty(HALF, (meta & 8) == 0 ? EnumBlockHalf.BOTTOM : EnumBlockHalf.TOP);
        }

        return iblockstate;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = 0;
        i = i | state.getValue(VARIANT).getMetadata();

        if (this.isDouble()) {
            if (state.getValue(SEAMLESS)) {
                i |= 8;
            }
        } else if (state.getValue(HALF) == EnumBlockHalf.TOP) {
            i |= 8;
        }

        return i;
    }

    @Override
    protected BlockState createBlockState() {
        return this.isDouble() ? new BlockState(this, SEAMLESS, VARIANT) : new BlockState(this, HALF, VARIANT);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }

    @Override
    public MapColor getMapColor(IBlockState state) {
        return state.getValue(VARIANT).func_181074_c();
    }

    public enum EnumType implements IStringSerializable {
        STONE(0, MapColor.STONE_COLOR, "stone"),
        SAND(1, MapColor.SAND_COLOR, "sandstone", "sand"),
        WOOD(2, MapColor.WOOD_COLOR, "wood_old", "wood"),
        COBBLESTONE(3, MapColor.STONE_COLOR, "cobblestone", "cobble"),
        BRICK(4, MapColor.RED_COLOR, "brick"),
        SMOOTHBRICK(5, MapColor.STONE_COLOR, "stone_brick", "smoothStoneBrick"),
        NETHERBRICK(6, MapColor.NETHERRACK_COLOR, "nether_brick", "netherBrick"),
        QUARTZ(7, MapColor.QUARTZ_COLOR, "quartz");

        private static final EnumType[] META_LOOKUP = new EnumType[values().length];
        private final int meta;
        private final MapColor field_181075_k;
        private final String name;
        private final String unlocalizedName;

        EnumType(int p_i46381_3_, MapColor p_i46381_4_, String p_i46381_5_) {
            this(p_i46381_3_, p_i46381_4_, p_i46381_5_, p_i46381_5_);
        }

        EnumType(int p_i46382_3_, MapColor p_i46382_4_, String p_i46382_5_, String p_i46382_6_) {
            this.meta = p_i46382_3_;
            this.field_181075_k = p_i46382_4_;
            this.name = p_i46382_5_;
            this.unlocalizedName = p_i46382_6_;
        }

        public int getMetadata() {
            return this.meta;
        }

        public MapColor func_181074_c() {
            return this.field_181075_k;
        }

        public String toString() {
            return this.name;
        }

        public static EnumType byMetadata(int meta) {
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
            for (EnumType blockstoneslab$enumtype : values()) {
                META_LOOKUP[blockstoneslab$enumtype.getMetadata()] = blockstoneslab$enumtype;
            }
        }
    }
}
