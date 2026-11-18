package net.minecraft.client.renderer.chunk;

import net.minecraft.util.math.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Bounded queue whose priorities follow the most recently published camera position. */
final class ChunkCompileQueue {
    private static final int CAPACITY = 100;
    private static final int REBUILD_QUOTA = 2;

    private final List<ChunkCompileTaskGenerator> pending = new ArrayList<>(CAPACITY);
    private volatile Vec3 camera;
    private int rebuildQuota = REBUILD_QUOTA;

    void setCamera(Vec3 camera) {
        this.camera = camera;
    }

    Vec3 getCamera() {
        return camera;
    }

    synchronized boolean offer(ChunkCompileTaskGenerator task) {
        if (pending.size() == CAPACITY) {
            return false;
        }
        pending.add(task);
        notify();
        return true;
    }

    synchronized ChunkCompileTaskGenerator take() throws InterruptedException {
        while (pending.isEmpty()) {
            wait();
        }
        Vec3 position = camera;
        int nearestNew = -1;
        int nearestRebuild = -1;
        double newDistance = Double.POSITIVE_INFINITY;
        double rebuildDistance = Double.POSITIVE_INFINITY;
        for (int index = 0; index < pending.size(); index++) {
            ChunkCompileTaskGenerator task = pending.get(index);
            double distance = position == null ? 0 : task.distanceSquaredTo(position);
            if (task.isRecompile()) {
                if (nearestRebuild < 0 || distance < rebuildDistance) {
                    nearestRebuild = index;
                    rebuildDistance = distance;
                }
            } else if (nearestNew < 0 || distance < newDistance) {
                nearestNew = index;
                newDistance = distance;
            }
        }
        if (nearestRebuild >= 0 && (nearestNew < 0 || (rebuildQuota > 0 && rebuildDistance < newDistance))) {
            rebuildQuota = Math.max(0, rebuildQuota - 1);
            return pending.remove(nearestRebuild);
        }
        rebuildQuota = REBUILD_QUOTA;
        return pending.remove(nearestNew);
    }

    synchronized void remove(ChunkCompileTaskGenerator task) {
        pending.remove(task);
    }

    synchronized int size() {
        return pending.size();
    }

    synchronized List<ChunkCompileTaskGenerator> drain() {
        List<ChunkCompileTaskGenerator> discarded = new ArrayList<>(pending);
        pending.clear();
        rebuildQuota = REBUILD_QUOTA;
        return discarded;
    }
}
