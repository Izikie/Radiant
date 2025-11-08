package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockPistonStructureHelper;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;

public class BlockPistonBase extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    public static final PropertyBool EXTENDED = PropertyBool.create("extended");
    private final boolean isSticky;

    public BlockPistonBase(boolean isSticky) {
        super(Material.PISTON);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, Direction.NORTH).withProperty(EXTENDED, Boolean.FALSE));
        this.isSticky = isSticky;
        this.setStepSound(soundTypePiston);
        this.setHardness(0.5F);
        this.setCreativeTab(CreativeTabs.TAB_REDSTONE);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        worldIn.setBlockState(pos, state.withProperty(FACING, getFacingFromEntity(worldIn, pos, placer)), 2);

        if (!worldIn.isRemote) {
            this.checkForMove(worldIn, pos, state);
        }
    }

    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
        if (!worldIn.isRemote) {
            this.checkForMove(worldIn, pos, state);
        }
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote && worldIn.getTileEntity(pos) == null) {
            this.checkForMove(worldIn, pos, state);
        }
    }

    @Override
    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(FACING, getFacingFromEntity(worldIn, pos, placer)).withProperty(EXTENDED, Boolean.FALSE);
    }

    private void checkForMove(World worldIn, BlockPos pos, IBlockState state) {
        Direction enumfacing = state.getValue(FACING);
        boolean flag = this.shouldBeExtended(worldIn, pos, enumfacing);

        if (flag && !state.getValue(EXTENDED)) {
            if ((new BlockPistonStructureHelper(worldIn, pos, enumfacing, true)).canMove()) {
                worldIn.addBlockEvent(pos, this, 0, enumfacing.getIndex());
            }
        } else if (!flag && state.getValue(EXTENDED)) {
            worldIn.setBlockState(pos, state.withProperty(EXTENDED, Boolean.FALSE), 2);
            worldIn.addBlockEvent(pos, this, 1, enumfacing.getIndex());
        }
    }

    private boolean shouldBeExtended(World worldIn, BlockPos pos, Direction facing) {
        for (Direction enumfacing : Direction.values()) {
            if (enumfacing != facing && worldIn.isSidePowered(pos.offset(enumfacing), enumfacing)) {
                return true;
            }
        }

        if (worldIn.isSidePowered(pos, Direction.DOWN)) {
            return true;
        } else {
            BlockPos blockpos = pos.up();

            for (Direction enumfacing1 : Direction.values()) {
                if (enumfacing1 != Direction.DOWN && worldIn.isSidePowered(blockpos.offset(enumfacing1), enumfacing1)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean onBlockEventReceived(World worldIn, BlockPos pos, IBlockState state, int eventID, int eventParam) {
        Direction enumfacing = state.getValue(FACING);

        if (!worldIn.isRemote) {
            boolean flag = this.shouldBeExtended(worldIn, pos, enumfacing);

            if (flag && eventID == 1) {
                worldIn.setBlockState(pos, state.withProperty(EXTENDED, Boolean.TRUE), 2);
                return false;
            }

            if (!flag && eventID == 0) {
                return false;
            }
        }

        if (eventID == 0) {
            if (!this.doMove(worldIn, pos, enumfacing, true)) {
                return false;
            }

            worldIn.setBlockState(pos, state.withProperty(EXTENDED, Boolean.TRUE), 2);
            worldIn.playSoundEffect(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, "tile.piston.out", 0.5F, worldIn.rand.nextFloat() * 0.25F + 0.6F);
        } else if (eventID == 1) {
            TileEntity tileentity1 = worldIn.getTileEntity(pos.offset(enumfacing));

            if (tileentity1 instanceof TileEntityPiston tileEntityPiston) {
                tileEntityPiston.clearPistonTileEntity();
            }

            worldIn.setBlockState(pos, Blocks.PISTON_EXTENSION.getDefaultState().withProperty(BlockPistonMoving.FACING, enumfacing).withProperty(BlockPistonMoving.TYPE, this.isSticky ? BlockPistonExtension.PistonType.STICKY : BlockPistonExtension.PistonType.DEFAULT), 3);
            worldIn.setTileEntity(pos, BlockPistonMoving.newTileEntity(this.getStateFromMeta(eventParam), enumfacing, false, true));

            if (this.isSticky) {
                BlockPos blockpos = pos.add(enumfacing.getFrontOffsetX() * 2, enumfacing.getFrontOffsetY() * 2, enumfacing.getFrontOffsetZ() * 2);
                Block block = worldIn.getBlockState(blockpos).getBlock();
                boolean flag1 = false;

                if (block == Blocks.PISTON_EXTENSION) {
                    TileEntity tileentity = worldIn.getTileEntity(blockpos);

                    if (tileentity instanceof TileEntityPiston tileentitypiston) {

                        if (tileentitypiston.getFacing() == enumfacing && tileentitypiston.isExtending()) {
                            tileentitypiston.clearPistonTileEntity();
                            flag1 = true;
                        }
                    }
                }

                if (!flag1 && block.getMaterial() != Material.AIR && canPush(block, worldIn, blockpos, enumfacing.getOpposite(), false) && (block.getMobilityFlag() == 0 || block == Blocks.PISTON || block == Blocks.STICKY_PISTON)) {
                    this.doMove(worldIn, pos, enumfacing, false);
                }
            } else {
                worldIn.setBlockToAir(pos.offset(enumfacing));
            }

            worldIn.playSoundEffect(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, "tile.piston.in", 0.5F, worldIn.rand.nextFloat() * 0.15F + 0.6F);
        }

        return true;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess worldIn, BlockPos pos) {
        IBlockState iblockstate = worldIn.getBlockState(pos);

        if (iblockstate.getBlock() == this && iblockstate.getValue(EXTENDED)) {
            float f = 0.25F;
            Direction enumfacing = iblockstate.getValue(FACING);

            if (enumfacing != null) {
                switch (enumfacing) {
                    case DOWN:
                        this.setBlockBounds(0.0F, 0.25F, 0.0F, 1.0F, 1.0F, 1.0F);
                        break;

                    case UP:
                        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.75F, 1.0F);
                        break;

                    case NORTH:
                        this.setBlockBounds(0.0F, 0.0F, 0.25F, 1.0F, 1.0F, 1.0F);
                        break;

                    case SOUTH:
                        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.75F);
                        break;

                    case WEST:
                        this.setBlockBounds(0.25F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                        break;

                    case EAST:
                        this.setBlockBounds(0.0F, 0.0F, 0.0F, 0.75F, 1.0F, 1.0F);
                }
            }
        } else {
            this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public void setBlockBoundsForItemRender() {
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void addCollisionBoxesToList(World worldIn, BlockPos pos, IBlockState state, AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collidingEntity) {
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        super.addCollisionBoxesToList(worldIn, pos, state, mask, list, collidingEntity);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state) {
        this.setBlockBoundsBasedOnState(worldIn, pos);
        return super.getCollisionBoundingBox(worldIn, pos, state);
    }

    @Override
    public boolean isFullCube() {
        return false;
    }

    public static Direction getFacing(int meta) {
        int i = meta & 7;
        return i > 5 ? null : Direction.getFront(i);
    }

    public static Direction getFacingFromEntity(World worldIn, BlockPos clickedBlock, EntityLivingBase entityIn) {
        if (MathHelper.abs((float) entityIn.posX - clickedBlock.getX()) < 2.0F && MathHelper.abs((float) entityIn.posZ - clickedBlock.getZ()) < 2.0F) {
            double d0 = entityIn.posY + entityIn.getEyeHeight();

            if (d0 - clickedBlock.getY() > 2.0D) {
                return Direction.UP;
            }

            if (clickedBlock.getY() - d0 > 0.0D) {
                return Direction.DOWN;
            }
        }

        return entityIn.getHorizontalFacing().getOpposite();
    }

    public static boolean canPush(Block blockIn, World worldIn, BlockPos pos, Direction direction, boolean allowDestroy) {
        if (blockIn == Blocks.OBSIDIAN) {
            return false;
        } else if (!worldIn.getWorldBorder().contains(pos)) {
            return false;
        } else if (pos.getY() >= 0 && (direction != Direction.DOWN || pos.getY() != 0)) {
            if (pos.getY() <= worldIn.getHeight() - 1 && (direction != Direction.UP || pos.getY() != worldIn.getHeight() - 1)) {
                if (blockIn != Blocks.PISTON && blockIn != Blocks.STICKY_PISTON) {
                    if (blockIn.getBlockHardness(worldIn, pos) == -1.0F) {
                        return false;
                    }

                    if (blockIn.getMobilityFlag() == 2) {
                        return false;
                    }

                    if (blockIn.getMobilityFlag() == 1) {
                        return allowDestroy;
                    }
                } else if (worldIn.getBlockState(pos).getValue(EXTENDED)) {
                    return false;
                }

                return !(blockIn instanceof ITileEntityProvider);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean doMove(World worldIn, BlockPos pos, Direction direction, boolean extending) {
        if (!extending) {
            worldIn.setBlockToAir(pos.offset(direction));
        }

        BlockPistonStructureHelper blockpistonstructurehelper = new BlockPistonStructureHelper(worldIn, pos, direction, extending);
        List<BlockPos> list = blockpistonstructurehelper.getBlocksToMove();
        List<BlockPos> list1 = blockpistonstructurehelper.getBlocksToDestroy();

        if (!blockpistonstructurehelper.canMove()) {
            return false;
        } else {
            int i = list.size() + list1.size();
            Block[] ablock = new Block[i];
            Direction enumfacing = extending ? direction : direction.getOpposite();

            for (int j = list1.size() - 1; j >= 0; --j) {
                BlockPos blockpos = list1.get(j);
                Block block = worldIn.getBlockState(blockpos).getBlock();
                block.dropBlockAsItem(worldIn, blockpos, worldIn.getBlockState(blockpos), 0);
                worldIn.setBlockToAir(blockpos);
                --i;
                ablock[i] = block;
            }

            for (int k = list.size() - 1; k >= 0; --k) {
                BlockPos blockpos2 = list.get(k);
                IBlockState iblockstate = worldIn.getBlockState(blockpos2);
                Block block1 = iblockstate.getBlock();
                block1.getMetaFromState(iblockstate);
                worldIn.setBlockToAir(blockpos2);
                blockpos2 = blockpos2.offset(enumfacing);
                worldIn.setBlockState(blockpos2, Blocks.PISTON_EXTENSION.getDefaultState().withProperty(FACING, direction), 4);
                worldIn.setTileEntity(blockpos2, BlockPistonMoving.newTileEntity(iblockstate, direction, extending, false));
                --i;
                ablock[i] = block1;
            }

            BlockPos blockpos1 = pos.offset(direction);

            if (extending) {
                BlockPistonExtension.PistonType blockpistonextension$enumpistontype = this.isSticky ? BlockPistonExtension.PistonType.STICKY : BlockPistonExtension.PistonType.DEFAULT;
                IBlockState iblockstate1 = Blocks.PISTON_HEAD.getDefaultState().withProperty(BlockPistonExtension.FACING, direction).withProperty(BlockPistonExtension.TYPE, blockpistonextension$enumpistontype);
                IBlockState iblockstate2 = Blocks.PISTON_EXTENSION.getDefaultState().withProperty(BlockPistonMoving.FACING, direction).withProperty(BlockPistonMoving.TYPE, this.isSticky ? BlockPistonExtension.PistonType.STICKY : BlockPistonExtension.PistonType.DEFAULT);
                worldIn.setBlockState(blockpos1, iblockstate2, 4);
                worldIn.setTileEntity(blockpos1, BlockPistonMoving.newTileEntity(iblockstate1, direction, true, false));
            }

            for (int l = list1.size() - 1; l >= 0; --l) {
                worldIn.notifyNeighborsOfStateChange(list1.get(l), ablock[i++]);
            }

            for (int i1 = list.size() - 1; i1 >= 0; --i1) {
                worldIn.notifyNeighborsOfStateChange(list.get(i1), ablock[i++]);
            }

            if (extending) {
                worldIn.notifyNeighborsOfStateChange(blockpos1, Blocks.PISTON_HEAD);
                worldIn.notifyNeighborsOfStateChange(pos, this);
            }

            return true;
        }
    }

    @Override
    public IBlockState getStateForEntityRender(IBlockState state) {
        return this.getDefaultState().withProperty(FACING, Direction.UP);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, getFacing(meta)).withProperty(EXTENDED, (meta & 8) > 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = 0;
        i = i | state.getValue(FACING).getIndex();

        if (state.getValue(EXTENDED)) {
            i |= 8;
        }

        return i;
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, FACING, EXTENDED);
    }
}
