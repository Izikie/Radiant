package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class BlockOldLog extends BlockLog {
    public static final PropertyEnum<BlockPlanks.WoodType> VARIANT = PropertyEnum.create("variant", BlockPlanks.WoodType.class, p_apply_1_ -> p_apply_1_.getMetadata() < 4);

    public BlockOldLog() {
        this.setDefaultState(this.blockState.getBaseState().withProperty(VARIANT, BlockPlanks.WoodType.OAK).withProperty(LOG_AXIS, Axis.Y));
    }

    @Override
    public MapColor getMapColor(IBlockState state) {
        BlockPlanks.WoodType blockplanks$enumtype = state.getValue(VARIANT);

        switch (state.getValue(LOG_AXIS)) {
            case Y:
                return blockplanks$enumtype.getMapColor();

            case X:
            case Z:
            case NONE:
            default:
                return switch (blockplanks$enumtype) {
                    case SPRUCE -> BlockPlanks.WoodType.DARK_OAK.getMapColor();
                    case BIRCH -> MapColor.QUARTZ_COLOR;
                    case JUNGLE -> BlockPlanks.WoodType.SPRUCE.getMapColor();
                    default -> BlockPlanks.WoodType.SPRUCE.getMapColor();
                };
        }
    }

    @Override
    public void getSubBlocks(Item itemIn, CreativeTabs tab, List<ItemStack> list) {
        list.add(new ItemStack(itemIn, 1, BlockPlanks.WoodType.OAK.getMetadata()));
        list.add(new ItemStack(itemIn, 1, BlockPlanks.WoodType.SPRUCE.getMetadata()));
        list.add(new ItemStack(itemIn, 1, BlockPlanks.WoodType.BIRCH.getMetadata()));
        list.add(new ItemStack(itemIn, 1, BlockPlanks.WoodType.JUNGLE.getMetadata()));
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        IBlockState iblockstate = this.getDefaultState().withProperty(VARIANT, BlockPlanks.WoodType.byMetadata((meta & 3) % 4));

        iblockstate = switch (meta & 12) {
            case 0 -> iblockstate.withProperty(LOG_AXIS, Axis.Y);
            case 4 -> iblockstate.withProperty(LOG_AXIS, Axis.X);
            case 8 -> iblockstate.withProperty(LOG_AXIS, Axis.Z);
            default -> iblockstate.withProperty(LOG_AXIS, Axis.NONE);
        };

        return iblockstate;
    }


    @Override
    public int getMetaFromState(IBlockState state) {
        int i = 0;
        i = i | state.getValue(VARIANT).getMetadata();

        switch (state.getValue(LOG_AXIS)) {
            case X:
                i |= 4;
                break;

            case Z:
                i |= 8;
                break;

            case NONE:
                i |= 12;
        }

        return i;
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, VARIANT, LOG_AXIS);
    }

    @Override
    protected ItemStack createStackedBlock(IBlockState state) {
        return new ItemStack(Item.getItemFromBlock(this), 1, state.getValue(VARIANT).getMetadata());
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }
}
