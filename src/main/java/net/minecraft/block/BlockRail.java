package net.minecraft.block;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class BlockRail extends BlockRailBase {
    public static final PropertyEnum<RailShape> SHAPE = PropertyEnum.create("shape", RailShape.class);

    protected BlockRail() {
        super(false);
        this.setDefaultState(this.blockState.getBaseState().withProperty(SHAPE, RailShape.NORTH_SOUTH));
    }

    @Override
    protected void onNeighborChangedInternal(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
        if (neighborBlock.canProvidePower() && (new Rail(worldIn, pos, state)).countAdjacentRails() == 3) {
            this.func_176564_a(worldIn, pos, state, false);
        }
    }

    @Override
    public IProperty<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(SHAPE, RailShape.byMetadata(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(SHAPE).getMetadata();
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, SHAPE);
    }
}
