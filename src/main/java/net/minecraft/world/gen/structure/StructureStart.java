package net.minecraft.world.gen.structure;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import java.util.LinkedList;
import java.util.Random;

public abstract class StructureStart {
    protected final LinkedList<StructureComponent> components = new LinkedList<>();
    protected StructureBoundingBox boundingBox;
    private int chunkPosX;
    private int chunkPosZ;

    public StructureStart() {
    }

    public StructureStart(int chunkX, int chunkZ) {
        this.chunkPosX = chunkX;
        this.chunkPosZ = chunkZ;
    }

    public StructureBoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    public LinkedList<StructureComponent> getComponents() {
        return this.components;
    }

    public void generateStructure(World worldIn, Random rand, StructureBoundingBox structurebb) {

        this.components.removeIf(structurecomponent -> structurecomponent.getBoundingBox().intersectsWith(structurebb) && !structurecomponent.addComponentParts(worldIn, rand, structurebb));
    }

    protected void updateBoundingBox() {
        this.boundingBox = StructureBoundingBox.getNewBoundingBox();

        for (StructureComponent structurecomponent : this.components) {
            this.boundingBox.expandTo(structurecomponent.getBoundingBox());
        }
    }

    public NBTTagCompound writeStructureComponentsToNBT(int chunkX, int chunkZ) {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        nbttagcompound.setString("id", MapGenStructureIO.getStructureStartName(this));
        nbttagcompound.setInteger("ChunkX", chunkX);
        nbttagcompound.setInteger("ChunkZ", chunkZ);
        nbttagcompound.setTag("BB", this.boundingBox.toNBTTagIntArray());
        NBTTagList nbttaglist = new NBTTagList();

        for (StructureComponent structurecomponent : this.components) {
            nbttaglist.appendTag(structurecomponent.createStructureBaseNBT());
        }

        nbttagcompound.setTag("Children", nbttaglist);
        this.writeToNBT(nbttagcompound);
        return nbttagcompound;
    }

    public void writeToNBT(NBTTagCompound tagCompound) {
    }

    public void readStructureComponentsFromNBT(World worldIn, NBTTagCompound tagCompound) {
        this.chunkPosX = tagCompound.getInteger("ChunkX");
        this.chunkPosZ = tagCompound.getInteger("ChunkZ");

        if (tagCompound.hasKey("BB")) {
            this.boundingBox = new StructureBoundingBox(tagCompound.getIntArray("BB"));
        }

        NBTTagList nbttaglist = tagCompound.getTagList("Children", 10);

        for (int i = 0; i < nbttaglist.tagCount(); ++i) {
            this.components.add(MapGenStructureIO.getStructureComponent(nbttaglist.getCompoundTagAt(i), worldIn));
        }

        this.readFromNBT(tagCompound);
    }

    public void readFromNBT(NBTTagCompound tagCompound) {
    }

    protected void markAvailableHeight(World worldIn, Random rand, int p_75067_3_) {
        int i = worldIn.getSeaLevel() - p_75067_3_;
        int j = this.boundingBox.getYSize() + 1;

        if (j < i) {
            j += rand.nextInt(i - j);
        }

        int k = j - this.boundingBox.maxY;
        this.boundingBox.offset(0, k, 0);

        for (StructureComponent structurecomponent : this.components) {
            structurecomponent.func_181138_a(0, k, 0);
        }
    }

    protected void setRandomHeight(World worldIn, Random rand, int p_75070_3_, int p_75070_4_) {
        int i = p_75070_4_ - p_75070_3_ + 1 - this.boundingBox.getYSize();
        int j;

        if (i > 1) {
            j = p_75070_3_ + rand.nextInt(i);
        } else {
            j = p_75070_3_;
        }

        int k = j - this.boundingBox.minY;
        this.boundingBox.offset(0, k, 0);

        for (StructureComponent structurecomponent : this.components) {
            structurecomponent.func_181138_a(0, k, 0);
        }
    }

    public boolean isSizeableStructure() {
        return true;
    }

    public boolean func_175788_a(ChunkCoordIntPair pair) {
        return true;
    }

    public void func_175787_b(ChunkCoordIntPair pair) {
    }

    public int getChunkPosX() {
        return this.chunkPosX;
    }

    public int getChunkPosZ() {
        return this.chunkPosZ;
    }
}
