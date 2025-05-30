package net.minecraft.client.renderer.chunk;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.src.Config;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.RenderLayer;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.optifine.BlockPosM;
import net.optifine.CustomBlockLayers;
import net.optifine.override.ChunkCacheOF;
import net.optifine.render.AabbFrame;
import net.optifine.render.RenderEnv;
import net.optifine.shaders.SVertexBuilder;

import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class RenderChunk {
    private final World world;
    private final RenderGlobal renderGlobal;
    public static int renderChunksUpdated;
    private BlockPos position;
    public CompiledChunk compiledChunk = CompiledChunk.DUMMY;
    private final ReentrantLock lockCompileTask = new ReentrantLock();
    private final ReentrantLock lockCompiledChunk = new ReentrantLock();
    private ChunkCompileTaskGenerator compileTask = null;
    private final Set<TileEntity> setTileEntities = new HashSet<>();
    private final int index;
    private final FloatBuffer modelviewMatrix = GLAllocation.createDirectFloatBuffer(16);
    private final VertexBuffer[] vertexBuffers = new VertexBuffer[RenderLayer.values().length];
    public AxisAlignedBB boundingBox;
    private int frameIndex = -1;
    private boolean needsUpdate = true;
    private final EnumMap<Direction, BlockPos> mapEnumFacing = null;
    private final BlockPos[] positionOffsets16 = new BlockPos[Direction.VALUES.length];
    public static final RenderLayer[] ENUM_WORLD_BLOCK_LAYERS = RenderLayer.values();
    private final RenderLayer[] blockLayersSingle = new RenderLayer[1];
    private final boolean isMipmaps = Config.isMipmaps();
    private boolean playerUpdate = false;
    public int regionX;
    public int regionZ;
    private final RenderChunk[] renderChunksOfset16 = new RenderChunk[6];
    private boolean renderChunksOffset16Updated = false;
    private Chunk chunk;
    private final RenderChunk[] renderChunkNeighbours = new RenderChunk[Direction.VALUES.length];
    private final RenderChunk[] renderChunkNeighboursValid = new RenderChunk[Direction.VALUES.length];
    private boolean renderChunkNeighboursUpated = false;
    private final RenderGlobal.ContainerLocalRenderInformation renderInfo = new RenderGlobal.ContainerLocalRenderInformation(this, null, 0);
    public AabbFrame boundingBoxParent;

    public RenderChunk(World worldIn, RenderGlobal renderGlobalIn, BlockPos blockPosIn, int indexIn) {
        this.world = worldIn;
        this.renderGlobal = renderGlobalIn;
        this.index = indexIn;

        if (!blockPosIn.equals(this.getPosition())) {
            this.setPosition(blockPosIn);
        }

        if (OpenGlHelper.useVbo()) {
            for (int i = 0; i < RenderLayer.values().length; ++i) {
                this.vertexBuffers[i] = new VertexBuffer(DefaultVertexFormats.BLOCK);
            }
        }
    }

    public boolean setFrameIndex(int frameIndexIn) {
        if (this.frameIndex == frameIndexIn) {
            return false;
        } else {
            this.frameIndex = frameIndexIn;
            return true;
        }
    }

    public VertexBuffer getVertexBufferByLayer(int layer) {
        return this.vertexBuffers[layer];
    }

    public void setPosition(BlockPos pos) {
        this.stopCompileTask();
        this.position = pos;
        int i = 8;
        this.regionX = pos.getX() >> i << i;
        this.regionZ = pos.getZ() >> i << i;
        this.boundingBox = new AxisAlignedBB(pos, pos.add(16, 16, 16));
        this.initModelviewMatrix();

        Arrays.fill(this.positionOffsets16, null);

        this.renderChunksOffset16Updated = false;
        this.renderChunkNeighboursUpated = false;

        for (RenderChunk renderchunk : this.renderChunkNeighbours) {
            if (renderchunk != null) {
                renderchunk.renderChunkNeighboursUpated = false;
            }
        }

        this.chunk = null;
        this.boundingBoxParent = null;
    }

    public void resortTransparency(float x, float y, float z, ChunkCompileTaskGenerator generator) {
        CompiledChunk compiledchunk = generator.getCompiledChunk();

        if (compiledchunk.getState() != null && !compiledchunk.isLayerEmpty(RenderLayer.TRANSLUCENT)) {
            WorldRenderer worldrenderer = generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(RenderLayer.TRANSLUCENT);
            this.preRenderBlocks(worldrenderer, this.position);
            worldrenderer.setVertexState(compiledchunk.getState());
            this.postRenderBlocks(RenderLayer.TRANSLUCENT, x, y, z, worldrenderer, compiledchunk);
        }
    }

    public void rebuildChunk(float x, float y, float z, ChunkCompileTaskGenerator generator) {
        CompiledChunk compiledchunk = new CompiledChunk();
        int i = 1;
        BlockPos blockpos = new BlockPos(this.position);
        BlockPos blockpos1 = blockpos.add(15, 15, 15);
        generator.getLock().lock();

        try {
            if (generator.getStatus() != ChunkCompileTaskGenerator.Status.COMPILING) {
                return;
            }

            generator.setCompiledChunk(compiledchunk);
        } finally {
            generator.getLock().unlock();
        }

        VisGraph lvt_10_1_ = new VisGraph();
        HashSet<TileEntity> lvt_11_1_ = new HashSet<>();

        if (!this.isChunkRegionEmpty(blockpos)) {
            ++renderChunksUpdated;
            ChunkCacheOF chunkcacheof = this.makeChunkCacheOF(blockpos);
            chunkcacheof.renderStart();
            boolean[] aboolean = new boolean[ENUM_WORLD_BLOCK_LAYERS.length];
            BlockRendererDispatcher blockrendererdispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();

            for (Object o : BlockPosM.getAllInBoxMutable(blockpos, blockpos1)) {
                BlockPosM blockposm = (BlockPosM) o;
                IBlockState iblockstate = chunkcacheof.getBlockState(blockposm);
                Block block = iblockstate.getBlock();

                if (block.isOpaqueCube()) {
                    lvt_10_1_.func_178606_a(blockposm);
                }

                if (block.hasTileEntity()) {
                    TileEntity tileentity = chunkcacheof.getTileEntity(new BlockPos(blockposm));
                    TileEntitySpecialRenderer<TileEntity> tileentityspecialrenderer = TileEntityRendererDispatcher.INSTANCE.getSpecialRenderer(tileentity);

                    if (tileentity != null && tileentityspecialrenderer != null) {
                        compiledchunk.addTileEntity(tileentity);

                        if (tileentityspecialrenderer.forceTileEntityRender()) {
                            lvt_11_1_.add(tileentity);
                        }
                    }
                }

                RenderLayer[] aenumworldblocklayer;

                aenumworldblocklayer = this.blockLayersSingle;
                aenumworldblocklayer[0] = block.getBlockLayer();

                for (RenderLayer enumWorldBlockLayer : aenumworldblocklayer) {
                    RenderLayer enumworldblocklayer = enumWorldBlockLayer;

                    enumworldblocklayer = this.fixBlockLayer(iblockstate, enumworldblocklayer);
                    int k = enumworldblocklayer.ordinal();

                    if (block.getRenderType() != -1) {
                        WorldRenderer worldrenderer = generator.getRegionRenderCacheBuilder().getWorldRendererByLayerId(k);
                        worldrenderer.setBlockLayer(enumworldblocklayer);
                        RenderEnv renderenv = worldrenderer.getRenderEnv(iblockstate, blockposm);
                        renderenv.setRegionRenderCacheBuilder(generator.getRegionRenderCacheBuilder());

                        if (!compiledchunk.isLayerStarted(enumworldblocklayer)) {
                            compiledchunk.setLayerStarted(enumworldblocklayer);
                            this.preRenderBlocks(worldrenderer, blockpos);
                        }

                        aboolean[k] |= blockrendererdispatcher.renderBlock(iblockstate, blockposm, chunkcacheof, worldrenderer);

                        if (renderenv.isOverlaysRendered()) {
                            this.postRenderOverlays(generator.getRegionRenderCacheBuilder(), compiledchunk, aboolean);
                            renderenv.setOverlaysRendered(false);
                        }
                    }
                }

            }

            for (RenderLayer enumworldblocklayer1 : ENUM_WORLD_BLOCK_LAYERS) {
                if (aboolean[enumworldblocklayer1.ordinal()]) {
                    compiledchunk.setLayerUsed(enumworldblocklayer1);
                }

                if (compiledchunk.isLayerStarted(enumworldblocklayer1)) {
                    if (Config.isShaders()) {
                        SVertexBuilder.calcNormalChunkLayer(generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(enumworldblocklayer1));
                    }

                    WorldRenderer worldrenderer1 = generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(enumworldblocklayer1);
                    this.postRenderBlocks(enumworldblocklayer1, x, y, z, worldrenderer1, compiledchunk);

                    if (worldrenderer1.animatedSprites != null) {
                        compiledchunk.setAnimatedSprites(enumworldblocklayer1, (BitSet) worldrenderer1.animatedSprites.clone());
                    }
                } else {
                    compiledchunk.setAnimatedSprites(enumworldblocklayer1, null);
                }
            }

            chunkcacheof.renderFinish();
        }

        compiledchunk.setVisibility(lvt_10_1_.computeVisibility());
        this.lockCompileTask.lock();

        try {
            Set<TileEntity> set = new HashSet<>(lvt_11_1_);
            Set<TileEntity> set1 = new HashSet<>(this.setTileEntities);
            set.removeAll(this.setTileEntities);
            set1.removeAll(lvt_11_1_);
            this.setTileEntities.clear();
            this.setTileEntities.addAll(lvt_11_1_);
            this.renderGlobal.updateTileEntities(set1, set);
        } finally {
            this.lockCompileTask.unlock();
        }
    }

    protected void finishCompileTask() {
        this.lockCompileTask.lock();

        try {
            if (this.compileTask != null && this.compileTask.getStatus() != ChunkCompileTaskGenerator.Status.DONE) {
                this.compileTask.finish();
                this.compileTask = null;
            }
        } finally {
            this.lockCompileTask.unlock();
        }
    }

    public ReentrantLock getLockCompileTask() {
        return this.lockCompileTask;
    }

    public ChunkCompileTaskGenerator makeCompileTaskChunk() {
        this.lockCompileTask.lock();
        ChunkCompileTaskGenerator chunkcompiletaskgenerator;

        try {
            this.finishCompileTask();
            this.compileTask = new ChunkCompileTaskGenerator(this, ChunkCompileTaskGenerator.Type.REBUILD_CHUNK);
            chunkcompiletaskgenerator = this.compileTask;
        } finally {
            this.lockCompileTask.unlock();
        }

        return chunkcompiletaskgenerator;
    }

    public ChunkCompileTaskGenerator makeCompileTaskTransparency() {
        this.lockCompileTask.lock();
        ChunkCompileTaskGenerator chunkcompiletaskgenerator1;

        try {
            if (this.compileTask != null && this.compileTask.getStatus() == ChunkCompileTaskGenerator.Status.PENDING) {
                return null;
            }

            if (this.compileTask != null && this.compileTask.getStatus() != ChunkCompileTaskGenerator.Status.DONE) {
                this.compileTask.finish();
                this.compileTask = null;
            }

            this.compileTask = new ChunkCompileTaskGenerator(this, ChunkCompileTaskGenerator.Type.RESORT_TRANSPARENCY);
            this.compileTask.setCompiledChunk(this.compiledChunk);
            chunkcompiletaskgenerator1 = this.compileTask;
        } finally {
            this.lockCompileTask.unlock();
        }

        return chunkcompiletaskgenerator1;
    }

    private void preRenderBlocks(WorldRenderer worldRendererIn, BlockPos pos) {
        worldRendererIn.begin(7, DefaultVertexFormats.BLOCK);

        if (Config.isRenderRegions()) {
            int i = 8;
            int j;
            int k = pos.getY() >> i << i;
            int l;
            j = this.regionX;
            l = this.regionZ;
            worldRendererIn.setTranslation((-j), (-k), (-l));
        } else {
            worldRendererIn.setTranslation((-pos.getX()), (-pos.getY()), (-pos.getZ()));
        }
    }

    private void postRenderBlocks(RenderLayer layer, float x, float y, float z, WorldRenderer worldRendererIn, CompiledChunk compiledChunkIn) {
        if (layer == RenderLayer.TRANSLUCENT && !compiledChunkIn.isLayerEmpty(layer)) {
            worldRendererIn.sortVertexData(x, y, z);
            compiledChunkIn.setState(worldRendererIn.getVertexState());
        }

        worldRendererIn.finishDrawing();
    }

    private void initModelviewMatrix() {
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        float f = 1.000001F;
        GlStateManager.translate(-8.0F, -8.0F, -8.0F);
        GlStateManager.scale(f, f, f);
        GlStateManager.translate(8.0F, 8.0F, 8.0F);
        GlStateManager.getFloat(2982, this.modelviewMatrix);
        GlStateManager.popMatrix();
    }

    public void multModelviewMatrix() {
        GlStateManager.multMatrix(this.modelviewMatrix);
    }

    public CompiledChunk getCompiledChunk() {
        return this.compiledChunk;
    }

    public void setCompiledChunk(CompiledChunk compiledChunkIn) {
        this.lockCompiledChunk.lock();

        try {
            this.compiledChunk = compiledChunkIn;
        } finally {
            this.lockCompiledChunk.unlock();
        }
    }

    public void stopCompileTask() {
        this.finishCompileTask();
        this.compiledChunk = CompiledChunk.DUMMY;
    }

    public void deleteGlResources() {
        this.stopCompileTask();

        for (int i = 0; i < RenderLayer.values().length; ++i) {
            if (this.vertexBuffers[i] != null) {
                this.vertexBuffers[i].deleteGlBuffers();
            }
        }
    }

    public BlockPos getPosition() {
        return this.position;
    }

    public void setNeedsUpdate(boolean needsUpdateIn) {
        this.needsUpdate = needsUpdateIn;

        if (needsUpdateIn) {
            if (this.isWorldPlayerUpdate()) {
                this.playerUpdate = true;
            }
        } else {
            this.playerUpdate = false;
        }
    }

    public boolean isNeedsUpdate() {
        return this.needsUpdate;
    }

    public BlockPos getBlockPosOffset16(Direction p_181701_1_) {
        return this.getPositionOffset16(p_181701_1_);
    }

    public BlockPos getPositionOffset16(Direction p_getPositionOffset16_1_) {
        int i = p_getPositionOffset16_1_.getIndex();
        BlockPos blockpos = this.positionOffsets16[i];

        if (blockpos == null) {
            blockpos = this.getPosition().offset(p_getPositionOffset16_1_, 16);
            this.positionOffsets16[i] = blockpos;
        }

        return blockpos;
    }

    private boolean isWorldPlayerUpdate() {
        if (this.world instanceof WorldClient worldclient) {
            return worldclient.isPlayerUpdate();
        } else {
            return false;
        }
    }

    public boolean isPlayerUpdate() {
        return this.playerUpdate;
    }

    protected RegionRenderCache createRegionRenderCache(World p_createRegionRenderCache_1_, BlockPos p_createRegionRenderCache_2_, BlockPos p_createRegionRenderCache_3_, int p_createRegionRenderCache_4_) {
        return new RegionRenderCache(p_createRegionRenderCache_1_, p_createRegionRenderCache_2_, p_createRegionRenderCache_3_, p_createRegionRenderCache_4_);
    }

    private RenderLayer fixBlockLayer(IBlockState p_fixBlockLayer_1_, RenderLayer p_fixBlockLayer_2_) {
        if (CustomBlockLayers.isActive()) {
            RenderLayer enumworldblocklayer = CustomBlockLayers.getRenderLayer(p_fixBlockLayer_1_);

            if (enumworldblocklayer != null) {
                return enumworldblocklayer;
            }
        }

        if (this.isMipmaps) {
            if (p_fixBlockLayer_2_ == RenderLayer.CUTOUT) {
                Block block = p_fixBlockLayer_1_.getBlock();

                if (block instanceof BlockRedstoneWire) {
                    return p_fixBlockLayer_2_;
                }

                if (block instanceof BlockCactus) {
                    return p_fixBlockLayer_2_;
                }

                return RenderLayer.CUTOUT_MIPPED;
            }
        } else if (p_fixBlockLayer_2_ == RenderLayer.CUTOUT_MIPPED) {
            return RenderLayer.CUTOUT;
        }

        return p_fixBlockLayer_2_;
    }

    private void postRenderOverlays(RegionRenderCacheBuilder p_postRenderOverlays_1_, CompiledChunk p_postRenderOverlays_2_, boolean[] p_postRenderOverlays_3_) {
        this.postRenderOverlay(RenderLayer.CUTOUT, p_postRenderOverlays_1_, p_postRenderOverlays_2_, p_postRenderOverlays_3_);
        this.postRenderOverlay(RenderLayer.CUTOUT_MIPPED, p_postRenderOverlays_1_, p_postRenderOverlays_2_, p_postRenderOverlays_3_);
        this.postRenderOverlay(RenderLayer.TRANSLUCENT, p_postRenderOverlays_1_, p_postRenderOverlays_2_, p_postRenderOverlays_3_);
    }

    private void postRenderOverlay(RenderLayer p_postRenderOverlay_1_, RegionRenderCacheBuilder p_postRenderOverlay_2_, CompiledChunk p_postRenderOverlay_3_, boolean[] p_postRenderOverlay_4_) {
        WorldRenderer worldrenderer = p_postRenderOverlay_2_.getWorldRendererByLayer(p_postRenderOverlay_1_);

        if (worldrenderer.isDrawing()) {
            p_postRenderOverlay_3_.setLayerStarted(p_postRenderOverlay_1_);
            p_postRenderOverlay_4_[p_postRenderOverlay_1_.ordinal()] = true;
        }
    }

    private ChunkCacheOF makeChunkCacheOF(BlockPos p_makeChunkCacheOF_1_) {
        BlockPos blockpos = p_makeChunkCacheOF_1_.add(-1, -1, -1);
        BlockPos blockpos1 = p_makeChunkCacheOF_1_.add(16, 16, 16);
        ChunkCache chunkcache = this.createRegionRenderCache(this.world, blockpos, blockpos1, 1);

        return new ChunkCacheOF(chunkcache, blockpos, blockpos1, 1);
    }

    public RenderChunk getRenderChunkOffset16(ViewFrustum p_getRenderChunkOffset16_1_, Direction p_getRenderChunkOffset16_2_) {
        if (!this.renderChunksOffset16Updated) {
            for (int i = 0; i < Direction.VALUES.length; ++i) {
                Direction enumfacing = Direction.VALUES[i];
                BlockPos blockpos = this.getBlockPosOffset16(enumfacing);
                this.renderChunksOfset16[i] = p_getRenderChunkOffset16_1_.getRenderChunk(blockpos);
            }

            this.renderChunksOffset16Updated = true;
        }

        return this.renderChunksOfset16[p_getRenderChunkOffset16_2_.ordinal()];
    }

    public Chunk getChunk() {
        return this.getChunk(this.position);
    }

    private Chunk getChunk(BlockPos p_getChunk_1_) {
        Chunk chunk = this.chunk;

        if (chunk == null || !chunk.isLoaded()) {
            chunk = this.world.getChunkFromBlockCoords(p_getChunk_1_);
            this.chunk = chunk;
        }
        return chunk;
    }

    public boolean isChunkRegionEmpty() {
        return this.isChunkRegionEmpty(this.position);
    }

    private boolean isChunkRegionEmpty(BlockPos p_isChunkRegionEmpty_1_) {
        int i = p_isChunkRegionEmpty_1_.getY();
        int j = i + 15;
        return this.getChunk(p_isChunkRegionEmpty_1_).getAreLevelsEmpty(i, j);
    }

    public void setRenderChunkNeighbour(Direction p_setRenderChunkNeighbour_1_, RenderChunk p_setRenderChunkNeighbour_2_) {
        this.renderChunkNeighbours[p_setRenderChunkNeighbour_1_.ordinal()] = p_setRenderChunkNeighbour_2_;
        this.renderChunkNeighboursValid[p_setRenderChunkNeighbour_1_.ordinal()] = p_setRenderChunkNeighbour_2_;
    }

    public RenderChunk getRenderChunkNeighbour(Direction p_getRenderChunkNeighbour_1_) {
        if (!this.renderChunkNeighboursUpated) {
            this.updateRenderChunkNeighboursValid();
        }

        return this.renderChunkNeighboursValid[p_getRenderChunkNeighbour_1_.ordinal()];
    }

    public RenderGlobal.ContainerLocalRenderInformation getRenderInfo() {
        return this.renderInfo;
    }

    private void updateRenderChunkNeighboursValid() {
        int i = this.getPosition().getX();
        int j = this.getPosition().getZ();
        int k = Direction.NORTH.ordinal();
        int l = Direction.SOUTH.ordinal();
        int i1 = Direction.WEST.ordinal();
        int j1 = Direction.EAST.ordinal();
        this.renderChunkNeighboursValid[k] = this.renderChunkNeighbours[k].getPosition().getZ() == j - 16 ? this.renderChunkNeighbours[k] : null;
        this.renderChunkNeighboursValid[l] = this.renderChunkNeighbours[l].getPosition().getZ() == j + 16 ? this.renderChunkNeighbours[l] : null;
        this.renderChunkNeighboursValid[i1] = this.renderChunkNeighbours[i1].getPosition().getX() == i - 16 ? this.renderChunkNeighbours[i1] : null;
        this.renderChunkNeighboursValid[j1] = this.renderChunkNeighbours[j1].getPosition().getX() == i + 16 ? this.renderChunkNeighbours[j1] : null;
        this.renderChunkNeighboursUpated = true;
    }

    public boolean isBoundingBoxInFrustum(ICamera p_isBoundingBoxInFrustum_1_, int p_isBoundingBoxInFrustum_2_) {
        return this.getBoundingBoxParent().isBoundingBoxInFrustumFully(p_isBoundingBoxInFrustum_1_, p_isBoundingBoxInFrustum_2_) || p_isBoundingBoxInFrustum_1_.isBoundingBoxInFrustum(this.boundingBox);
    }

    public AabbFrame getBoundingBoxParent() {
        if (this.boundingBoxParent == null) {
            BlockPos blockpos = this.getPosition();
            int i = blockpos.getX();
            int j = blockpos.getY();
            int k = blockpos.getZ();
            int l = 5;
            int i1 = i >> l << l;
            int j1 = j >> l << l;
            int k1 = k >> l << l;

            if (i1 != i || j1 != j || k1 != k) {
                AabbFrame aabbframe = this.renderGlobal.getRenderChunk(new BlockPos(i1, j1, k1)).getBoundingBoxParent();

                if (aabbframe != null && aabbframe.minX == i1 && aabbframe.minY == j1 && aabbframe.minZ == k1) {
                    this.boundingBoxParent = aabbframe;
                }
            }

            if (this.boundingBoxParent == null) {
                int l1 = 1 << l;
                this.boundingBoxParent = new AabbFrame(i1, j1, k1, (i1 + l1), (j1 + l1), (k1 + l1));
            }
        }

        return this.boundingBoxParent;
    }

    public String toString() {
        return "pos: " + this.getPosition() + ", frameIndex: " + this.frameIndex;
    }
}
