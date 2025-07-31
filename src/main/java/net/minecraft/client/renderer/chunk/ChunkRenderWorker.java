package net.minecraft.client.renderer.chunk;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.Entity;
import net.minecraft.util.RenderLayer;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

public class ChunkRenderWorker implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkRenderWorker.class);
    private final ChunkRenderDispatcher chunkRenderDispatcher;
    private final RegionRenderCacheBuilder regionRenderCacheBuilder;

    public ChunkRenderWorker(ChunkRenderDispatcher p_i46201_1_) {
        this(p_i46201_1_, null);
    }

    public ChunkRenderWorker(ChunkRenderDispatcher chunkRenderDispatcherIn, RegionRenderCacheBuilder regionRenderCacheBuilderIn) {
        this.chunkRenderDispatcher = chunkRenderDispatcherIn;
        this.regionRenderCacheBuilder = regionRenderCacheBuilderIn;
    }

    public void run() {
        while (true) {
            try {
                this.processTask(this.chunkRenderDispatcher.getNextChunkUpdate());
            } catch (InterruptedException exception) {
                LOGGER.debug("Stopping due to interrupt");
                return;
            } catch (Throwable throwable) {
                CrashReport report = CrashReport.makeCrashReport(throwable, "Batching chunks");
                Minecraft.get().crashed(Minecraft.get().addGraphicsAndWorldToCrashReport(report));
                return;
            }
        }
    }

    protected void processTask(final ChunkCompileTaskGenerator generator) throws InterruptedException {
        generator.getLock().lock();

        try {
            if (generator.getStatus() != ChunkCompileTaskGenerator.Status.PENDING) {
                if (!generator.isFinished()) {
                    LOGGER.warn("Chunk render task was {} when I expected it to be pending; ignoring task", generator.getStatus());
                }

                return;
            }

            generator.setStatus(ChunkCompileTaskGenerator.Status.COMPILING);
        } finally {
            generator.getLock().unlock();
        }

        Entity lvt_2_1_ = Minecraft.get().getRenderViewEntity();

        if (lvt_2_1_ == null) {
            generator.finish();
        } else {
            generator.setRegionRenderCacheBuilder(this.getRegionRenderCacheBuilder());
            float f = (float) lvt_2_1_.posX;
            float f1 = (float) lvt_2_1_.posY + lvt_2_1_.getEyeHeight();
            float f2 = (float) lvt_2_1_.posZ;
            ChunkCompileTaskGenerator.Type chunkcompiletaskgenerator$type = generator.getType();

            if (chunkcompiletaskgenerator$type == ChunkCompileTaskGenerator.Type.REBUILD_CHUNK) {
                generator.getRenderChunk().rebuildChunk(f, f1, f2, generator);
            } else if (chunkcompiletaskgenerator$type == ChunkCompileTaskGenerator.Type.RESORT_TRANSPARENCY) {
                generator.getRenderChunk().resortTransparency(f, f1, f2, generator);
            }

            generator.getLock().lock();

            try {
                if (generator.getStatus() != ChunkCompileTaskGenerator.Status.COMPILING) {
                    if (!generator.isFinished()) {
                        LOGGER.warn("Chunk render task was {} when I expected it to be compiling; aborting task", generator.getStatus());
                    }

                    this.freeRenderBuilder(generator);
                    return;
                }

                generator.setStatus(ChunkCompileTaskGenerator.Status.UPLOADING);
            } finally {
                generator.getLock().unlock();
            }

            final CompiledChunk lvt_7_1_ = generator.getCompiledChunk();
            ArrayList<ListenableFuture<Object>> lvt_8_1_ = new ArrayList<>();

            if (chunkcompiletaskgenerator$type == ChunkCompileTaskGenerator.Type.REBUILD_CHUNK) {
                for (RenderLayer enumworldblocklayer : RenderLayer.values()) {
                    if (lvt_7_1_.isLayerStarted(enumworldblocklayer)) {
                        lvt_8_1_.add(this.chunkRenderDispatcher.uploadChunk(enumworldblocklayer, generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(enumworldblocklayer), generator.getRenderChunk(), lvt_7_1_));
                    }
                }
            } else if (chunkcompiletaskgenerator$type == ChunkCompileTaskGenerator.Type.RESORT_TRANSPARENCY) {
                lvt_8_1_.add(this.chunkRenderDispatcher.uploadChunk(RenderLayer.TRANSLUCENT, generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(RenderLayer.TRANSLUCENT), generator.getRenderChunk(), lvt_7_1_));
            }

            final ListenableFuture<List<Object>> listenablefuture = Futures.allAsList(lvt_8_1_);
            generator.addFinishRunnable(() -> listenablefuture.cancel(false));
            Futures.addCallback(listenablefuture, new FutureCallback<>() {
                public void onSuccess(List<Object> p_onSuccess_1_) {
                    ChunkRenderWorker.this.freeRenderBuilder(generator);
                    generator.getLock().lock();
                    label21:
                    {
                        try {
                            if (generator.getStatus() == ChunkCompileTaskGenerator.Status.UPLOADING) {
                                generator.setStatus(ChunkCompileTaskGenerator.Status.DONE);
                                break label21;
                            }

                            if (!generator.isFinished()) {
                                ChunkRenderWorker.LOGGER.warn("Chunk render task was {} when I expected it to be uploading; aborting task", generator.getStatus());
                            }
                        } finally {
                            generator.getLock().unlock();
                        }

                        return;
                    }
                    generator.getRenderChunk().setCompiledChunk(lvt_7_1_);
                }

                public void onFailure(Throwable throwable) {
                    ChunkRenderWorker.this.freeRenderBuilder(generator);

                    if (!(throwable instanceof CancellationException) && !(throwable instanceof InterruptedException)) {
                        Minecraft.get().crashed(CrashReport.makeCrashReport(throwable, "Rendering chunk"));
                    }
                }
            }, Runnable::run);
        }
    }

    private RegionRenderCacheBuilder getRegionRenderCacheBuilder() throws InterruptedException {
        return this.regionRenderCacheBuilder != null ? this.regionRenderCacheBuilder : this.chunkRenderDispatcher.allocateRenderBuilder();
    }

    private void freeRenderBuilder(ChunkCompileTaskGenerator taskGenerator) {
        if (this.regionRenderCacheBuilder == null) {
            this.chunkRenderDispatcher.freeRenderBuilder(taskGenerator.getRegionRenderCacheBuilder());
        }
    }
}
