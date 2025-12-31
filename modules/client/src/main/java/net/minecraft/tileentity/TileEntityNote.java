package net.minecraft.tileentity;

import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class TileEntityNote extends TileEntity {
    public byte note;
    public boolean previousRedstoneState;

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setByte("note", this.note);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.note = compound.getByte("note");
        this.note = (byte) Math.clamp(this.note, 0, 24);
    }

    public void changePitch() {
        this.note = (byte) ((this.note + 1) % 25);
        this.markDirty();
    }

    public void triggerNote(World worldIn, BlockPos p_175108_2_) {
        if (worldIn.getBlockState(p_175108_2_.up()).getBlock().getMaterial() == Material.AIR) {
            Material material = worldIn.getBlockState(p_175108_2_.down()).getBlock().getMaterial();
            int i = 0;

            if (material == Material.ROCK) {
                i = 1;
            }

            if (material == Material.SAND) {
                i = 2;
            }

            if (material == Material.GLASS) {
                i = 3;
            }

            if (material == Material.WOOD) {
                i = 4;
            }

            worldIn.addBlockEvent(p_175108_2_, Blocks.NOTEBLOCK, i, this.note);
        }
    }
}
