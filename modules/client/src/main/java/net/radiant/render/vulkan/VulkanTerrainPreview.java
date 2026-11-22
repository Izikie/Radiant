package net.radiant.render.vulkan;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.RenderLayer;
import net.minecraft.util.math.Vec3;
import net.minecraft.world.border.WorldBorderStage;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.CustomSky;
import net.radiant.benchmark.ClientBenchmark;
import net.radiant.util.NativeImage;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.memCalloc;
import static org.lwjgl.system.MemoryUtil.memFree;

/** Temporary presentation bridge while 1.8.9 runs its game loop and chunk builders. */
public final class VulkanTerrainPreview implements AutoCloseable {
    public static final boolean ENABLED = Boolean.getBoolean("radiant.vulkan.terrain");
    private static final boolean TERRAIN_ONLY = Boolean.getBoolean("radiant.vulkan.terrainOnly");
    private static final Logger LOGGER = LoggerFactory.getLogger(VulkanTerrainPreview.class);
    private static final RenderLayer[] LAYERS = RenderLayer.values();
    private static VulkanTerrainPreview active;
    private static boolean manuallyClosed;
    private static boolean unsupportedSettings;
    private static boolean rebuildOpenGlTerrain;

    private final long window;
    private final VulkanObjectCapture objects = new VulkanObjectCapture();
    private final VulkanTerrainScene terrain = new VulkanTerrainScene(objects);
    private final VulkanPresenter presenter;
    private final Map<RenderChunk, Mesh[]> sections = new IdentityHashMap<>();
    private boolean atlasDirty = true;
    private boolean windowVisible;
    private boolean skippedOpenGlUpload;
    private VulkanTerrainScene.Frame pendingFrame;
    private long frames;
    private long drawCalls;

    private VulkanTerrainPreview(long window) {
        this.window = window;
        this.presenter = new VulkanPresenter(window, terrain, ClientBenchmark.COMPARE_FRAMES && !TERRAIN_ONLY);
    }

