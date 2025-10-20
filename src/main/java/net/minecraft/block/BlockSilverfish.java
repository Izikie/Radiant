package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IStringSerializable;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class BlockSilverfish extends Block {
    public static final PropertyEnum<EnumType> VARIANT = PropertyEnum.create("variant", EnumType.class);

    public BlockSilverfish() {
        super(Material.CLAY);
        this.setDefaultState(this.blockState.getBaseState().withProperty(VARIANT, EnumType.STONE));
        this.setHardness(0.0F);
        this.setCreativeTab(CreativeTabs.TAB_DECORATIONS);
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    public static boolean canContainSilverfish(IBlockState blockState) {
        Block block = blockState.getBlock();
        return blockState == Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.StoneType.STONE) || block == Blocks.COBBLESTONE || block == Blocks.STONEBRICK;
    }

    @Override
    protected ItemStack createStackedBlock(IBlockState state) {
        return switch (state.getValue(VARIANT)) {
            case COBBLESTONE -> new ItemStack(Blocks.COBBLESTONE);
            case STONEBRICK -> new ItemStack(Blocks.STONEBRICK);
            case MOSSY_STONEBRICK -> new ItemStack(Blocks.STONEBRICK, 1, BlockStoneBrick.EnumType.MOSSY.getMetadata());
            case CRACKED_STONEBRICK ->
                    new ItemStack(Blocks.STONEBRICK, 1, BlockStoneBrick.EnumType.CRACKED.getMetadata());
            case CHISELED_STONEBRICK ->
                    new ItemStack(Blocks.STONEBRICK, 1, BlockStoneBrick.EnumType.CHISELED.getMetadata());
            default -> new ItemStack(Blocks.STONE);
        };
    }

    @Override
    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
        if (!worldIn.isRemote && worldIn.getGameRules().getBoolean("doTileDrops")) {
            EntitySilverfish entitysilverfish = new EntitySilverfish(worldIn);
            entitysilverfish.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
            worldIn.spawnEntityInWorld(entitysilverfish);
            entitysilverfish.spawnExplosionParticle();
        }
    }

    @Override
    public int getDamageValue(World worldIn, BlockPos pos) {
        IBlockState iblockstate = worldIn.getBlockState(pos);
        return iblockstate.getBlock().getMetaFromState(iblockstate);
    }

    @Override
    public void getSubBlocks(Item itemIn, CreativeTabs tab, List<ItemStack> list) {
        for (EnumType blocksilverfish$enumtype : EnumType.values()) {
            list.add(new ItemStack(itemIn, 1, blocksilverfish$enumtype.getMetadata()));
        }
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(VARIANT, EnumType.byMetadata(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, VARIANT);
    }

    public enum EnumType implements IStringSerializable {
        STONE(0, "stone") {
            @Override
            public IBlockState getModelBlock() {
                return Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.StoneType.STONE);
            }
        },
        COBBLESTONE(1, "cobblestone", "cobble") {
            @Override
            public IBlockState getModelBlock() {
                return Blocks.COBBLESTONE.getDefaultState();
            }
        },
        STONEBRICK(2, "stone_brick", "brick") {
            @Override
            public IBlockState getModelBlock() {
                return Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.DEFAULT);
            }
        },
        MOSSY_STONEBRICK(3, "mossy_brick", "mossybrick") {
            @Override
            public IBlockState getModelBlock() {
                return Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.MOSSY);
            }
        },
        CRACKED_STONEBRICK(4, "cracked_brick", "crackedbrick") {
            @Override
            public IBlockState getModelBlock() {
                return Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CRACKED);
            }
        },
        CHISELED_STONEBRICK(5, "chiseled_brick", "chiseledbrick") {
            @Override
            public IBlockState getModelBlock() {
                return Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CHISELED);
            }
        };

        private static final EnumType[] META_LOOKUP = new EnumType[values().length];
        private final int meta;
        private final String name;
        private final String unlocalizedName;

        EnumType(int meta, String name) {
            this(meta, name, name);
        }

        EnumType(int meta, String name, String unlocalizedName) {
            this.meta = meta;
            this.name = name;
            this.unlocalizedName = unlocalizedName;
        }

        public int getMetadata() {
            return this.meta;
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

        public abstract IBlockState getModelBlock();

        public static EnumType forModelBlock(IBlockState model) {
            for (EnumType blocksilverfish$enumtype : values()) {
                if (model == blocksilverfish$enumtype.getModelBlock()) {
                    return blocksilverfish$enumtype;
                }
            }

            return STONE;
        }

        static {
            for (EnumType blocksilverfish$enumtype : values()) {
                META_LOOKUP[blocksilverfish$enumtype.getMetadata()] = blocksilverfish$enumtype;
            }
        }
    }
}
