package net.minecraft.block.dispenser;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;

public interface IBlockSource extends ILocatableSource {
    @Override
    double x();

    @Override
    double y();

    @Override
    double z();

    BlockPos getBlockPos();

    int getBlockMetadata();

    <T extends TileEntity> T getBlockTileEntity();
}
