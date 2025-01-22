package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.World;

public class BlockChest extends BlockContainer {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", Direction.Plane.HORIZONTAL);
    public final int chestType;

    protected BlockChest(int type) {
        super(Material.WOOD);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, Direction.NORTH));
        this.chestType = type;
        this.setCreativeTab(CreativeTabs.tabDecorations);
        this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
    }

    public boolean isOpaqueCube() {
        return false;
    }

    public boolean isFullCube() {
        return false;
    }

    public int getRenderType() {
        return 2;
    }

    public void setBlockBoundsBasedOnState(IBlockAccess worldIn, BlockPos pos) {
        if (worldIn.getBlockState(pos.north()).getBlock() == this) {
            this.setBlockBounds(0.0625F, 0.0F, 0.0F, 0.9375F, 0.875F, 0.9375F);
        } else if (worldIn.getBlockState(pos.south()).getBlock() == this) {
            this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 1.0F);
        } else if (worldIn.getBlockState(pos.west()).getBlock() == this) {
            this.setBlockBounds(0.0F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
        } else if (worldIn.getBlockState(pos.east()).getBlock() == this) {
            this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 1.0F, 0.875F, 0.9375F);
        } else {
            this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
        }
    }

    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        this.checkForSurroundingChests(worldIn, pos, state);

        for (Direction enumfacing : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = pos.offset(enumfacing);
            IBlockState iblockstate = worldIn.getBlockState(blockpos);

            if (iblockstate.getBlock() == this) {
                this.checkForSurroundingChests(worldIn, blockpos, iblockstate);
            }
        }
    }

    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
    }

    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        Direction enumfacing = Direction.getHorizontal(MathHelper.floor_double((placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3).getOpposite();
        state = state.withProperty(FACING, enumfacing);
        BlockPos blockpos = pos.north();
        BlockPos blockpos1 = pos.south();
        BlockPos blockpos2 = pos.west();
        BlockPos blockpos3 = pos.east();
        boolean flag = this == worldIn.getBlockState(blockpos).getBlock();
        boolean flag1 = this == worldIn.getBlockState(blockpos1).getBlock();
        boolean flag2 = this == worldIn.getBlockState(blockpos2).getBlock();
        boolean flag3 = this == worldIn.getBlockState(blockpos3).getBlock();

        if (!flag && !flag1 && !flag2 && !flag3) {
            worldIn.setBlockState(pos, state, 3);
        } else if (enumfacing.getAxis() != Direction.Axis.X || !flag && !flag1) {
            if (enumfacing.getAxis() == Direction.Axis.Z && (flag2 || flag3)) {
                if (flag2) {
                    worldIn.setBlockState(blockpos2, state, 3);
                } else {
                    worldIn.setBlockState(blockpos3, state, 3);
                }

                worldIn.setBlockState(pos, state, 3);
            }
        } else {
            if (flag) {
                worldIn.setBlockState(blockpos, state, 3);
            } else {
                worldIn.setBlockState(blockpos1, state, 3);
            }

            worldIn.setBlockState(pos, state, 3);
        }

        if (stack.hasDisplayName()) {
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity instanceof TileEntityChest tileEntityChest) {
                tileEntityChest.setCustomName(stack.getDisplayName());
            }
        }
    }

    public IBlockState checkForSurroundingChests(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            IBlockState iblockstate = worldIn.getBlockState(pos.north());
            IBlockState iblockstate1 = worldIn.getBlockState(pos.south());
            IBlockState iblockstate2 = worldIn.getBlockState(pos.west());
            IBlockState iblockstate3 = worldIn.getBlockState(pos.east());
            Direction enumfacing = state.getValue(FACING);
            Block block = iblockstate.getBlock();
            Block block1 = iblockstate1.getBlock();
            Block block2 = iblockstate2.getBlock();
            Block block3 = iblockstate3.getBlock();

            if (block != this && block1 != this) {
                boolean flag = block.isFullBlock();
                boolean flag1 = block1.isFullBlock();

                if (block2 == this || block3 == this) {
                    BlockPos blockpos1 = block2 == this ? pos.west() : pos.east();
                    IBlockState iblockstate6 = worldIn.getBlockState(blockpos1.north());
                    IBlockState iblockstate7 = worldIn.getBlockState(blockpos1.south());
                    enumfacing = Direction.SOUTH;
                    Direction enumfacing2;

                    if (block2 == this) {
                        enumfacing2 = iblockstate2.getValue(FACING);
                    } else {
                        enumfacing2 = iblockstate3.getValue(FACING);
                    }

                    if (enumfacing2 == Direction.NORTH) {
                        enumfacing = Direction.NORTH;
                    }

                    Block block6 = iblockstate6.getBlock();
                    Block block7 = iblockstate7.getBlock();

                    if ((flag || block6.isFullBlock()) && !flag1 && !block7.isFullBlock()) {
                        enumfacing = Direction.SOUTH;
                    }

                    if ((flag1 || block7.isFullBlock()) && !flag && !block6.isFullBlock()) {
                        enumfacing = Direction.NORTH;
                    }
                }
            } else {
                BlockPos blockpos = block == this ? pos.north() : pos.south();
                IBlockState iblockstate4 = worldIn.getBlockState(blockpos.west());
                IBlockState iblockstate5 = worldIn.getBlockState(blockpos.east());
                enumfacing = Direction.EAST;
                Direction enumfacing1;

                if (block == this) {
                    enumfacing1 = iblockstate.getValue(FACING);
                } else {
                    enumfacing1 = iblockstate1.getValue(FACING);
                }

                if (enumfacing1 == Direction.WEST) {
                    enumfacing = Direction.WEST;
                }

                Block block4 = iblockstate4.getBlock();
                Block block5 = iblockstate5.getBlock();

                if ((block2.isFullBlock() || block4.isFullBlock()) && !block3.isFullBlock() && !block5.isFullBlock()) {
                    enumfacing = Direction.EAST;
                }

                if ((block3.isFullBlock() || block5.isFullBlock()) && !block2.isFullBlock() && !block4.isFullBlock()) {
                    enumfacing = Direction.WEST;
                }
            }

            state = state.withProperty(FACING, enumfacing);
            worldIn.setBlockState(pos, state, 3);
        }
        return state;
    }

    public IBlockState correctFacing(World worldIn, BlockPos pos, IBlockState state) {
        Direction enumfacing = null;

        for (Direction enumfacing1 : Direction.Plane.HORIZONTAL) {
            IBlockState iblockstate = worldIn.getBlockState(pos.offset(enumfacing1));

            if (iblockstate.getBlock() == this) {
                return state;
            }

            if (iblockstate.getBlock().isFullBlock()) {
                if (enumfacing != null) {
                    enumfacing = null;
                    break;
                }

                enumfacing = enumfacing1;
            }
        }

        if (enumfacing != null) {
            return state.withProperty(FACING, enumfacing.getOpposite());
        } else {
            Direction enumfacing2 = state.getValue(FACING);

            if (worldIn.getBlockState(pos.offset(enumfacing2)).getBlock().isFullBlock()) {
                enumfacing2 = enumfacing2.getOpposite();
            }

            if (worldIn.getBlockState(pos.offset(enumfacing2)).getBlock().isFullBlock()) {
                enumfacing2 = enumfacing2.rotateY();
            }

            if (worldIn.getBlockState(pos.offset(enumfacing2)).getBlock().isFullBlock()) {
                enumfacing2 = enumfacing2.getOpposite();
            }

            return state.withProperty(FACING, enumfacing2);
        }
    }

    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        int i = 0;
        BlockPos blockpos = pos.west();
        BlockPos blockpos1 = pos.east();
        BlockPos blockpos2 = pos.north();
        BlockPos blockpos3 = pos.south();

        if (worldIn.getBlockState(blockpos).getBlock() == this) {
            if (this.isDoubleChest(worldIn, blockpos)) {
                return false;
            }

            ++i;
        }

        if (worldIn.getBlockState(blockpos1).getBlock() == this) {
            if (this.isDoubleChest(worldIn, blockpos1)) {
                return false;
            }

            ++i;
        }

        if (worldIn.getBlockState(blockpos2).getBlock() == this) {
            if (this.isDoubleChest(worldIn, blockpos2)) {
                return false;
            }

            ++i;
        }

        if (worldIn.getBlockState(blockpos3).getBlock() == this) {
            if (this.isDoubleChest(worldIn, blockpos3)) {
                return false;
            }

            ++i;
        }

        return i <= 1;
    }

    private boolean isDoubleChest(World worldIn, BlockPos pos) {
        if (worldIn.getBlockState(pos).getBlock() != this) {
            return false;
        } else {
            for (Direction enumfacing : Direction.Plane.HORIZONTAL) {
                if (worldIn.getBlockState(pos.offset(enumfacing)).getBlock() == this) {
                    return true;
                }
            }

            return false;
        }
    }

    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
        super.onNeighborBlockChange(worldIn, pos, state, neighborBlock);
        TileEntity tileentity = worldIn.getTileEntity(pos);

        if (tileentity instanceof TileEntityChest) {
            tileentity.updateContainingBlockInfo();
        }
    }

    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileentity = worldIn.getTileEntity(pos);

        if (tileentity instanceof IInventory iInventory) {
            InventoryHelper.dropInventoryItems(worldIn, pos, iInventory);
            worldIn.updateComparatorOutputLevel(pos, this);
        }

        super.breakBlock(worldIn, pos, state);
    }

    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, Direction side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            ILockableContainer ilockablecontainer = this.getLockableContainer(worldIn, pos);

            if (ilockablecontainer != null) {
                playerIn.displayGUIChest(ilockablecontainer);

                if (this.chestType == 0) {
                    playerIn.triggerAchievement(StatList.field_181723_aa);
                } else if (this.chestType == 1) {
                    playerIn.triggerAchievement(StatList.field_181737_U);
                }
            }

        }
        return true;
    }

    public ILockableContainer getLockableContainer(World worldIn, BlockPos pos) {
        TileEntity tileentity = worldIn.getTileEntity(pos);

        if (!(tileentity instanceof TileEntityChest tileEntityChest)) {
            return null;
        } else {
            ILockableContainer ilockablecontainer = tileEntityChest;

            if (this.isBlocked(worldIn, pos)) {
                return null;
            } else {
                for (Direction enumfacing : Direction.Plane.HORIZONTAL) {
                    BlockPos blockpos = pos.offset(enumfacing);
                    Block block = worldIn.getBlockState(blockpos).getBlock();

                    if (block == this) {
                        if (this.isBlocked(worldIn, blockpos)) {
                            return null;
                        }

                        TileEntity tileentity1 = worldIn.getTileEntity(blockpos);

                        if (tileentity1 instanceof TileEntityChest) {
                            if (enumfacing != Direction.WEST && enumfacing != Direction.NORTH) {
                                ilockablecontainer = new InventoryLargeChest("container.chestDouble", ilockablecontainer, (TileEntityChest) tileentity1);
                            } else {
                                ilockablecontainer = new InventoryLargeChest("container.chestDouble", (TileEntityChest) tileentity1, ilockablecontainer);
                            }
                        }
                    }
                }

                return ilockablecontainer;
            }
        }
    }

    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityChest();
    }

    public boolean canProvidePower() {
        return this.chestType == 1;
    }

    public int getWeakPower(IBlockAccess worldIn, BlockPos pos, IBlockState state, Direction side) {
        if (!this.canProvidePower()) {
            return 0;
        } else {
            int i = 0;
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity instanceof TileEntityChest tileEntityChest) {
                i = tileEntityChest.numPlayersUsing;
            }

            return MathHelper.clamp_int(i, 0, 15);
        }
    }

    public int getStrongPower(IBlockAccess worldIn, BlockPos pos, IBlockState state, Direction side) {
        return side == Direction.UP ? this.getWeakPower(worldIn, pos, state, side) : 0;
    }

    private boolean isBlocked(World worldIn, BlockPos pos) {
        return this.isBelowSolidBlock(worldIn, pos) || this.isOcelotSittingOnChest(worldIn, pos);
    }

    private boolean isBelowSolidBlock(World worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos.up()).getBlock().isNormalCube();
    }

    private boolean isOcelotSittingOnChest(World worldIn, BlockPos pos) {
        for (EntityOcelot entity : worldIn.getEntitiesWithinAABB(EntityOcelot.class, new AxisAlignedBB(pos.getX(), (pos.getY() + 1), pos.getZ(), (pos.getX() + 1), (pos.getY() + 2), (pos.getZ() + 1)))) {
            if (entity.isSitting()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasComparatorInputOverride() {
        return true;
    }

    public int getComparatorInputOverride(World worldIn, BlockPos pos) {
        return Container.calcRedstoneFromInventory(this.getLockableContainer(worldIn, pos));
    }

    public IBlockState getStateFromMeta(int meta) {
        Direction enumfacing = Direction.getFront(meta);

        if (enumfacing.getAxis() == Direction.Axis.Y) {
            enumfacing = Direction.NORTH;
        }

        return this.getDefaultState().withProperty(FACING, enumfacing);
    }

    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    protected BlockState createBlockState() {
        return new BlockState(this, FACING);
    }
}