    public static void openIfNeeded(Minecraft mc) {
        if (manuallyClosed) {
            return;
        }
        if (Config.isShaders() || Config.isMultiTexture()) {
            if (!unsupportedSettings) {
                shutdown();
                unsupportedSettings = true;
                LOGGER.warn("Vulkan terrain preview requires shader packs, antialiasing, and anisotropic filtering to be off");
            }
            return;
        }
        unsupportedSettings = false;
        if (active != null && active.windowVisible) {
            return;
        }
        if (active != null) {
            glfwShowWindow(active.window);
            active.windowVisible = true;
            return;
        }
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        long window;
        try {
            window = glfwCreateWindow(mc.displayWidth, mc.displayHeight, "Radiant Vulkan Terrain", 0, 0);
        } finally {
            glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
            glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_TRUE);
        }
        if (window == 0) {
            throw new IllegalStateException("Could not create Vulkan terrain window");
        }
        VulkanTerrainPreview preview = new VulkanTerrainPreview(window);
        try {
            preview.presenter.initialize(Boolean.parseBoolean(System.getProperty("radiant.vulkan.validation", "true")));
            glfwShowWindow(window);
            preview.windowVisible = true;
            active = preview;
        } catch (RuntimeException | Error e) {
            preview.close();
            throw e;
        }
    }

    /** Returns whether this layer can skip its OpenGL upload. */
    public static boolean upload(RenderChunk chunk, CompiledChunk compiled, RenderLayer layer, WorldRenderer mesh) {
        VulkanTerrainPreview preview = active;
        if (preview == null || Config.isShaders() || Config.isMultiTexture()) {
            return false;
        }
        if (compiled.isLayerEmpty(layer)) {
            return TERRAIN_ONLY;
        }
        ByteBuffer vertices = mesh.getByteBuffer();
        int vertexCount = mesh.getVertexCount();
        if (mesh.getDrawMode() != GL_QUADS || mesh.getVertexFormat().getNextOffset() != VulkanTerrainMeshes.STRIDE
                || vertexCount == 0 || vertexCount % 4 != 0 || vertices.remaining() != vertexCount * VulkanTerrainMeshes.STRIDE) {
            throw new IllegalStateException("Unexpected chunk vertex buffer for Vulkan terrain");
        }
        Mesh[] layers = preview.sections.computeIfAbsent(chunk, unused -> new Mesh[LAYERS.length]);
        int index = layer.ordinal();
        Mesh previous = layers[index];
        if (previous != null) {
            preview.terrain.retire(previous.slice);
        }
        layers[index] = new Mesh(compiled, preview.terrain.upload(vertices));
        ClientBenchmark.recordTerrainUpload(true);
        preview.skippedOpenGlUpload |= TERRAIN_ONLY;
        return TERRAIN_ONLY;
    }

    public static boolean needsOpenGlTerrainRebuild() {
        return rebuildOpenGlTerrain;
    }

    public static boolean replacesTerrainDraws() {
        return TERRAIN_ONLY && active != null && active.pendingFrame != null;
    }

    public static void remove(RenderChunk chunk) {
        VulkanTerrainPreview preview = active;
        if (preview != null) {
            preview.retire(preview.sections.remove(chunk));
        }
    }

    private void retire(Mesh[] layers) {
        if (layers != null) {
            for (Mesh mesh : layers) {
                if (mesh != null) {
                    terrain.retire(mesh.slice);
                }
            }
        }
    }

    public static void invalidateTerrain() {
        rebuildOpenGlTerrain = false;
        VulkanTerrainPreview preview = active;
        if (preview != null) {
            preview.skippedOpenGlUpload = false;
            preview.pendingFrame = null;
            preview.sections.values().forEach(preview::retire);
            preview.sections.clear();
            preview.atlasDirty = true;
            preview.terrain.setAtlas(null);
        }
    }

    public static void updateSprite(TextureAtlasSprite sprite, int[][] pixels) {
        VulkanTerrainPreview preview = active;
        if (preview == null || preview.atlasDirty) {
            return;
        }
        String name = sprite.getIconName();
        if (Minecraft.get().getTextureMapBlocks().getAtlasSprite(name) == sprite) {
            preview.terrain.updateSprite(name, sprite.getOriginX(), sprite.getOriginY(),
                    sprite.getIconWidth(), sprite.getIconHeight(), pixels);
        }
    }

    public static void leaveWorld() {
        invalidateTerrain();
        if (active != null) {
            glfwHideWindow(active.window);
            active.windowVisible = false;
        }
    }

    public static Extraction extract(Minecraft mc, Entity view, double x, double y, double z, float partialTicks,
                                     int cloudTick, Matrix4fc projection, Matrix4fc viewMatrix, int[] lightmap,
                                     GlStateManager.FogParameters skyFog) {
        VulkanTerrainPreview preview = active;
        if (preview == null) {
            return null;
        }
        preview.objects.reset();
        preview.objects.setWorldProjection(projection);
        if (preview.atlasDirty) {
            preview.terrain.setAtlas(buildAtlas(mc.getTextureMapBlocks()));
            preview.terrain.setEnvironmentImages(loadEnvironmentImages());
            preview.atlasDirty = false;
        }
        Matrix4f viewProjection = projection.mul(viewMatrix, new Matrix4f());
        VulkanEnvironment.Frame environment = captureEnvironment(mc, view, x, y, z, partialTicks, cloudTick, skyFog);
        return new Extraction(preview, x, y, z, viewProjection, lightmap.clone(), captureFog(mc),
                environment, Config.isRenderRegions(), mc.displayWidth, mc.displayHeight);
    }

    public static boolean beginObjects(int[] lightmap, int atlas) {
        return beginObjects(lightmap, atlas, VulkanObjectCapture.Phase.ENTITIES);
    }

    public static boolean beginParticles(int[] lightmap, int atlas) {
        return beginObjects(lightmap, atlas, VulkanObjectCapture.Phase.LIT_PARTICLES);
    }

    public static boolean beginSelection(int[] lightmap, int atlas) {
        return beginObjects(lightmap, atlas, VulkanObjectCapture.Phase.SELECTION);
    }

    public static boolean beginBlockDamage(int[] lightmap, int atlas) {
        return beginObjects(lightmap, atlas, VulkanObjectCapture.Phase.BLOCK_DAMAGE);
    }

    public static boolean beginWorldBorder(WorldBorderStage stage) {
        VulkanTerrainPreview preview = active;
        if (preview == null || preview.pendingFrame == null) {
            return false;
        }
        preview.objects.beginWorldBorder(preview.pendingFrame.lightmap(), stage);
        return true;
    }

    public static boolean beginPrecipitation(int[] lightmap, int atlas) {
        return beginObjects(lightmap, atlas, VulkanObjectCapture.Phase.RAIN);
    }

    public static boolean beginFirstPerson(int[] lightmap, int atlas, Matrix4fc projection) {
        return beginObjects(lightmap, atlas, VulkanObjectCapture.Phase.FIRST_PERSON, projection);
    }

    public static boolean beginCameraOverlays(int[] lightmap, int atlas, Matrix4fc projection) {
        return beginObjects(lightmap, atlas, VulkanObjectCapture.Phase.CAMERA_OVERLAYS, projection);
    }

    public static boolean beginWorldDirections() {
        VulkanTerrainPreview preview = active;
        if (preview == null || preview.pendingFrame == null) {
            return false;
        }
        preview.objects.begin(preview.pendingFrame.lightmap(), 0, VulkanObjectCapture.Phase.WORLD_DIRECTIONS,
                GlStateManager.projectionMatrix());
        return true;
    }

    public static boolean beginHud(int[] lightmap, int atlas) {
        return beginObjects(lightmap, atlas, VulkanObjectCapture.Phase.HUD);
    }

    public static boolean beginScreen(int[] lightmap, int atlas) {
        return beginObjects(lightmap, atlas, VulkanObjectCapture.Phase.SCREEN);
    }

    public static boolean beginAchievement(int atlas) {
        VulkanTerrainPreview preview = active;
        if (preview == null || preview.pendingFrame == null) {
            return false;
        }
        preview.objects.begin(preview.pendingFrame.lightmap(), atlas, VulkanObjectCapture.Phase.ACHIEVEMENT, null);
        return true;
    }

    private static boolean beginObjects(int[] lightmap, int atlas, VulkanObjectCapture.Phase phase) {
        return beginObjects(lightmap, atlas, phase, null);
    }

    private static boolean beginObjects(int[] lightmap, int atlas, VulkanObjectCapture.Phase phase, Matrix4fc projection) {
        VulkanTerrainPreview preview = active;
        if (preview == null || preview.pendingFrame == null) {
            return false;
        }
        preview.objects.begin(lightmap, atlas, phase, projection);
        return true;
    }

    public static void registerObjectTexture(int texture, NativeImage image, boolean blur, boolean clamp) {
        VulkanObjectCapture.registerTexture(texture, image, blur, clamp);
        invalidateObjectTextureImage(texture);
    }

    public static void registerObjectTexture(int texture, int width, int height, int[] argb) {
        VulkanObjectCapture.registerTexture(texture, width, height, argb);
        invalidateObjectTextureImage(texture);
    }

    public static void invalidateObjectTexture(int texture) {
        VulkanObjectCapture.forgetTexture(texture);
        invalidateObjectTextureImage(texture);
    }

    public static void setObjectTextureClamped(int texture, boolean clamp) {
        if (VulkanObjectCapture.setTextureClamped(texture, clamp)) {
            invalidateObjectTextureImage(texture);
        }
    }

    private static void invalidateObjectTextureImage(int texture) {
        VulkanTerrainPreview preview = active;
        if (preview != null) {
            preview.objects.invalidateTexture(texture);
        }
    }

    private static VulkanEnvironment.Images loadEnvironmentImages() {
        try {
            return new VulkanEnvironment.Images(NativeImage.loadFromResourceLocation(RenderGlobal.LOCATION_CLOUDS_PNG),
                    NativeImage.loadFromResourceLocation(RenderGlobal.LOCATION_SUN_PNG),
                    NativeImage.loadFromResourceLocation(RenderGlobal.LOCATION_MOON_PHASES_PNG),
                    NativeImage.loadFromResourceLocation(RenderGlobal.LOCATION_END_SKY_PNG));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load Vulkan sky and cloud textures", e);
        }
    }

    private static VulkanEnvironment.Frame captureEnvironment(Minecraft mc, Entity view, double x, double y, double z,
                                                               float partialTicks, int cloudTick, GlStateManager.FogParameters skyFog) {
        var world = mc.world;
        if (world.provider.getDimensionId() == 1 && Config.isSkyEnabled()) {
            Vec3 tint = new Vec3(40.0 / 255.0, 40.0 / 255.0, 40.0 / 255.0);
            if (Config.isCustomColors()) {
                tint = CustomColors.getWorldSkyColor(tint, world, view, partialTicks);
            }
            return VulkanEnvironment.Frame.dimensionSky(VulkanEnvironment.SkyMode.END,
                    (float) tint.xCoord(), (float) tint.yCoord(), (float) tint.zCoord());
        }
        if (!world.provider.isSurfaceWorld()) {
            return VulkanEnvironment.Frame.dimensionSky(VulkanEnvironment.SkyMode.FOG, 0, 0, 0);
        }
        Vec3 sky = CustomColors.getSkyColor(world.getSkyColor(view, partialTicks), world, view.posX, view.posY + 1, view.posZ);
        float angle = world.getCelestialAngle(partialTicks);
        double radians = angle * Math.TAU;
        float rainVisibility = 1 - world.getRainStrength(partialTicks);
        float stars = Config.isStarsEnabled() && !CustomSky.hasSkyLayers(world)
                ? world.getStarBrightness(partialTicks) * rainVisibility : 0;
        float[] sunrise = Config.isSunMoonEnabled() ? world.provider.calcSunriseSunsetColors(angle, partialTicks) : null;
        int mode = Config.isCloudsOff() || mc.gameSettings.renderDistanceChunks < 4 ? 0 : Config.isCloudsFancy() ? 2 : 1;
        Vec3 cloudColor = mode == 0 ? new Vec3(0, 0, 0) : world.getCloudColour(partialTicks);
        double period = mode == 2 ? 3072.0 : 2048.0;
        float cloudX = wrapCloudOffset(x + (cloudTick + partialTicks) * 0.03, period);
        float cloudZ = wrapCloudOffset(z + (mode == 2 ? 3.96 : 0.0), period);
        float cloudHeight = (float) (world.provider.getCloudHeight() + mc.gameSettings.ofCloudsHeight * 128 + 0.33 - y);
        int chunks = mc.gameSettings.renderDistanceChunks;
        float skyExtent = Config.isSkyEnabled() ? (chunks / 4 + 2) * 64 : 0;
        float lowerHeight = (float) (world.getHorizon() - mc.player.getPositionEyes(partialTicks).yCoord());
        Vec3 lowerColor = sky;
        if (chunks <= 4) {
            lowerColor = new Vec3(mc.entityRenderer.fogColorRed, mc.entityRenderer.fogColorGreen, mc.entityRenderer.fogColorBlue);
        } else if (world.provider.isSkyColored()) {
            lowerColor = new Vec3(sky.xCoord() * 0.2 + 0.04, sky.yCoord() * 0.2 + 0.04, sky.zCoord() * 0.6 + 0.1);
        }
        return new VulkanEnvironment.Frame(VulkanEnvironment.SkyMode.OVERWORLD, skyExtent,
                (float) sky.xCoord(), (float) sky.yCoord(), (float) sky.zCoord(),
                stars, sunrise == null ? 0 : sunrise[0], sunrise == null ? 0 : sunrise[1],
                sunrise == null ? 0 : sunrise[2], sunrise == null ? 0 : sunrise[3],
                (float) Math.sin(radians), (float) Math.cos(radians), Config.isSunMoonEnabled() ? rainVisibility : 0, world.getMoonPhase(),
                mode, mc.entityRenderer.cloudsBeforeTerrain(view),
                (float) cloudColor.xCoord(), (float) cloudColor.yCoord(), (float) cloudColor.zCoord(), cloudX, cloudZ, cloudHeight,
                mc.entityRenderer.cloudClipDistance(), lowerHeight, lowerColor, skyFog == null ? 0 : fogMode(skyFog), skyFog);
    }

    private static float wrapCloudOffset(double position, double period) {
        return (float) (position - Math.floor(position / period) * period);
    }

    private static VulkanTerrainScene.Fog captureFog(Minecraft mc) {
        var renderer = mc.entityRenderer;
        var parameters = GlStateManager.fogParameters();
        int mode = renderer.fogStandard && Config.isFogOff() ? 0 : fogMode(parameters);
        return new VulkanTerrainScene.Fog(renderer.fogColorRed, renderer.fogColorGreen, renderer.fogColorBlue,
                mode, parameters.start(), parameters.end(), parameters.density());
    }

    private static int fogMode(GlStateManager.FogParameters parameters) {
        return parameters.enabled() ? switch (parameters.mode()) {
            case GL_LINEAR -> 1;
            case GL_EXP -> 2;
            case GL_EXP2 -> 3;
            default -> 0;
        } : 0;
    }

    private static VulkanTerrainScene.Atlas buildAtlas(TextureMap texture) {
        int width = texture.atlasWidth;
        int height = texture.atlasHeight;
        if (width <= 0 || height <= 0) {
            throw new IllegalStateException("Block atlas has not been stitched");
        }
        ByteBuffer[] levels = new ByteBuffer[texture.getMipmapLevels() + 1];
        try {
            for (int level = 0; level < levels.length; level++) {
                int mipWidth = width >> level;
                int mipHeight = height >> level;
                levels[level] = memCalloc(Math.multiplyExact(Math.multiplyExact(mipWidth, mipHeight), 4));
            }
            for (TextureAtlasSprite sprite : texture.getUploadedSprites()) {
                int[][] pixels = sprite.getUploadedFrameTextureData();
                for (int level = 0; level < Math.min(levels.length, pixels.length); level++) {
                    int mipWidth = width >> level;
                    int spriteWidth = sprite.getIconWidth() >> level;
                    int spriteHeight = sprite.getIconHeight() >> level;
                    int x = sprite.getOriginX() >> level;
                    int y = sprite.getOriginY() >> level;
                    ByteBuffer mip = levels[level];
                    for (int row = 0; row < spriteHeight; row++) {
                        mip.position(((y + row) * mipWidth + x) * 4);
                        VulkanAtlasUpdates.writeRgba(mip, pixels[level], row * spriteWidth, spriteWidth);
                    }
                }
            }
            for (ByteBuffer mip : levels) {
                mip.clear();
            }
            return new VulkanTerrainScene.Atlas(width, height, levels);
        } catch (RuntimeException | Error e) {
            for (ByteBuffer mip : levels) {
                memFree(mip);
            }
            throw e;
        }
    }

    public static void present() {
        VulkanTerrainPreview preview = active;
        if (preview == null) {
            return;
        }
        if (glfwWindowShouldClose(preview.window)) {
            manuallyClosed = true;
            shutdown();
            return;
        }
        VulkanTerrainScene.Frame frame = preview.pendingFrame;
        preview.pendingFrame = null;
        if (frame == null) {
            return;
        }
        preview.terrain.setFrame(frame);
        try {
            VulkanTerrainScene.Fog fog = frame.fog();
            VulkanPresenter.FrameCapture capture = null;
            if (ClientBenchmark.COMPARE_FRAMES && !TERRAIN_ONLY) {
                Minecraft mc = Minecraft.get();
                if (ClientBenchmark.wantsFrameComparison(mc)) {
                    float partialTicks = Config.renderPartialTicks;
                    capture = new VulkanPresenter.FrameCapture(frame.width(), frame.height(),
                            image -> ClientBenchmark.saveFrameComparison(mc, partialTicks, image));
                }
            }
            if (preview.presenter.present(fog.red(), fog.green(), fog.blue(), capture)) {
                preview.frames++;
                preview.drawCalls += preview.terrain.drawCalls();
                ClientBenchmark.recordVulkanTerrainFrame(preview.terrain.drawCalls(), preview.terrain.quads(),
                        preview.terrain.cutoutQuads(), preview.terrain.translucentQuads());
                ClientBenchmark.recordVulkanEnvironmentFrame(preview.terrain.skyDrawn(), preview.terrain.cloudsDrawn(),
                        preview.terrain.endSkyDrawn(), preview.terrain.fogBackgroundDrawn(), (int) fog.mode());
                ClientBenchmark.recordVulkanObjectFrame(preview.objects.entityTriangles(), preview.objects.blockEntityTriangles(),
                        preview.objects.blockEntityGlyphs());
                ClientBenchmark.recordVulkanEntityShadows(preview.objects.entityShadowQuads());
                ClientBenchmark.recordVulkanEntityFire(preview.objects.entityFireQuads());
                ClientBenchmark.recordVulkanEntityHitboxes(preview.objects.entityHitboxSegments());
                ClientBenchmark.recordVulkanEntityLeashes(preview.objects.entityLeashTriangles());
                ClientBenchmark.recordVulkanEntityNameTags(preview.objects.entityNameTagGlyphs(),
                        preview.objects.entityNameTagThroughWallGlyphs());
                ClientBenchmark.recordVulkanEntityTint(preview.objects.entityTintTriangles());
                ClientBenchmark.recordVulkanEntityEyes(preview.objects.entityEyeTriangles());
                ClientBenchmark.recordVulkanParticleFrame(preview.objects.particleTriangles(), preview.objects.terrainParticleTriangles(),
                        preview.objects.translucentParticleTriangles(), preview.objects.litParticleTriangles());
                ClientBenchmark.recordVulkanPrecipitationFrame(preview.objects.rainQuads(), preview.objects.snowQuads());
                ClientBenchmark.recordVulkanFirstPersonFrame(preview.objects.handTriangles(), preview.objects.heldItemTriangles());
                ClientBenchmark.recordVulkanItemGlint(preview.objects.heldGlintTriangles(), preview.objects.screenGlintTriangles());
                ClientBenchmark.recordVulkanEntityOutlines(preview.objects.outlineTriangles());
                var world = Minecraft.get().world;
                ClientBenchmark.recordVulkanLightning(preview.objects.lightningTriangles(),
                        preview.terrain.skyDrawn() && world != null && world.getLastLightningBolt() > 0);
                ClientBenchmark.recordVulkanArmorGlint(preview.objects.armorGlintTriangles());
                ClientBenchmark.recordVulkanBlockInteraction(preview.objects.selectionSegments(), preview.objects.blockDamageQuads());
                ClientBenchmark.recordVulkanWorldBorder(preview.objects.worldBorderQuads(), preview.objects.worldBorderStage());
                ClientBenchmark.recordVulkanCameraOverlays(
                        preview.objects.cameraOverlayQuads(VulkanObjectCapture.CameraOverlay.BLOCK),
                        preview.objects.cameraOverlayQuads(VulkanObjectCapture.CameraOverlay.WATER),
                        preview.objects.cameraOverlayQuads(VulkanObjectCapture.CameraOverlay.FIRE));
                ClientBenchmark.recordVulkanUiFrame(preview.objects.hudTriangles(), preview.objects.hudItemTriangles(),
                        preview.objects.screenTriangles(), preview.objects.screenItemTriangles(), preview.objects.screenGlyphs());
            }
        } finally {
            preview.terrain.setFrame(null);
        }
    }

    public static void shutdown() {
        VulkanTerrainPreview preview = active;
        active = null;
        if (preview != null) {
            rebuildOpenGlTerrain |= preview.skippedOpenGlUpload;
            preview.close();
        }
    }

    @Override
    public void close() {
        try {
            presenter.close();
        } finally {
            objects.close();
            glfwDestroyWindow(window);
            LOGGER.info("Vulkan terrain preview presented {} frames and {} terrain draws with {} validation errors",
                    frames, drawCalls, presenter.validationErrors());
            LOGGER.info("Vulkan object textures loaded from CPU pixels: {} ({} dynamic); missing CPU sources: {}",
                    objects.cpuTextureLoads(), objects.dynamicTextureLoads(), objects.missingTextureSources());
        }
    }

    private record Mesh(CompiledChunk compiled, VulkanTerrainMeshes.Slice slice) {
    }

    public static final class Extraction {
        private final VulkanTerrainPreview preview;
        private final double x;
        private final double y;
        private final double z;
        private final Matrix4f matrix;
        private final int[] lightmap;
        private final VulkanTerrainScene.Fog fog;
        private final VulkanEnvironment.Frame environment;
        private final boolean renderRegions;
        private final int width;
        private final int height;
        private final List<VulkanTerrainScene.Draw> solid = new ArrayList<>();
        private final List<VulkanTerrainScene.Draw> cutoutMipped = new ArrayList<>();
        private final List<VulkanTerrainScene.Draw> cutout = new ArrayList<>();
        private final List<VulkanTerrainScene.Draw> translucent = new ArrayList<>();

        private Extraction(VulkanTerrainPreview preview, double x, double y, double z, Matrix4f matrix,
                           int[] lightmap, VulkanTerrainScene.Fog fog, VulkanEnvironment.Frame environment,
                           boolean renderRegions, int width, int height) {
            this.preview = preview;
            this.x = x;
            this.y = y;
            this.z = z;
            this.matrix = matrix;
            this.lightmap = lightmap;
            this.fog = fog;
            this.environment = environment;
            this.renderRegions = renderRegions;
            this.width = width;
            this.height = height;
        }

        public void add(RenderChunk chunk) {
            Mesh[] layers = preview.sections.get(chunk);
            if (layers == null) {
                return;
            }
            BlockPos origin = chunk.getPosition();
            float offsetX = (float) ((renderRegions ? chunk.regionX : origin.getX()) - x);
            float offsetY = (float) ((renderRegions ? 0 : origin.getY()) - y);
            float offsetZ = (float) ((renderRegions ? chunk.regionZ : origin.getZ()) - z);
            CompiledChunk compiled = chunk.getCompiledChunk();
            for (RenderLayer layer : LAYERS) {
                int index = layer.ordinal();
                Mesh mesh = layers[index];
                if (mesh != null && mesh.compiled == compiled && !compiled.isLayerEmpty(layer)) {
                    List<VulkanTerrainScene.Draw> draws = switch (layer) {
                        case SOLID -> solid;
                        case CUTOUT_MIPPED -> cutoutMipped;
                        case CUTOUT -> cutout;
                        case TRANSLUCENT -> translucent;
                    };
                    draws.add(new VulkanTerrainScene.Draw(mesh.slice, offsetX, offsetY, offsetZ, index));
                }
            }
        }

        public void finish() {
            // Submit each opaque layer across all sections before advancing to the next layer.
            var opaque = new ArrayList<VulkanTerrainScene.Draw>(solid.size() + cutoutMipped.size() + cutout.size());
            opaque.addAll(solid);
            opaque.addAll(cutoutMipped);
            opaque.addAll(cutout);
            // OpenGL traverses visible sections in reverse for translucency. Their quads are already sorted.
            preview.pendingFrame = new VulkanTerrainScene.Frame(matrix, lightmap, fog, environment,
                    List.copyOf(opaque), List.copyOf(translucent.reversed()), width, height);
        }
    }
}
