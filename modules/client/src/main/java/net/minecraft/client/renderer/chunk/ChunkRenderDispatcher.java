package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.RenderLayer;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ChunkRenderDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkRenderDispatcher.class);
    private static final ThreadFactory THREAD_FACTORY = (new ThreadFactoryBuilder()).setNameFormat("Chunk Batcher %d").setDaemon(true).build();
    private final List<ChunkRenderWorker> listThreadedWorkers;
    private final BlockingQueue<ChunkCompileTaskGenerator> queueChunkUpdates;
    private final BlockingQueue<RegionRenderCacheBuilder> queueFreeRenderBuilders;
    private final WorldVertexBufferUploader worldVertexUploader;
    private final VertexBufferUploader vertexUploader;
    private final Queue<ListenableFutureTask<?>> queueChunkUploads;
    private final ChunkRenderWorker renderWorker;
    private final int countRenderBuilders;
    private final List<RegionRenderCacheBuilder> listPausedBuilders;

    public ChunkRenderDispatcher() {
        this(-1);
    }

    public ChunkRenderDispatcher(int p_i4_1_) {
        this.listThreadedWorkers = new ArrayList<>();
        this.queueChunkUpdates = Queues.newArrayBlockingQueue(100);
        this.worldVertexUploader = new WorldVertexBufferUploader();
        this.vertexUploader = new VertexBufferUploader();
        this.queueChunkUploads = Queues.newArrayDeque();
        this.listPausedBuilders = new ArrayList<>();
        int i = Math.max(1, (int) (Runtime.getRuntime().maxMemory() * 0.3D) / 10485760);
        int j = Math.max(1, Math.clamp(Runtime.getRuntime().availableProcessors() - 2, 1, i / 5));

        if (p_i4_1_ < 0) {
            this.countRenderBuilders = Math.clamp(j * 8, 1, i);
        } else {
            this.countRenderBuilders = p_i4_1_;
        }

        for (int k = 0; k < j; ++k) {
            ChunkRenderWorker chunkrenderworker = new ChunkRenderWorker(this);
            Thread thread = THREAD_FACTORY.newThread(chunkrenderworker);
            thread.start();
            this.listThreadedWorkers.add(chunkrenderworker);
        }

        this.queueFreeRenderBuilders = Queues.newArrayBlockingQueue(this.countRenderBuilders);

        for (int l = 0; l < this.countRenderBuilders; ++l) {
            this.queueFreeRenderBuilders.add(new RegionRenderCacheBuilder());
        }

        this.renderWorker = new ChunkRenderWorker(this, new RegionRenderCacheBuilder());
    }

    public String getDebugInfo() {
        return String.format("pC: %03d, pU: %1d, aB: %1d", this.queueChunkUpdates.size(), this.queueChunkUploads.size(), this.queueFreeRenderBuilders.size());
    }

    public boolean runChunkUploads(long p_178516_1_) {
        boolean flag = false;

        while (true) {
            boolean flag1 = false;
            ListenableFutureTask<?> listenablefuturetask;

            synchronized (this.queueChunkUploads) {
                listenablefuturetask = this.queueChunkUploads.poll();
            }

            if (listenablefuturetask != null) {
                listenablefuturetask.run();
                flag1 = true;
                flag = true;
            }

            if (p_178516_1_ == 0L || !flag1) {
                break;
            }

            long i = p_178516_1_ - System.nanoTime();

            if (i < 0L) {
                break;
            }
        }

        return flag;
    }

    public boolean updateChunkLater(RenderChunk chunkRenderer) {
        chunkRenderer.getLockCompileTask().lock();
        boolean flag;

        try {
            ChunkCompileTaskGenerator chunkcompiletaskgenerator = chunkRenderer.makeCompileTaskChunk();
            chunkcompiletaskgenerator.addFinishRunnable(() -> ChunkRenderDispatcher.this.queueChunkUpdates.remove(chunkcompiletaskgenerator));
            boolean flag1 = this.queueChunkUpdates.offer(chunkcompiletaskgenerator);

            if (!flag1) {
                chunkcompiletaskgenerator.finish();
            }

            flag = flag1;
        } finally {
            chunkRenderer.getLockCompileTask().unlock();
        }

        return flag;
    }

    public boolean updateChunkNow(RenderChunk chunkRenderer) {
        chunkRenderer.getLockCompileTask().lock();
        boolean flag;

        try {
            ChunkCompileTaskGenerator chunkcompiletaskgenerator = chunkRenderer.makeCompileTaskChunk();

            try {
                this.renderWorker.processTask(chunkcompiletaskgenerator);
            } catch (InterruptedException _) {
            }

            flag = true;
        } finally {
            chunkRenderer.getLockCompileTask().unlock();
        }

        return flag;
    }

    public void stopChunkUpdates() {
        this.clearChunkUpdates();

        while (this.runChunkUploads(0L)) {
        }

        List<RegionRenderCacheBuilder> list = new ArrayList<>();

        while (list.size() != this.countRenderBuilders) {
            try {
                list.add(this.allocateRenderBuilder());
            } catch (InterruptedException _) {
            }
        }

        this.queueFreeRenderBuilders.addAll(list);
    }

    public void freeRenderBuilder(RegionRenderCacheBuilder p_178512_1_) {
        this.queueFreeRenderBuilders.add(p_178512_1_);
    }

    public RegionRenderCacheBuilder allocateRenderBuilder() throws InterruptedException {
        return this.queueFreeRenderBuilders.take();
    }

    public ChunkCompileTaskGenerator getNextChunkUpdate() throws InterruptedException {
        return this.queueChunkUpdates.take();
    }

    public boolean updateTransparencyLater(RenderChunk chunkRenderer) {
        chunkRenderer.getLockCompileTask().lock();
        boolean flag1;

        try {
            ChunkCompileTaskGenerator chunkcompiletaskgenerator = chunkRenderer.makeCompileTaskTransparency();

            if (chunkcompiletaskgenerator != null) {
                chunkcompiletaskgenerator.addFinishRunnable(() -> ChunkRenderDispatcher.this.queueChunkUpdates.remove(chunkcompiletaskgenerator));
                return this.queueChunkUpdates.offer(chunkcompiletaskgenerator);
            }

            flag1 = true;
        } finally {
            chunkRenderer.getLockCompileTask().unlock();
        }

        return flag1;
    }

    public ListenableFuture<Object> uploadChunk(RenderLayer player, WorldRenderer p_178503_2_, RenderChunk chunkRenderer, CompiledChunk compiledChunkIn) {
        if (Minecraft.get().isCallingFromMinecraftThread()) {
            if (OpenGlHelper.useVbo()) {
                this.uploadVertexBuffer(p_178503_2_, chunkRenderer.getVertexBufferByLayer(player.ordinal()));
            } else {
                this.uploadDisplayList(p_178503_2_, ((ListedRenderChunk) chunkRenderer).getDisplayList(player, compiledChunkIn), chunkRenderer);
            }

            p_178503_2_.setTranslation(0.0D, 0.0D, 0.0D);
            return Futures.immediateFuture(null);
        } else {
            ListenableFutureTask<Object> listenablefuturetask = ListenableFutureTask.create(() -> ChunkRenderDispatcher.this.uploadChunk(player, p_178503_2_, chunkRenderer, compiledChunkIn), null);

            synchronized (this.queueChunkUploads) {
                this.queueChunkUploads.add(listenablefuturetask);
                return listenablefuturetask;
            }
        }
    }

    private void uploadDisplayList(WorldRenderer p_178510_1_, int p_178510_2_, RenderChunk chunkRenderer) {
        GL11.glNewList(p_178510_2_, GL11.GL_COMPILE);
        GlStateManager.pushMatrix();
        chunkRenderer.multModelviewMatrix();
        this.worldVertexUploader.draw(p_178510_1_);
        GlStateManager.popMatrix();
        GL11.glEndList();
    }

    private void uploadVertexBuffer(WorldRenderer p_178506_1_, VertexBuffer vertexBufferIn) {
        this.vertexUploader.setVertexBuffer(vertexBufferIn);
        this.vertexUploader.draw(p_178506_1_);
    }

    public void clearChunkUpdates() {
        while (!this.queueChunkUpdates.isEmpty()) {
            ChunkCompileTaskGenerator chunkcompiletaskgenerator = this.queueChunkUpdates.poll();

            if (chunkcompiletaskgenerator != null) {
                chunkcompiletaskgenerator.finish();
            }
        }
    }

    public boolean hasChunkUpdates() {
        return this.queueChunkUpdates.isEmpty() && this.queueChunkUploads.isEmpty();
    }

    public void pauseChunkUpdates() {
        while (this.listPausedBuilders.size() != this.countRenderBuilders) {
            try {
                this.runChunkUploads(Long.MAX_VALUE);
                RegionRenderCacheBuilder regionrendercachebuilder = this.queueFreeRenderBuilders.poll(100L, TimeUnit.MILLISECONDS);

                if (regionrendercachebuilder != null) {
                    this.listPausedBuilders.add(regionrendercachebuilder);
                }
            } catch (InterruptedException _) {
            }
        }
    }

    public void resumeChunkUpdates() {
        this.queueFreeRenderBuilders.addAll(this.listPausedBuilders);
        this.listPausedBuilders.clear();
    }
}
