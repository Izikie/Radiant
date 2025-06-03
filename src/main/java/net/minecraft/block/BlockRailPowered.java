package net.minecraft.block;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class BlockRailPowered extends BlockRailBase {
    public static final PropertyEnum<RailShape> SHAPE = PropertyEnum.create("shape", RailShape.class, p_apply_1_ -> p_apply_1_ != RailShape.NORTH_EAST && p_apply_1_ != RailShape.NORTH_WEST && p_apply_1_ != RailShape.SOUTH_EAST && p_apply_1_ != RailShape.SOUTH_WEST);
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    protected BlockRailPowered() {
        super(true);
        this.setDefaultState(this.blockState.getBaseState().withProperty(SHAPE, RailShape.NORTH_SOUTH).withProperty(POWERED, Boolean.FALSE));
    }

    protected boolean findSignal(World world, BlockPos pos, IBlockState state, boolean forward, int distance) {
        if (distance >= 8) {
            return false;
        } else {
            int i = pos.getX();
            int j = pos.getY();
            int k = pos.getZ();
            boolean flag = true;
            RailShape railShape = state.getValue(SHAPE);

            switch (railShape) {
                case NORTH_SOUTH -> {
                    if (forward) {
                        ++k;
                    } else {
                        --k;
                    }
                }
                case EAST_WEST -> {
                    if (forward) {
                        --i;
                    } else {
                        ++i;
                    }
                }
                case ASCENDING_EAST -> {
                    if (forward) {
                        --i;
                    } else {
                        ++i;
                        ++j;
                        flag = false;
                    }

                    railShape = RailShape.EAST_WEST;
                }
                case ASCENDING_WEST -> {
                    if (forward) {
                        --i;
                        ++j;
                        flag = false;
                    } else {
                        ++i;
                    }

                    railShape = RailShape.EAST_WEST;
                }
                case ASCENDING_NORTH -> {
                    if (forward) {
                        ++k;
                    } else {
                        --k;
                        ++j;
                        flag = false;
                    }

                    railShape = RailShape.NORTH_SOUTH;
                }
                case ASCENDING_SOUTH -> {
                    if (forward) {
                        ++k;
                        ++j;
                        flag = false;
                    } else {
                        --k;
                    }

                    railShape = RailShape.NORTH_SOUTH;
                }
            }

            return this.func_176567_a(world, new BlockPos(i, j, k), forward, distance, railShape) || flag && this.func_176567_a(world, new BlockPos(i, j - 1, k), forward, distance, railShape);
        }
    }

    protected boolean func_176567_a(World world, BlockPos pos, boolean forward, int distance, RailShape expectedShape) {
        IBlockState state = world.getBlockState(pos);

        if (state.getBlock() != this) {
            return false;
        } else {
            RailShape actualShape = state.getValue(SHAPE);
            return (expectedShape != RailShape.EAST_WEST || actualShape != RailShape.NORTH_SOUTH && actualShape != RailShape.ASCENDING_NORTH && actualShape != RailShape.ASCENDING_SOUTH)
                    && ((expectedShape != RailShape.NORTH_SOUTH || actualShape != RailShape.EAST_WEST && actualShape != RailShape.ASCENDING_EAST && actualShape != RailShape.ASCENDING_WEST)
                    && (state.getValue(POWERED) && (world.isBlockPowered(pos) || this.findSignal(world, pos, state, forward, distance + 1))));
        }
    }

    protected void onNeighborChangedInternal(World world, BlockPos pos, IBlockState state, Block neighborBlock) {
        boolean flag = state.getValue(POWERED);
        boolean flag1 = world.isBlockPowered(pos) || this.findSignal(world, pos, state, true, 0) || this.findSignal(world, pos, state, false, 0);

        if (flag1 != flag) {
            world.setBlockState(pos, state.withProperty(POWERED, flag1), 3);
            world.notifyNeighborsOfStateChange(pos.down(), this);

            if (state.getValue(SHAPE).isAscending()) {
                world.notifyNeighborsOfStateChange(pos.up(), this);
            }
        }
    }

    public IProperty<RailShape> getShapeProperty() {
        return SHAPE;
    }

    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(SHAPE, RailShape.byMetadata(meta & 7)).withProperty(POWERED, (meta & 8) > 0);
    }

    public int getMetaFromState(IBlockState state) {
        int i = 0;
        i = i | state.getValue(SHAPE).getMetadata();

        if (state.getValue(POWERED)) {
            i |= 8;
        }

        return i;
    }

    protected BlockState createBlockState() {
        return new BlockState(this, SHAPE, POWERED);
    }
}
