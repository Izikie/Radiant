package net.minecraft.world.pathfinder;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;

public abstract class NodeProcessor {
    protected IBlockAccess blockaccess;
    protected final Int2ObjectOpenHashMap<PathPoint> pointMap = new Int2ObjectOpenHashMap<>();
    protected int entitySizeX;
    protected int entitySizeY;
    protected int entitySizeZ;

    public void initProcessor(IBlockAccess iblockaccessIn, Entity entityIn) {
        this.blockaccess = iblockaccessIn;
        this.pointMap.clear();
        this.entitySizeX = MathHelper.floor_float(entityIn.width + 1.0F);
        this.entitySizeY = MathHelper.floor_float(entityIn.height + 1.0F);
        this.entitySizeZ = MathHelper.floor_float(entityIn.width + 1.0F);
    }

    public void postProcess() {
    }

    protected PathPoint openPoint(int x, int y, int z) {
        int i = PathPoint.makeHash(x, y, z);
        PathPoint pathpoint = this.pointMap.get(i);

        if (pathpoint == null) {
            pathpoint = new PathPoint(x, y, z);
            this.pointMap.put(i, pathpoint);
        }

        return pathpoint;
    }

    public abstract PathPoint getPathPointTo(Entity entityIn);

    public abstract PathPoint getPathPointToCoords(Entity entityIn, double x, double y, double target);

    public abstract int findPathOptions(PathPoint[] pathOptions, Entity entityIn, PathPoint currentPoint, PathPoint targetPoint, float maxDistance);
}
