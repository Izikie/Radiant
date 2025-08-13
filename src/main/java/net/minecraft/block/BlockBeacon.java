package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.RenderLayer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

public class BlockBeacon extends BlockContainer {
    public BlockBeacon() {
        super(Material.GLASS, MapColor.DIAMOND_COLOR);
        this.setHardness(3.0F);
        this.setCreativeTab(CreativeTabs.TAB_MISC);
    }

    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityBeacon();
    }

    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, Direction side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity instanceof TileEntityBeacon tileEntityBeacon) {
                playerIn.displayGUIChest(tileEntityBeacon);
                playerIn.triggerAchievement(StatList.field_181730_N);
            }

        }
        return true;
    }

    public boolean isOpaqueCube() {
        return false;
    }

    public boolean isFullCube() {
        return false;
    }

    public int getRenderType() {
        return 3;
    }

    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        if (stack.hasDisplayName()) {
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity instanceof TileEntityBeacon tileEntityBeacon) {
                tileEntityBeacon.setName(stack.getDisplayName());
            }
        }
    }

    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
        TileEntity tileentity = worldIn.getTileEntity(pos);

        if (tileentity instanceof TileEntityBeacon tileEntityBeacon) {
            tileEntityBeacon.updateBeacon();
            worldIn.addBlockEvent(pos, this, 1, 0);
        }
    }

    public RenderLayer getBlockLayer() {
        return RenderLayer.CUTOUT;
    }

    public static void updateColorAsync(final World worldIn, final BlockPos glassPos) {
        HttpUtil.DOWNLOAD_EXECUTOR.submit(() -> {
            Chunk chunk = worldIn.getChunkFromBlockCoords(glassPos);

            for (int i = glassPos.getY() - 1; i >= 0; --i) {
                final BlockPos blockpos = new BlockPos(glassPos.getX(), i, glassPos.getZ());

                if (!chunk.canSeeSky(blockpos)) {
                    break;
                }

                IBlockState iblockstate = worldIn.getBlockState(blockpos);

                if (iblockstate.getBlock() == Blocks.BEACON) {
                    ((WorldServer) worldIn).addScheduledTask(() -> {
                        TileEntity tileentity = worldIn.getTileEntity(blockpos);

                        if (tileentity instanceof TileEntityBeacon tileEntityBeacon) {
                            tileEntityBeacon.updateBeacon();
                            worldIn.addBlockEvent(blockpos, Blocks.BEACON, 1, 0);
                        }
                    });
                }
            }
        });
    }
}
