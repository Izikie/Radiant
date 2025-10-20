package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.ParticleTypes;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class BlockBrewingStand extends BlockContainer {
    public static final PropertyBool[] HAS_BOTTLE = new PropertyBool[]{PropertyBool.create("has_bottle_0"), PropertyBool.create("has_bottle_1"), PropertyBool.create("has_bottle_2")};

    public BlockBrewingStand() {
        super(Material.IRON);
        this.setDefaultState(this.blockState.getBaseState().withProperty(HAS_BOTTLE[0], Boolean.FALSE).withProperty(HAS_BOTTLE[1], Boolean.FALSE).withProperty(HAS_BOTTLE[2], Boolean.FALSE));
    }

    @Override
    public String getLocalizedName() {
        return StatCollector.translateToLocal("item.brewingStand.name");
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 3;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityBrewingStand();
    }

    @Override
    public boolean isFullCube() {
        return false;
    }

    @Override
    public void addCollisionBoxesToList(World worldIn, BlockPos pos, IBlockState state, AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collidingEntity) {
        this.setBlockBounds(0.4375F, 0.0F, 0.4375F, 0.5625F, 0.875F, 0.5625F);
        super.addCollisionBoxesToList(worldIn, pos, state, mask, list, collidingEntity);
        this.setBlockBoundsForItemRender();
        super.addCollisionBoxesToList(worldIn, pos, state, mask, list, collidingEntity);
    }

    @Override
    public void setBlockBoundsForItemRender() {
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, Direction side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity instanceof TileEntityBrewingStand tileEntityBrewingStand) {
                playerIn.displayGUIChest(tileEntityBrewingStand);
                playerIn.triggerAchievement(StatList.field_181729_M);
            }

        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (stack.hasDisplayName()) {
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity instanceof TileEntityBrewingStand tileEntityBrewingStand) {
                tileEntityBrewingStand.setName(stack.getDisplayName());
            }
        }
    }

    @Override
    public void randomDisplayTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        double d0 = (pos.getX() + 0.4F + rand.nextFloat() * 0.2F);
        double d1 = (pos.getY() + 0.7F + rand.nextFloat() * 0.3F);
        double d2 = (pos.getZ() + 0.4F + rand.nextFloat() * 0.2F);
        worldIn.spawnParticle(ParticleTypes.SMOKE_NORMAL, d0, d1, d2, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileentity = worldIn.getTileEntity(pos);

        if (tileentity instanceof TileEntityBrewingStand tileEntityBrewingStand) {
            InventoryHelper.dropInventoryItems(worldIn, pos, tileEntityBrewingStand);
        }

        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.BREWING_STAND;
    }

    @Override
    public Item getItem(World worldIn, BlockPos pos) {
        return Items.BREWING_STAND;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World worldIn, BlockPos pos) {
        return Container.calcRedstone(worldIn.getTileEntity(pos));
    }

    @Override
    public RenderLayer getBlockLayer() {
        return RenderLayer.CUTOUT;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        IBlockState iblockstate = this.getDefaultState();

        for (int i = 0; i < 3; ++i) {
            iblockstate = iblockstate.withProperty(HAS_BOTTLE[i], (meta & 1 << i) > 0);
        }

        return iblockstate;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = 0;

        for (int j = 0; j < 3; ++j) {
            if (state.getValue(HAS_BOTTLE[j])) {
                i |= 1 << j;
            }
        }

        return i;
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, HAS_BOTTLE[0], HAS_BOTTLE[1], HAS_BOTTLE[2]);
    }
}
