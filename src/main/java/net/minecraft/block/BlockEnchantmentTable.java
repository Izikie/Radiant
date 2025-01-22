package net.minecraft.block;

import java.util.Random;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEnchantmentTable;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.ParticleTypes;
import net.minecraft.world.World;

public class BlockEnchantmentTable extends BlockContainer {
    protected BlockEnchantmentTable() {
        super(Material.ROCK, MapColor.RED_COLOR);
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.75F, 1.0F);
        this.setLightOpacity(0);
        this.setCreativeTab(CreativeTabs.tabDecorations);
    }

    public boolean isFullCube() {
        return false;
    }

    public void randomDisplayTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        super.randomDisplayTick(worldIn, pos, state, rand);

        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                if (i > -2 && i < 2 && j == -1) {
                    j = 2;
                }

                if (rand.nextInt(16) == 0) {
                    for (int k = 0; k <= 1; ++k) {
                        BlockPos blockpos = pos.add(i, k, j);

                        if (worldIn.getBlockState(blockpos).getBlock() == Blocks.BOOKSHELF) {
                            if (!worldIn.isAirBlock(pos.add(i / 2, 0, j / 2))) {
                                break;
                            }

                            worldIn.spawnParticle(ParticleTypes.ENCHANTMENT_TABLE, pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D, (i + rand.nextFloat()) - 0.5D, (k - rand.nextFloat() - 1.0F), (j + rand.nextFloat()) - 0.5D);
                        }
                    }
                }
            }
        }
    }

    public boolean isOpaqueCube() {
        return false;
    }

    public int getRenderType() {
        return 3;
    }

    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityEnchantmentTable();
    }

    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, Direction side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity instanceof TileEntityEnchantmentTable tileEntityEnchantmentTable) {
                playerIn.displayGui(tileEntityEnchantmentTable);
            }

        }
        return true;
    }

    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        if (stack.hasDisplayName()) {
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity instanceof TileEntityEnchantmentTable tileEntityEnchantmentTable) {
                tileEntityEnchantmentTable.setCustomName(stack.getDisplayName());
            }
        }
    }
}
