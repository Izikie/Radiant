package net.optifine;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.BlockPos;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.UUID;

public class RandomEntity implements IRandomEntity {
    private Entity entity;

    @Override
    public int getId() {
        UUID uuid = this.entity.getUniqueID();
        long i = uuid.getLeastSignificantBits();
        return (int) (i & 2147483647L);
    }

    @Override
    public BlockPos getSpawnPosition() {
        return this.entity.getDataWatcher().spawnPosition;
    }

    @Override
    public BiomeGenBase getSpawnBiome() {
        return this.entity.getDataWatcher().spawnBiome;
    }

    @Override
    public String getName() {
        return this.entity.hasCustomName() ? this.entity.getCustomNameTag() : null;
    }

    @Override
    public int getHealth() {
        if (!(this.entity instanceof EntityLiving entityliving)) {
            return 0;
        } else {
            return (int) entityliving.getHealth();
        }
    }

    @Override
    public int getMaxHealth() {
        if (!(this.entity instanceof EntityLiving entityliving)) {
            return 0;
        } else {
            return (int) entityliving.getMaxHealth();
        }
    }

    public Entity getEntity() {
        return this.entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }
}
