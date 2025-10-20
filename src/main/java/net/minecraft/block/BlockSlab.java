package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public abstract class BlockSlab extends Block {
    public static final PropertyEnum<EnumBlockHalf> HALF = PropertyEnum.create("half", EnumBlockHalf.class);

    public BlockSlab(Material materialIn) {
        super(materialIn);

        if (this.isDouble()) {
            this.fullBlock = true;
        } else {
            this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
        }

        this.setLightOpacity(255);
    }

    @Override
    protected boolean canSilkHarvest() {
        return false;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess worldIn, BlockPos pos) {
        if (this.isDouble()) {
            this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        } else {
            IBlockState iblockstate = worldIn.getBlockState(pos);

            if (iblockstate.getBlock() == this) {
                if (iblockstate.getValue(HALF) == EnumBlockHalf.TOP) {
                    this.setBlockBounds(0.0F, 0.5F, 0.0F, 1.0F, 1.0F, 1.0F);
                } else {
                    this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
                }
            }
        }
    }

    @Override
    public void setBlockBoundsForItemRender() {
        if (this.isDouble()) {
            this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        } else {
            this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
        }
    }

    @Override
    public void addCollisionBoxesToList(World worldIn, BlockPos pos, IBlockState state, AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collidingEntity) {
        this.setBlockBoundsBasedOnState(worldIn, pos);
        super.addCollisionBoxesToList(worldIn, pos, state, mask, list, collidingEntity);
    }

    @Override
    public boolean isOpaqueCube() {
        return this.isDouble();
    }

    @Override
    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        IBlockState iblockstate = super.onBlockPlaced(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer).withProperty(HALF, EnumBlockHalf.BOTTOM);
        return this.isDouble() ? iblockstate : (facing != Direction.DOWN && (facing == Direction.UP || hitY <= 0.5D) ? iblockstate : iblockstate.withProperty(HALF, EnumBlockHalf.TOP));
    }

    @Override
    public int quantityDropped(Random random) {
        return this.isDouble() ? 2 : 1;
    }

    @Override
    public boolean isFullCube() {
        return this.isDouble();
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess worldIn, BlockPos pos, Direction side) {
        if (this.isDouble()) {
            return super.shouldSideBeRendered(worldIn, pos, side);
        } else if (side != Direction.UP && side != Direction.DOWN && !super.shouldSideBeRendered(worldIn, pos, side)) {
            return false;
        } else {
            BlockPos blockpos = pos.offset(side.getOpposite());
            IBlockState iblockstate = worldIn.getBlockState(pos);
            IBlockState iblockstate1 = worldIn.getBlockState(blockpos);
            boolean flag = isSlab(iblockstate.getBlock()) && iblockstate.getValue(HALF) == EnumBlockHalf.TOP;
            boolean flag1 = isSlab(iblockstate1.getBlock()) && iblockstate1.getValue(HALF) == EnumBlockHalf.TOP;
            return flag1 ? (side == Direction.DOWN || (side == Direction.UP && super.shouldSideBeRendered(worldIn, pos, side) || !isSlab(iblockstate.getBlock()) || !flag)) : (side == Direction.UP || (side == Direction.DOWN && super.shouldSideBeRendered(worldIn, pos, side) || !isSlab(iblockstate.getBlock()) || flag));
        }
    }

    protected static boolean isSlab(Block blockIn) {
        return blockIn == Blocks.STONE_SLAB || blockIn == Blocks.WOODEN_SLAB || blockIn == Blocks.BLOCK_SLAB;
    }

    public abstract String getUnlocalizedName(int meta);

    @Override
    public int getDamageValue(World worldIn, BlockPos pos) {
        return super.getDamageValue(worldIn, pos) & 7;
    }

    public abstract boolean isDouble();

    public abstract IProperty<?> getVariantProperty();

    public abstract Object getVariant(ItemStack stack);

    public enum EnumBlockHalf implements IStringSerializable {
        TOP("top"),
        BOTTOM("bottom");

        private final String name;

        EnumBlockHalf(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }

        @Override
        public String getName() {
            return this.name;
        }
    }
}
