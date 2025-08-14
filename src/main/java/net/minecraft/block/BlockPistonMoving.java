package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.*;
import net.minecraft.util.input.MovingObjectPosition;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

public class BlockPistonMoving extends BlockContainer {
    public static final PropertyDirection FACING = BlockPistonExtension.FACING;
    public static final PropertyEnum<BlockPistonExtension.PistonType> TYPE = BlockPistonExtension.TYPE;

    public BlockPistonMoving() {
        super(Material.PISTON);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, Direction.NORTH).withProperty(TYPE, BlockPistonExtension.PistonType.DEFAULT));
        this.setHardness(-1.0F);
    }

    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return null;
    }

    public static TileEntity newTileEntity(IBlockState state, Direction facing, boolean extending, boolean renderHead) {
        return new TileEntityPiston(state, facing, extending, renderHead);
    }

    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileentity = worldIn.getTileEntity(pos);

        if (tileentity instanceof TileEntityPiston tileEntityPiston) {
            tileEntityPiston.clearPistonTileEntity();
        } else {
            super.breakBlock(worldIn, pos, state);
        }
    }

    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return false;
    }

    public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, Direction side) {
        return false;
    }

    public void onBlockDestroyedByPlayer(World worldIn, BlockPos pos, IBlockState state) {
        BlockPos blockpos = pos.offset(state.getValue(FACING).getOpposite());
        IBlockState iblockstate = worldIn.getBlockState(blockpos);

        if (iblockstate.getBlock() instanceof BlockPistonBase && iblockstate.getValue(BlockPistonBase.EXTENDED)) {
            worldIn.setBlockToAir(blockpos);
        }
    }

    public boolean isOpaqueCube() {
        return false;
    }

    public boolean isFullCube() {
        return false;
    }

    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, Direction side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote && worldIn.getTileEntity(pos) == null) {
            worldIn.setBlockToAir(pos);
            return true;
        } else {
            return false;
        }
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return null;
    }

    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
        if (!worldIn.isRemote) {
            TileEntityPiston tileentitypiston = this.getTileEntity(worldIn, pos);

            if (tileentitypiston != null) {
                IBlockState iblockstate = tileentitypiston.getPistonState();
                iblockstate.getBlock().dropBlockAsItem(worldIn, pos, iblockstate, 0);
            }
        }
    }

    public MovingObjectPosition collisionRayTrace(World worldIn, BlockPos pos, Vec3 start, Vec3 end) {
        return null;
    }

    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
        if (!worldIn.isRemote) {
            worldIn.getTileEntity(pos);
        }
    }

    public AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state) {
        TileEntityPiston tileentitypiston = this.getTileEntity(worldIn, pos);

        if (tileentitypiston == null) {
            return null;
        } else {
            float f = tileentitypiston.getProgress(0.0F);

            if (tileentitypiston.isExtending()) {
                f = 1.0F - f;
            }

            return this.getBoundingBox(worldIn, pos, tileentitypiston.getPistonState(), f, tileentitypiston.getFacing());
        }
    }

    public void setBlockBoundsBasedOnState(IBlockAccess worldIn, BlockPos pos) {
        TileEntityPiston tileentitypiston = this.getTileEntity(worldIn, pos);

        if (tileentitypiston != null) {
            IBlockState iblockstate = tileentitypiston.getPistonState();
            Block block = iblockstate.getBlock();

            if (block == this || block.getMaterial() == Material.AIR) {
                return;
            }

            float f = tileentitypiston.getProgress(0.0F);

            if (tileentitypiston.isExtending()) {
                f = 1.0F - f;
            }

            block.setBlockBoundsBasedOnState(worldIn, pos);

            if (block == Blocks.PISTON || block == Blocks.STICKY_PISTON) {
                f = 0.0F;
            }

            Direction enumfacing = tileentitypiston.getFacing();
            this.minX = block.getBlockBoundsMinX() - (enumfacing.getFrontOffsetX() * f);
            this.minY = block.getBlockBoundsMinY() - (enumfacing.getFrontOffsetY() * f);
            this.minZ = block.getBlockBoundsMinZ() - (enumfacing.getFrontOffsetZ() * f);
            this.maxX = block.getBlockBoundsMaxX() - (enumfacing.getFrontOffsetX() * f);
            this.maxY = block.getBlockBoundsMaxY() - (enumfacing.getFrontOffsetY() * f);
            this.maxZ = block.getBlockBoundsMaxZ() - (enumfacing.getFrontOffsetZ() * f);
        }
    }

    public AxisAlignedBB getBoundingBox(World worldIn, BlockPos pos, IBlockState extendingBlock, float progress, Direction direction) {
        if (extendingBlock.getBlock() != this && extendingBlock.getBlock().getMaterial() != Material.AIR) {
            AxisAlignedBB axisalignedbb = extendingBlock.getBlock().getCollisionBoundingBox(worldIn, pos, extendingBlock);

            if (axisalignedbb == null) {
                return null;
            } else {
                double d0 = axisalignedbb.minX;
                double d1 = axisalignedbb.minY;
                double d2 = axisalignedbb.minZ;
                double d3 = axisalignedbb.maxX;
                double d4 = axisalignedbb.maxY;
                double d5 = axisalignedbb.maxZ;

                if (direction.getFrontOffsetX() < 0) {
                    d0 -= (direction.getFrontOffsetX() * progress);
                } else {
                    d3 -= (direction.getFrontOffsetX() * progress);
                }

                if (direction.getFrontOffsetY() < 0) {
                    d1 -= (direction.getFrontOffsetY() * progress);
                } else {
                    d4 -= (direction.getFrontOffsetY() * progress);
                }

                if (direction.getFrontOffsetZ() < 0) {
                    d2 -= (direction.getFrontOffsetZ() * progress);
                } else {
                    d5 -= (direction.getFrontOffsetZ() * progress);
                }

                return new AxisAlignedBB(d0, d1, d2, d3, d4, d5);
            }
        } else {
            return null;
        }
    }

    private TileEntityPiston getTileEntity(IBlockAccess worldIn, BlockPos pos) {
        TileEntity tileentity = worldIn.getTileEntity(pos);
        return tileentity instanceof TileEntityPiston tileEntityPiston ? tileEntityPiston : null;
    }

    public Item getItem(World worldIn, BlockPos pos) {
        return null;
    }

    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, BlockPistonExtension.getFacing(meta)).withProperty(TYPE, (meta & 8) > 0 ? BlockPistonExtension.PistonType.STICKY : BlockPistonExtension.PistonType.DEFAULT);
    }

    public int getMetaFromState(IBlockState state) {
        int i = 0;
        i = i | state.getValue(FACING).getIndex();

        if (state.getValue(TYPE) == BlockPistonExtension.PistonType.STICKY) {
            i |= 8;
        }

        return i;
    }

    protected BlockState createBlockState() {
        return new BlockState(this, FACING, TYPE);
    }
}
