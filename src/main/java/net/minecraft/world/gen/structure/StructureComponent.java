package net.minecraft.world.gen.structure;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemDoor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.World;

public abstract class StructureComponent {
    protected StructureBoundingBox boundingBox;
    protected Direction coordBaseMode;
    protected int componentType;

    public StructureComponent() {
    }

    protected StructureComponent(int type) {
        this.componentType = type;
    }

    public NBTTagCompound createStructureBaseNBT() {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        nbttagcompound.setString("id", MapGenStructureIO.getStructureComponentName(this));
        nbttagcompound.setTag("BB", this.boundingBox.toNBTTagIntArray());
        nbttagcompound.setInteger("O", this.coordBaseMode == null ? -1 : this.coordBaseMode.getHorizontalIndex());
        nbttagcompound.setInteger("GD", this.componentType);
        this.writeStructureToNBT(nbttagcompound);
        return nbttagcompound;
    }

    protected abstract void writeStructureToNBT(NBTTagCompound tagCompound);

    public void readStructureBaseNBT(World worldIn, NBTTagCompound tagCompound) {
        if (tagCompound.hasKey("BB")) {
            this.boundingBox = new StructureBoundingBox(tagCompound.getIntArray("BB"));
        }

        int i = tagCompound.getInteger("O");
        this.coordBaseMode = i == -1 ? null : Direction.getHorizontal(i);
        this.componentType = tagCompound.getInteger("GD");
        this.readStructureFromNBT(tagCompound);
    }

    protected abstract void readStructureFromNBT(NBTTagCompound tagCompound);

    public void buildComponent(StructureComponent componentIn, List<StructureComponent> listIn, Random rand) {
    }

