package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockLever extends Block {
    public static final PropertyEnum<Orientation> FACING = PropertyEnum.create("facing", Orientation.class);
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    protected BlockLever() {
        super(Material.CIRCUITS);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, Orientation.NORTH).withProperty(POWERED, Boolean.FALSE));
        this.setCreativeTab(CreativeTabs.TAB_REDSTONE);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state) {
        return null;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean isFullCube() {
        return false;
    }

    @Override
    public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, Direction side) {
        return func_181090_a(worldIn, pos, side.getOpposite());
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        for (Direction enumfacing : Direction.values()) {
            if (func_181090_a(worldIn, pos, enumfacing)) {
                return true;
            }
        }

        return false;
    }

    protected static boolean func_181090_a(World p_181090_0_, BlockPos p_181090_1_, Direction p_181090_2_) {
        return BlockButton.func_181088_a(p_181090_0_, p_181090_1_, p_181090_2_);
    }

    @Override
    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        IBlockState iblockstate = this.getDefaultState().withProperty(POWERED, Boolean.FALSE);

        if (func_181090_a(worldIn, pos, facing.getOpposite())) {
            return iblockstate.withProperty(FACING, Orientation.forFacings(facing, placer.getHorizontalFacing()));
        } else {
            for (Direction enumfacing : Direction.Plane.HORIZONTAL) {
                if (enumfacing != facing && func_181090_a(worldIn, pos, enumfacing.getOpposite())) {
                    return iblockstate.withProperty(FACING, Orientation.forFacings(enumfacing, placer.getHorizontalFacing()));
                }
            }

            if (World.doesBlockHaveSolidTopSurface(worldIn, pos.down())) {
                return iblockstate.withProperty(FACING, Orientation.forFacings(Direction.UP, placer.getHorizontalFacing()));
            } else {
                return iblockstate;
            }
        }
    }

    public static int getMetadataForFacing(Direction facing) {
        return switch (facing) {
            case DOWN -> 0;
            case UP -> 5;
            case NORTH -> 4;
            case SOUTH -> 3;
            case WEST -> 2;
            case EAST -> 1;
            default -> -1;
        };
    }

    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
        if (this.func_181091_e(worldIn, pos, state) && !func_181090_a(worldIn, pos, state.getValue(FACING).getFacing().getOpposite())) {
            this.dropBlockAsItem(worldIn, pos, state, 0);
            worldIn.setBlockToAir(pos);
        }
    }

    private boolean func_181091_e(World p_181091_1_, BlockPos p_181091_2_, IBlockState p_181091_3_) {
        if (this.canPlaceBlockAt(p_181091_1_, p_181091_2_)) {
            return true;
        } else {
            this.dropBlockAsItem(p_181091_1_, p_181091_2_, p_181091_3_, 0);
            p_181091_1_.setBlockToAir(p_181091_2_);
            return false;
        }
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess worldIn, BlockPos pos) {
        float f = 0.1875F;

        switch (worldIn.getBlockState(pos).getValue(FACING)) {
            case EAST:
                this.setBlockBounds(0.0F, 0.2F, 0.5F - f, f * 2.0F, 0.8F, 0.5F + f);
                break;

            case WEST:
                this.setBlockBounds(1.0F - f * 2.0F, 0.2F, 0.5F - f, 1.0F, 0.8F, 0.5F + f);
                break;

            case SOUTH:
                this.setBlockBounds(0.5F - f, 0.2F, 0.0F, 0.5F + f, 0.8F, f * 2.0F);
                break;

            case NORTH:
                this.setBlockBounds(0.5F - f, 0.2F, 1.0F - f * 2.0F, 0.5F + f, 0.8F, 1.0F);
                break;

            case UP_Z:
            case UP_X:
                f = 0.25F;
                this.setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, 0.6F, 0.5F + f);
                break;

            case DOWN_X:
            case DOWN_Z:
                f = 0.25F;
                this.setBlockBounds(0.5F - f, 0.4F, 0.5F - f, 0.5F + f, 1.0F, 0.5F + f);
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, Direction side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            state = state.cycleProperty(POWERED);
            worldIn.setBlockState(pos, state, 3);
            worldIn.playSoundEffect(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, "random.click", 0.3F, state.getValue(POWERED) ? 0.6F : 0.5F);
            worldIn.notifyNeighborsOfStateChange(pos, this);
            Direction enumfacing = state.getValue(FACING).getFacing();
            worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing.getOpposite()), this);
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        if (state.getValue(POWERED)) {
            worldIn.notifyNeighborsOfStateChange(pos, this);
            Direction enumfacing = state.getValue(FACING).getFacing();
            worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing.getOpposite()), this);
        }

        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public int getWeakPower(IBlockAccess worldIn, BlockPos pos, IBlockState state, Direction side) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongPower(IBlockAccess worldIn, BlockPos pos, IBlockState state, Direction side) {
        return !state.getValue(POWERED) ? 0 : (state.getValue(FACING).getFacing() == side ? 15 : 0);
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, Orientation.byMetadata(meta & 7)).withProperty(POWERED, (meta & 8) > 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = 0;
        i = i | state.getValue(FACING).getMetadata();

        if (state.getValue(POWERED)) {
            i |= 8;
        }

        return i;
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, FACING, POWERED);
    }

    public enum Orientation implements IStringSerializable {
        DOWN_X(0, "down_x", Direction.DOWN),
        EAST(1, "east", Direction.EAST),
        WEST(2, "west", Direction.WEST),
        SOUTH(3, "south", Direction.SOUTH),
        NORTH(4, "north", Direction.NORTH),
        UP_Z(5, "up_z", Direction.UP),
        UP_X(6, "up_x", Direction.UP),
        DOWN_Z(7, "down_z", Direction.DOWN);

        private static final Orientation[] META_LOOKUP = new Orientation[values().length];
        private final int meta;
        private final String name;
        private final Direction facing;

        Orientation(int meta, String name, Direction facing) {
            this.meta = meta;
            this.name = name;
            this.facing = facing;
        }

        public int getMetadata() {
            return this.meta;
        }

        public Direction getFacing() {
            return this.facing;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public static Orientation byMetadata(int meta) {
            if (meta < 0 || meta >= META_LOOKUP.length) {
                meta = 0;
            }

            return META_LOOKUP[meta];
        }

        public static Orientation forFacings(Direction clickedSide, Direction entityFacing) {
            return switch (clickedSide) {
                case DOWN -> switch (entityFacing.getAxis()) {
                    case X -> DOWN_X;
                    case Z -> DOWN_Z;
                    default ->
                            throw new IllegalArgumentException("Invalid entityFacing " + entityFacing + " for facing " + clickedSide);
                };
                case UP -> switch (entityFacing.getAxis()) {
                    case X -> UP_X;
                    case Z -> UP_Z;
                    default ->
                            throw new IllegalArgumentException("Invalid entityFacing " + entityFacing + " for facing " + clickedSide);
                };
                case NORTH -> NORTH;
                case SOUTH -> SOUTH;
                case WEST -> WEST;
                case EAST -> EAST;
            };
        }

        @Override
        public String getName() {
            return this.name;
        }

        static {
            for (Orientation blocklever$enumorientation : values()) {
                META_LOOKUP[blocklever$enumorientation.getMetadata()] = blocklever$enumorientation;
            }
        }
    }
}
