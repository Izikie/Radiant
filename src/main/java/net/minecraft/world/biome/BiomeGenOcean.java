package net.minecraft.world.biome;

import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Random;

public class BiomeGenOcean extends BiomeGenBase {
    public BiomeGenOcean(int id) {
        super(id);
        this.spawnableCreatureList.clear();
    }

    @Override
    public TempCategory getTempCategory() {
        return TempCategory.OCEAN;
    }

    @Override
    public void genTerrainBlocks(World worldIn, Random rand, ChunkPrimer chunkPrimerIn, int x, int z, double noiseVal) {
        super.genTerrainBlocks(worldIn, rand, chunkPrimerIn, x, z, noiseVal);
    }
}
