package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

public class BlockHay extends BlockRotatedPillar {
    public BlockHay() {
        super(Material.GRASS, MapColor.YELLOW_COLOR);
        this.setDefaultState(this.blockState.getBaseState().withProperty(AXIS, Direction.Axis.Y));
        this.setCreativeTab(CreativeTabs.TAB_BLOCK);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        Direction.Axis enumfacing$axis = Direction.Axis.Y;
        int i = meta & 12;

        if (i == 4) {
            enumfacing$axis = Direction.Axis.X;
        } else if (i == 8) {
            enumfacing$axis = Direction.Axis.Z;
        }

        return this.getDefaultState().withProperty(AXIS, enumfacing$axis);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = 0;
        Direction.Axis enumfacing$axis = state.getValue(AXIS);

        if (enumfacing$axis == Direction.Axis.X) {
            i |= 4;
        } else if (enumfacing$axis == Direction.Axis.Z) {
            i |= 8;
        }

        return i;
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, AXIS);
    }

    @Override
    protected ItemStack createStackedBlock(IBlockState state) {
        return new ItemStack(Item.getItemFromBlock(this), 1, 0);
    }

    @Override
    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return super.onBlockPlaced(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer).withProperty(AXIS, facing.getAxis());
    }
}
