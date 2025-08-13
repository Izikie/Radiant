package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import net.minecraft.util.input.MovingObjectPosition;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3;
import net.minecraft.world.ParticleTypes;
import net.minecraft.world.World;

import java.util.Random;

public class BlockTorch extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", p_apply_1_ -> p_apply_1_ != Direction.DOWN);

    protected BlockTorch() {
        super(Material.CIRCUITS);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, Direction.UP));
        this.setTickRandomly(true);
        this.setCreativeTab(CreativeTabs.TAB_DECORATIONS);
    }

    public AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state) {
        return null;
    }

    public boolean isOpaqueCube() {
        return false;
    }

    public boolean isFullCube() {
        return false;
    }

    private boolean canPlaceOn(World worldIn, BlockPos pos) {
        if (World.doesBlockHaveSolidTopSurface(worldIn, pos)) {
            return true;
        } else {
            Block block = worldIn.getBlockState(pos).getBlock();
            return block instanceof BlockFence || block == Blocks.GLASS || block == Blocks.COBBLESTONE_WALL || block == Blocks.STAINED_GLASS;
        }
    }

    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        for (Direction enumfacing : FACING.getAllowedValues()) {
            if (this.canPlaceAt(worldIn, pos, enumfacing)) {
                return true;
            }
        }

        return false;
    }

    private boolean canPlaceAt(World worldIn, BlockPos pos, Direction facing) {
        BlockPos blockpos = pos.offset(facing.getOpposite());
        boolean flag = facing.getAxis().isHorizontal();
        return flag && worldIn.isBlockNormalCube(blockpos, true) || facing == Direction.UP && this.canPlaceOn(worldIn, blockpos);
    }

    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        if (this.canPlaceAt(worldIn, pos, facing)) {
            return this.getDefaultState().withProperty(FACING, facing);
        } else {
            for (Direction enumfacing : Direction.Plane.HORIZONTAL) {
                if (worldIn.isBlockNormalCube(pos.offset(enumfacing.getOpposite()), true)) {
                    return this.getDefaultState().withProperty(FACING, enumfacing);
                }
            }

            return this.getDefaultState();
        }
    }

    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        this.checkForDrop(worldIn, pos, state);
    }

    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
        this.onNeighborChangeInternal(worldIn, pos, state);
    }

    protected boolean onNeighborChangeInternal(World worldIn, BlockPos pos, IBlockState state) {
        if (!this.checkForDrop(worldIn, pos, state)) {
            return true;
        } else {
            Direction enumfacing = state.getValue(FACING);
            Direction.Axis enumfacing$axis = enumfacing.getAxis();
            Direction enumfacing1 = enumfacing.getOpposite();
            boolean flag = false;

            if (enumfacing$axis.isHorizontal() && !worldIn.isBlockNormalCube(pos.offset(enumfacing1), true)) {
                flag = true;
            } else if (enumfacing$axis.isVertical() && !this.canPlaceOn(worldIn, pos.offset(enumfacing1))) {
                flag = true;
            }

            if (flag) {
                this.dropBlockAsItem(worldIn, pos, state, 0);
                worldIn.setBlockToAir(pos);
                return true;
            } else {
                return false;
            }
        }
    }

    protected boolean checkForDrop(World worldIn, BlockPos pos, IBlockState state) {
        if (state.getBlock() == this && this.canPlaceAt(worldIn, pos, state.getValue(FACING))) {
            return true;
        } else {
            if (worldIn.getBlockState(pos).getBlock() == this) {
                this.dropBlockAsItem(worldIn, pos, state, 0);
                worldIn.setBlockToAir(pos);
            }

            return false;
        }
    }

    public MovingObjectPosition collisionRayTrace(World worldIn, BlockPos pos, Vec3 start, Vec3 end) {
        Direction enumfacing = worldIn.getBlockState(pos).getValue(FACING);
        float f = 0.15F;

        if (enumfacing == Direction.EAST) {
            this.setBlockBounds(0.0F, 0.2F, 0.5F - f, f * 2.0F, 0.8F, 0.5F + f);
        } else if (enumfacing == Direction.WEST) {
            this.setBlockBounds(1.0F - f * 2.0F, 0.2F, 0.5F - f, 1.0F, 0.8F, 0.5F + f);
        } else if (enumfacing == Direction.SOUTH) {
            this.setBlockBounds(0.5F - f, 0.2F, 0.0F, 0.5F + f, 0.8F, f * 2.0F);
        } else if (enumfacing == Direction.NORTH) {
            this.setBlockBounds(0.5F - f, 0.2F, 1.0F - f * 2.0F, 0.5F + f, 0.8F, 1.0F);
        } else {
            f = 0.1F;
            this.setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, 0.6F, 0.5F + f);
        }

        return super.collisionRayTrace(worldIn, pos, start, end);
    }

    public void randomDisplayTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        Direction enumfacing = state.getValue(FACING);
        double d0 = pos.getX() + 0.5D;
        double d1 = pos.getY() + 0.7D;
        double d2 = pos.getZ() + 0.5D;
        double d3 = 0.22D;
        double d4 = 0.27D;

        if (enumfacing.getAxis().isHorizontal()) {
            Direction enumfacing1 = enumfacing.getOpposite();
            worldIn.spawnParticle(ParticleTypes.SMOKE_NORMAL, d0 + d4 * enumfacing1.getFrontOffsetX(), d1 + d3, d2 + d4 * enumfacing1.getFrontOffsetZ(), 0.0D, 0.0D, 0.0D);
            worldIn.spawnParticle(ParticleTypes.FLAME, d0 + d4 * enumfacing1.getFrontOffsetX(), d1 + d3, d2 + d4 * enumfacing1.getFrontOffsetZ(), 0.0D, 0.0D, 0.0D);
        } else {
            worldIn.spawnParticle(ParticleTypes.SMOKE_NORMAL, d0, d1, d2, 0.0D, 0.0D, 0.0D);
            worldIn.spawnParticle(ParticleTypes.FLAME, d0, d1, d2, 0.0D, 0.0D, 0.0D);
        }
    }

    public RenderLayer getBlockLayer() {
        return RenderLayer.CUTOUT;
    }

    public IBlockState getStateFromMeta(int meta) {
        IBlockState iblockstate = this.getDefaultState();

        iblockstate = switch (meta) {
            case 1 -> iblockstate.withProperty(FACING, Direction.EAST);
            case 2 -> iblockstate.withProperty(FACING, Direction.WEST);
            case 3 -> iblockstate.withProperty(FACING, Direction.SOUTH);
            case 4 -> iblockstate.withProperty(FACING, Direction.NORTH);
            default -> iblockstate.withProperty(FACING, Direction.UP);
        };

        return iblockstate;
    }

    public int getMetaFromState(IBlockState state) {
        int i = 0;

        i = switch (state.getValue(FACING)) {
            case EAST -> i | 1;
            case WEST -> i | 2;
            case SOUTH -> i | 3;
            case NORTH -> i | 4;
            default -> i | 5;
        };

        return i;
    }

    protected BlockState createBlockState() {
        return new BlockState(this, FACING);
    }
}