    public abstract boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox structureBoundingBoxIn);

    public StructureBoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    public int getComponentType() {
        return this.componentType;
    }

    public static StructureComponent findIntersecting(List<StructureComponent> listIn, StructureBoundingBox boundingboxIn) {
        for (StructureComponent structurecomponent : listIn) {
            if (structurecomponent.getBoundingBox() != null && structurecomponent.getBoundingBox().intersectsWith(boundingboxIn)) {
                return structurecomponent;
            }
        }

        return null;
    }

    public BlockPos getBoundingBoxCenter() {
        return new BlockPos(this.boundingBox.getCenter());
    }

    protected boolean isLiquidInStructureBoundingBox(World worldIn, StructureBoundingBox boundingboxIn) {
        int i = Math.max(this.boundingBox.minX - 1, boundingboxIn.minX);
        int j = Math.max(this.boundingBox.minY - 1, boundingboxIn.minY);
        int k = Math.max(this.boundingBox.minZ - 1, boundingboxIn.minZ);
        int l = Math.min(this.boundingBox.maxX + 1, boundingboxIn.maxX);
        int i1 = Math.min(this.boundingBox.maxY + 1, boundingboxIn.maxY);
        int j1 = Math.min(this.boundingBox.maxZ + 1, boundingboxIn.maxZ);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int k1 = i; k1 <= l; ++k1) {
            for (int l1 = k; l1 <= j1; ++l1) {
                if (worldIn.getBlockState(blockpos$mutableblockpos.set(k1, j, l1)).getBlock().getMaterial().isLiquid()) {
                    return true;
                }

                if (worldIn.getBlockState(blockpos$mutableblockpos.set(k1, i1, l1)).getBlock().getMaterial().isLiquid()) {
                    return true;
                }
            }
        }

        for (int i2 = i; i2 <= l; ++i2) {
            for (int k2 = j; k2 <= i1; ++k2) {
                if (worldIn.getBlockState(blockpos$mutableblockpos.set(i2, k2, k)).getBlock().getMaterial().isLiquid()) {
                    return true;
                }

                if (worldIn.getBlockState(blockpos$mutableblockpos.set(i2, k2, j1)).getBlock().getMaterial().isLiquid()) {
                    return true;
                }
            }
        }

        for (int j2 = k; j2 <= j1; ++j2) {
            for (int l2 = j; l2 <= i1; ++l2) {
                if (worldIn.getBlockState(blockpos$mutableblockpos.set(i, l2, j2)).getBlock().getMaterial().isLiquid()) {
                    return true;
                }

                if (worldIn.getBlockState(blockpos$mutableblockpos.set(l, l2, j2)).getBlock().getMaterial().isLiquid()) {
                    return true;
                }
            }
        }

        return false;
    }

    protected int getXWithOffset(int x, int z) {
        if (this.coordBaseMode == null) {
            return x;
        } else {
            return switch (this.coordBaseMode) {
                case NORTH, SOUTH -> this.boundingBox.minX + x;
                case WEST -> this.boundingBox.maxX - z;
                case EAST -> this.boundingBox.minX + z;
                default -> x;
            };
        }
    }

    protected int getYWithOffset(int y) {
        return this.coordBaseMode == null ? y : y + this.boundingBox.minY;
    }

    protected int getZWithOffset(int x, int z) {
        if (this.coordBaseMode == null) {
            return z;
        } else {
            return switch (this.coordBaseMode) {
                case NORTH -> this.boundingBox.maxZ - z;
                case SOUTH -> this.boundingBox.minZ + z;
                case WEST, EAST -> this.boundingBox.minZ + x;
                default -> z;
            };
        }
    }

    protected int getMetadataWithOffset(Block blockIn, int meta) {
        if (blockIn == Blocks.RAIL) {
            if (this.coordBaseMode == Direction.WEST || this.coordBaseMode == Direction.EAST) {
                if (meta == 1) {
                    return 0;
                }

                return 1;
            }
        } else if (blockIn instanceof BlockDoor) {
            if (this.coordBaseMode == Direction.SOUTH) {
                if (meta == 0) {
                    return 2;
                }

                if (meta == 2) {
                    return 0;
                }
            } else {
                if (this.coordBaseMode == Direction.WEST) {
                    return meta + 1 & 3;
                }

                if (this.coordBaseMode == Direction.EAST) {
                    return meta + 3 & 3;
                }
            }
        } else if (blockIn != Blocks.STONE_STAIRS && blockIn != Blocks.OAK_STAIRS && blockIn != Blocks.NETHER_BRICK_STAIRS && blockIn != Blocks.STONE_BRICK_STAIRS && blockIn != Blocks.SANDSTONE_STAIRS) {
            if (blockIn == Blocks.LADDER) {
                if (this.coordBaseMode == Direction.SOUTH) {
                    if (meta == Direction.NORTH.getIndex()) {
                        return Direction.SOUTH.getIndex();
                    }

                    if (meta == Direction.SOUTH.getIndex()) {
                        return Direction.NORTH.getIndex();
                    }
                } else if (this.coordBaseMode == Direction.WEST) {
                    if (meta == Direction.NORTH.getIndex()) {
                        return Direction.WEST.getIndex();
                    }

                    if (meta == Direction.SOUTH.getIndex()) {
                        return Direction.EAST.getIndex();
                    }

                    if (meta == Direction.WEST.getIndex()) {
                        return Direction.NORTH.getIndex();
                    }

                    if (meta == Direction.EAST.getIndex()) {
                        return Direction.SOUTH.getIndex();
                    }
                } else if (this.coordBaseMode == Direction.EAST) {
                    if (meta == Direction.NORTH.getIndex()) {
                        return Direction.EAST.getIndex();
                    }

                    if (meta == Direction.SOUTH.getIndex()) {
                        return Direction.WEST.getIndex();
                    }

                    if (meta == Direction.WEST.getIndex()) {
                        return Direction.NORTH.getIndex();
                    }

                    if (meta == Direction.EAST.getIndex()) {
                        return Direction.SOUTH.getIndex();
                    }
                }
            } else if (blockIn == Blocks.STONE_BUTTON) {
                if (this.coordBaseMode == Direction.SOUTH) {
                    if (meta == 3) {
                        return 4;
                    }

                    if (meta == 4) {
                        return 3;
                    }
                } else if (this.coordBaseMode == Direction.WEST) {
                    if (meta == 3) {
                        return 1;
                    }

                    if (meta == 4) {
                        return 2;
                    }

                    if (meta == 2) {
                        return 3;
                    }

                    if (meta == 1) {
                        return 4;
                    }
                } else if (this.coordBaseMode == Direction.EAST) {
                    if (meta == 3) {
                        return 2;
                    }

                    if (meta == 4) {
                        return 1;
                    }

                    if (meta == 2) {
                        return 3;
                    }

                    if (meta == 1) {
                        return 4;
                    }
                }
            } else if (blockIn != Blocks.TRIPWIRE_HOOK && !(blockIn instanceof BlockDirectional)) {
                if (blockIn == Blocks.PISTON || blockIn == Blocks.STICKY_PISTON || blockIn == Blocks.LEVER || blockIn == Blocks.DISPENSER) {
                    if (this.coordBaseMode == Direction.SOUTH) {
                        if (meta == Direction.NORTH.getIndex() || meta == Direction.SOUTH.getIndex()) {
                            return Direction.getFront(meta).getOpposite().getIndex();
                        }
                    } else if (this.coordBaseMode == Direction.WEST) {
                        if (meta == Direction.NORTH.getIndex()) {
                            return Direction.WEST.getIndex();
                        }

                        if (meta == Direction.SOUTH.getIndex()) {
                            return Direction.EAST.getIndex();
                        }

                        if (meta == Direction.WEST.getIndex()) {
                            return Direction.NORTH.getIndex();
                        }

                        if (meta == Direction.EAST.getIndex()) {
                            return Direction.SOUTH.getIndex();
                        }
                    } else if (this.coordBaseMode == Direction.EAST) {
                        if (meta == Direction.NORTH.getIndex()) {
                            return Direction.EAST.getIndex();
                        }

                        if (meta == Direction.SOUTH.getIndex()) {
                            return Direction.WEST.getIndex();
                        }

                        if (meta == Direction.WEST.getIndex()) {
                            return Direction.NORTH.getIndex();
                        }

                        if (meta == Direction.EAST.getIndex()) {
                            return Direction.SOUTH.getIndex();
                        }
                    }
                }
            } else {
                Direction enumfacing = Direction.getHorizontal(meta);

                if (this.coordBaseMode == Direction.SOUTH) {
                    if (enumfacing == Direction.SOUTH || enumfacing == Direction.NORTH) {
                        return enumfacing.getOpposite().getHorizontalIndex();
                    }
                } else if (this.coordBaseMode == Direction.WEST) {
                    if (enumfacing == Direction.NORTH) {
                        return Direction.WEST.getHorizontalIndex();
                    }

                    if (enumfacing == Direction.SOUTH) {
                        return Direction.EAST.getHorizontalIndex();
                    }

                    if (enumfacing == Direction.WEST) {
                        return Direction.NORTH.getHorizontalIndex();
                    }

                    if (enumfacing == Direction.EAST) {
                        return Direction.SOUTH.getHorizontalIndex();
                    }
                } else if (this.coordBaseMode == Direction.EAST) {
                    if (enumfacing == Direction.NORTH) {
                        return Direction.EAST.getHorizontalIndex();
                    }

                    if (enumfacing == Direction.SOUTH) {
                        return Direction.WEST.getHorizontalIndex();
                    }

                    if (enumfacing == Direction.WEST) {
                        return Direction.NORTH.getHorizontalIndex();
                    }

                    if (enumfacing == Direction.EAST) {
                        return Direction.SOUTH.getHorizontalIndex();
                    }
                }
            }
        } else if (this.coordBaseMode == Direction.SOUTH) {
            if (meta == 2) {
                return 3;
            }

            if (meta == 3) {
                return 2;
            }
        } else if (this.coordBaseMode == Direction.WEST) {
            if (meta == 0) {
                return 2;
            }

            if (meta == 1) {
                return 3;
            }

            if (meta == 2) {
                return 0;
            }

            if (meta == 3) {
                return 1;
            }
        } else if (this.coordBaseMode == Direction.EAST) {
            if (meta == 0) {
                return 2;
            }

            if (meta == 1) {
                return 3;
            }

            if (meta == 2) {
                return 1;
            }

            if (meta == 3) {
                return 0;
            }
        }

        return meta;
    }

    protected void setBlockState(World worldIn, IBlockState blockstateIn, int x, int y, int z, StructureBoundingBox boundingboxIn) {
        BlockPos blockpos = new BlockPos(this.getXWithOffset(x, z), this.getYWithOffset(y), this.getZWithOffset(x, z));

        if (boundingboxIn.isVecInside(blockpos)) {
            worldIn.setBlockState(blockpos, blockstateIn, 2);
        }
    }

    protected IBlockState getBlockStateFromPos(World worldIn, int x, int y, int z, StructureBoundingBox boundingboxIn) {
        int i = this.getXWithOffset(x, z);
        int j = this.getYWithOffset(y);
        int k = this.getZWithOffset(x, z);
        BlockPos blockpos = new BlockPos(i, j, k);
        return !boundingboxIn.isVecInside(blockpos) ? Blocks.AIR.getDefaultState() : worldIn.getBlockState(blockpos);
    }

    protected void fillWithAir(World worldIn, StructureBoundingBox structurebb, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (int i = minY; i <= maxY; ++i) {
            for (int j = minX; j <= maxX; ++j) {
                for (int k = minZ; k <= maxZ; ++k) {
                    this.setBlockState(worldIn, Blocks.AIR.getDefaultState(), j, i, k, structurebb);
                }
            }
        }
    }

    protected void fillWithBlocks(World worldIn, StructureBoundingBox boundingboxIn, int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, IBlockState boundaryBlockState, IBlockState insideBlockState, boolean existingOnly) {
        for (int i = yMin; i <= yMax; ++i) {
            for (int j = xMin; j <= xMax; ++j) {
                for (int k = zMin; k <= zMax; ++k) {
                    if (!existingOnly || this.getBlockStateFromPos(worldIn, j, i, k, boundingboxIn).getBlock().getMaterial() != Material.AIR) {
                        if (i != yMin && i != yMax && j != xMin && j != xMax && k != zMin && k != zMax) {
                            this.setBlockState(worldIn, insideBlockState, j, i, k, boundingboxIn);
                        } else {
                            this.setBlockState(worldIn, boundaryBlockState, j, i, k, boundingboxIn);
                        }
                    }
                }
            }
        }
    }

    protected void fillWithRandomizedBlocks(World worldIn, StructureBoundingBox boundingboxIn, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean alwaysReplace, Random rand, StructureComponent.BlockSelector blockselector) {
        for (int i = minY; i <= maxY; ++i) {
            for (int j = minX; j <= maxX; ++j) {
                for (int k = minZ; k <= maxZ; ++k) {
                    if (!alwaysReplace || this.getBlockStateFromPos(worldIn, j, i, k, boundingboxIn).getBlock().getMaterial() != Material.AIR) {
                        blockselector.selectBlocks(rand, j, i, k, i == minY || i == maxY || j == minX || j == maxX || k == minZ || k == maxZ);
                        this.setBlockState(worldIn, blockselector.getBlockState(), j, i, k, boundingboxIn);
                    }
                }
            }
        }
    }

    protected void func_175805_a(World worldIn, StructureBoundingBox boundingboxIn, Random rand, float chance, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, IBlockState blockstate1, IBlockState blockstate2, boolean p_175805_13_) {
        for (int i = minY; i <= maxY; ++i) {
            for (int j = minX; j <= maxX; ++j) {
                for (int k = minZ; k <= maxZ; ++k) {
                    if (rand.nextFloat() <= chance && (!p_175805_13_ || this.getBlockStateFromPos(worldIn, j, i, k, boundingboxIn).getBlock().getMaterial() != Material.AIR)) {
                        if (i != minY && i != maxY && j != minX && j != maxX && k != minZ && k != maxZ) {
                            this.setBlockState(worldIn, blockstate2, j, i, k, boundingboxIn);
                        } else {
                            this.setBlockState(worldIn, blockstate1, j, i, k, boundingboxIn);
                        }
                    }
                }
            }
        }
    }

    protected void randomlyPlaceBlock(World worldIn, StructureBoundingBox boundingboxIn, Random rand, float chance, int x, int y, int z, IBlockState blockstateIn) {
        if (rand.nextFloat() < chance) {
            this.setBlockState(worldIn, blockstateIn, x, y, z, boundingboxIn);
        }
    }

    protected void randomlyRareFillWithBlocks(World worldIn, StructureBoundingBox boundingboxIn, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, IBlockState blockstateIn, boolean p_180777_10_) {
        float f = (maxX - minX + 1);
        float f1 = (maxY - minY + 1);
        float f2 = (maxZ - minZ + 1);
        float f3 = minX + f / 2.0F;
        float f4 = minZ + f2 / 2.0F;

        for (int i = minY; i <= maxY; ++i) {
            float f5 = (i - minY) / f1;

            for (int j = minX; j <= maxX; ++j) {
                float f6 = (j - f3) / (f * 0.5F);

                for (int k = minZ; k <= maxZ; ++k) {
                    float f7 = (k - f4) / (f2 * 0.5F);

                    if (!p_180777_10_ || this.getBlockStateFromPos(worldIn, j, i, k, boundingboxIn).getBlock().getMaterial() != Material.AIR) {
                        float f8 = f6 * f6 + f5 * f5 + f7 * f7;

                        if (f8 <= 1.05F) {
                            this.setBlockState(worldIn, blockstateIn, j, i, k, boundingboxIn);
                        }
                    }
                }
            }
        }
    }

    protected void clearCurrentPositionBlocksUpwards(World worldIn, int x, int y, int z, StructureBoundingBox structurebb) {
        BlockPos blockpos = new BlockPos(this.getXWithOffset(x, z), this.getYWithOffset(y), this.getZWithOffset(x, z));

        if (structurebb.isVecInside(blockpos)) {
            while (!worldIn.isAirBlock(blockpos) && blockpos.getY() < 255) {
                worldIn.setBlockState(blockpos, Blocks.AIR.getDefaultState(), 2);
                blockpos = blockpos.up();
            }
        }
    }

    protected void replaceAirAndLiquidDownwards(World worldIn, IBlockState blockstateIn, int x, int y, int z, StructureBoundingBox boundingboxIn) {
        int i = this.getXWithOffset(x, z);
        int j = this.getYWithOffset(y);
        int k = this.getZWithOffset(x, z);

        if (boundingboxIn.isVecInside(new BlockPos(i, j, k))) {
            while ((worldIn.isAirBlock(new BlockPos(i, j, k)) || worldIn.getBlockState(new BlockPos(i, j, k)).getBlock().getMaterial().isLiquid()) && j > 1) {
                worldIn.setBlockState(new BlockPos(i, j, k), blockstateIn, 2);
                --j;
            }
        }
    }

    protected boolean generateChestContents(World worldIn, StructureBoundingBox boundingBoxIn, Random rand, int x, int y, int z, List<WeightedRandomChestContent> listIn, int max) {
        BlockPos blockpos = new BlockPos(this.getXWithOffset(x, z), this.getYWithOffset(y), this.getZWithOffset(x, z));

        if (boundingBoxIn.isVecInside(blockpos) && worldIn.getBlockState(blockpos).getBlock() != Blocks.CHEST) {
            IBlockState iblockstate = Blocks.CHEST.getDefaultState();
            worldIn.setBlockState(blockpos, Blocks.CHEST.correctFacing(worldIn, blockpos, iblockstate), 2);
            TileEntity tileentity = worldIn.getTileEntity(blockpos);

            if (tileentity instanceof TileEntityChest tileEntityChest) {
                WeightedRandomChestContent.generateChestContents(rand, listIn, tileEntityChest, max);
            }

            return true;
        } else {
            return false;
        }
    }

    protected boolean generateDispenserContents(World worldIn, StructureBoundingBox boundingBoxIn, Random rand, int x, int y, int z, int meta, List<WeightedRandomChestContent> listIn, int max) {
        BlockPos blockpos = new BlockPos(this.getXWithOffset(x, z), this.getYWithOffset(y), this.getZWithOffset(x, z));

        if (boundingBoxIn.isVecInside(blockpos) && worldIn.getBlockState(blockpos).getBlock() != Blocks.DISPENSER) {
            worldIn.setBlockState(blockpos, Blocks.DISPENSER.getStateFromMeta(this.getMetadataWithOffset(Blocks.DISPENSER, meta)), 2);
            TileEntity tileentity = worldIn.getTileEntity(blockpos);

            if (tileentity instanceof TileEntityDispenser tileEntityDispenser) {
                WeightedRandomChestContent.generateDispenserContents(rand, listIn, tileEntityDispenser, max);
            }

            return true;
        } else {
            return false;
        }
    }

    protected void placeDoorCurrentPosition(World worldIn, StructureBoundingBox boundingBoxIn, Random rand, int x, int y, int z, Direction facing) {
        BlockPos blockpos = new BlockPos(this.getXWithOffset(x, z), this.getYWithOffset(y), this.getZWithOffset(x, z));

        if (boundingBoxIn.isVecInside(blockpos)) {
            ItemDoor.placeDoor(worldIn, blockpos, facing.rotateYCCW(), Blocks.OAK_DOOR);
        }
    }

    public void func_181138_a(int p_181138_1_, int p_181138_2_, int p_181138_3_) {
        this.boundingBox.offset(p_181138_1_, p_181138_2_, p_181138_3_);
    }

    public abstract static class BlockSelector {
        protected IBlockState blockstate = Blocks.AIR.getDefaultState();

        public abstract void selectBlocks(Random rand, int x, int y, int z, boolean p_75062_5_);

        public IBlockState getBlockState() {
            return this.blockstate;
        }
    }
}
