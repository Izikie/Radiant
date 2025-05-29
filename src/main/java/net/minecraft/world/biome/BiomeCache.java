package net.minecraft.world.biome;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public class BiomeCache {
    private final WorldChunkManager chunkManager;
    private long lastCleanupTime;
    private final Long2ObjectOpenHashMap<Block> cacheMap = new Long2ObjectOpenHashMap<>();
    private final List<Block> cache = new ArrayList<>();

    public BiomeCache(WorldChunkManager chunkManagerIn) {
        this.chunkManager = chunkManagerIn;
    }

    public Block getBiomeCacheBlock(int x, int z) {
        x = x >> 4;
        z = z >> 4;
        long i = x & 4294967295L | (z & 4294967295L) << 32;
        Block biomecache$block = this.cacheMap.get(i);

        if (biomecache$block == null) {
            biomecache$block = new Block(x, z);
            this.cacheMap.put(i, biomecache$block);
            this.cache.add(biomecache$block);
        }

        biomecache$block.lastAccessTime = MinecraftServer.getCurrentTimeMillis();
        return biomecache$block;
    }

    public BiomeGenBase func_180284_a(int x, int z, BiomeGenBase p_180284_3_) {
        BiomeGenBase biomegenbase = this.getBiomeCacheBlock(x, z).getBiomeGenAt(x, z);
        return biomegenbase == null ? p_180284_3_ : biomegenbase;
    }

    public void cleanupCache() {
        long i = MinecraftServer.getCurrentTimeMillis();
        long j = i - this.lastCleanupTime;

        if (j > 7500L || j < 0L) {
            this.lastCleanupTime = i;

            for (int k = 0; k < this.cache.size(); ++k) {
                Block biomecache$block = this.cache.get(k);
                long l = i - biomecache$block.lastAccessTime;

                if (l > 30000L || l < 0L) {
                    this.cache.remove(k--);
                    long i1 = biomecache$block.xPosition & 4294967295L | (biomecache$block.zPosition & 4294967295L) << 32;
                    this.cacheMap.remove(i1);
                }
            }
        }
    }

    public BiomeGenBase[] getCachedBiomes(int x, int z) {
        return this.getBiomeCacheBlock(x, z).biomes;
    }

    public class Block {
        public final float[] rainfallValues = new float[256];
        public final BiomeGenBase[] biomes = new BiomeGenBase[256];
        public final int xPosition;
        public final int zPosition;
        public long lastAccessTime;

        public Block(int x, int z) {
            this.xPosition = x;
            this.zPosition = z;
            BiomeCache.this.chunkManager.getRainfall(this.rainfallValues, x << 4, z << 4, 16, 16);
            BiomeCache.this.chunkManager.getBiomeGenAt(this.biomes, x << 4, z << 4, 16, 16, false);
        }

        public BiomeGenBase getBiomeGenAt(int x, int z) {
            return this.biomes[x & 15 | (z & 15) << 4];
        }
    }
}
