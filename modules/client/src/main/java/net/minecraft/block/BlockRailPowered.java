package net.minecraft.block;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

// TODO: Possibly implement FX's Rail Optimization batched updates
public class BlockRailPowered extends BlockRailBase {
    public static final PropertyEnum<RailShape> SHAPE = PropertyEnum.create("shape", RailShape.class,
            shape -> shape != RailShape.NORTH_EAST && shape != RailShape.NORTH_WEST
                    && shape != RailShape.SOUTH_EAST && shape != RailShape.SOUTH_WEST);
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    private static final int RAIL_POWER_LIMIT = 8;

    protected BlockRailPowered() {
        super(true);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(SHAPE, RailShape.NORTH_SOUTH)
                .withProperty(POWERED, Boolean.FALSE));
    }

    protected boolean findSignal(World world, BlockPos pos, IBlockState state, boolean forward, int distance) {
        if (distance >= RAIL_POWER_LIMIT) return false;

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        boolean changed = true;

        RailShape shape = state.getValue(SHAPE);
        switch (shape) {
            case NORTH_SOUTH -> {
                if (forward) {
                    ++z;
                } else {
                    --z;
                }
            }
            case EAST_WEST -> {
                if (forward) {
                    --x;
                } else {
                    ++x;
                }
            }
            case ASCENDING_EAST -> {
                if (forward) {
                    --x;
                } else {
                    ++x;
                    ++y;
                    changed = false;
                }

                shape = RailShape.EAST_WEST;
            }
            case ASCENDING_WEST -> {
                if (forward) {
                    --x;
                    ++y;
                    changed = false;
                } else {
                    ++x;
                }

                shape = RailShape.EAST_WEST;
            }
            case ASCENDING_NORTH -> {
                if (forward) {
                    ++z;
                } else {
                    --z;
                    ++y;
                    changed = false;
                }

                shape = RailShape.NORTH_SOUTH;
            }
            case ASCENDING_SOUTH -> {
                if (forward) {
                    ++z;
                    ++y;
                    changed = false;
                } else {
                    --z;
                }

                shape = RailShape.NORTH_SOUTH;
            }
        }

        BlockPos nextPos = new BlockPos(x, y, z);
        return this.findSignal(world, nextPos, forward, distance, shape)
                || changed && this.findSignal(world, nextPos.down(), forward, distance, shape);
    }

    protected boolean findSignal(World world, BlockPos pos, boolean forward, int distance, RailShape shape) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != this) return false;

        RailShape railShape = state.getValue(SHAPE);
        if ((shape == RailShape.EAST_WEST &&
                (railShape == RailShape.NORTH_SOUTH ||
                        railShape == RailShape.ASCENDING_NORTH ||
                        railShape == RailShape.ASCENDING_SOUTH))
                || (shape == RailShape.NORTH_SOUTH &&
                (railShape == RailShape.EAST_WEST ||
                        railShape == RailShape.ASCENDING_EAST ||
                        railShape == RailShape.ASCENDING_WEST))) {
            return false;
        }

        return state.getValue(POWERED) && (world.isBlockPowered(pos) || findSignal(world, pos, state, forward, distance + 1));
    }

    @Override
    protected void onNeighborChangedInternal(World world, BlockPos pos, IBlockState state, Block neighborBlock) {
        boolean currentlyPowered = state.getValue(POWERED);
        boolean shouldBePowered = world.isBlockPowered(pos) ||
                this.findSignal(world, pos, state, true, 0) ||
                this.findSignal(world, pos, state, false, 0);

        if (currentlyPowered != shouldBePowered) {
            world.setBlockState(pos, state.withProperty(POWERED, shouldBePowered), 3);
            world.notifyNeighborsOfStateChange(pos.down(), this);

            if (state.getValue(SHAPE).isAscending()) {
                world.notifyNeighborsOfStateChange(pos.up(), this);
            }
        }
    }

    @Override
    public IProperty<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(SHAPE, RailShape.byMetadata(meta & 7))
                .withProperty(POWERED, (meta & 8) > 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(SHAPE).getMetadata() | (state.getValue(POWERED) ? 8 : 0);
    }


    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, SHAPE, POWERED);
    }
}
