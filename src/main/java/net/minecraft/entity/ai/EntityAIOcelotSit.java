package net.minecraft.entity.ai;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class EntityAIOcelotSit extends EntityAIMoveToBlock {
    private final EntityOcelot ocelot;

    public EntityAIOcelotSit(EntityOcelot ocelotIn, double p_i45315_2_) {
        super(ocelotIn, p_i45315_2_, 8);
        this.ocelot = ocelotIn;
    }

    @Override
    public boolean shouldExecute() {
        return this.ocelot.isTamed() && !this.ocelot.isSitting() && super.shouldExecute();
    }

    @Override
    public boolean continueExecuting() {
        return super.continueExecuting();
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        this.ocelot.getAISit().setSitting(false);
    }

    @Override
    public void resetTask() {
        super.resetTask();
        this.ocelot.setSitting(false);
    }

    @Override
    public void updateTask() {
        super.updateTask();
        this.ocelot.getAISit().setSitting(false);

        if (!this.getIsAboveDestination()) {
            this.ocelot.setSitting(false);
        } else if (!this.ocelot.isSitting()) {
            this.ocelot.setSitting(true);
        }
    }

    @Override
    protected boolean shouldMoveTo(World worldIn, BlockPos pos) {
        if (!worldIn.isAirBlock(pos.up())) {
            return false;
        } else {
            IBlockState iblockstate = worldIn.getBlockState(pos);
            Block block = iblockstate.getBlock();

            if (block == Blocks.CHEST) {
                TileEntity tileentity = worldIn.getTileEntity(pos);

                return tileentity instanceof TileEntityChest tileEntityChest && tileEntityChest.numPlayersUsing < 1;
            } else {
                if (block == Blocks.LIT_FURNACE) {
                    return true;
                }

                return block == Blocks.BED && iblockstate.getValue(BlockBed.PART) != BlockBed.EnumPartType.HEAD;
            }
        }
    }
}
