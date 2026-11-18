package net.radiant.render.vulkan;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
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
import net.optifine.Config;
import net.optifine.CustomColors;
import net.radiant.benchmark.ClientBenchmark;
import net.radiant.util.NativeImage;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL13;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

/** Temporary presentation bridge while 1.8.9 runs its game loop and chunk builders. */
public final class VulkanTerrainPreview implements AutoCloseable {
    public static final boolean ENABLED = Boolean.getBoolean("radiant.vulkan.terrain");
    private static final Logger LOGGER = LoggerFactory.getLogger(VulkanTerrainPreview.class);
    private static final RenderLayer[] LAYERS = RenderLayer.values();
    private static final Comparator<VulkanTerrainScene.Draw> FAR_TO_NEAR =
            Comparator.comparingDouble(VulkanTerrainScene.Draw::distanceSquared).reversed();
    private static VulkanTerrainPreview active;
    private static boolean manuallyClosed;
    private static boolean unsupportedSettings;

    private final long window;
    private final VulkanObjectCapture objects = new VulkanObjectCapture();
    private final VulkanTerrainScene terrain = new VulkanTerrainScene(objects);
    private final VulkanPresenter presenter;
    private final Map<RenderChunk, Mesh[]> sections = new IdentityHashMap<>();
    private boolean atlasDirty = true;
    private boolean windowVisible;
    private VulkanTerrainScene.Frame pendingFrame;
    private long frames;
    private long drawCalls;

    private VulkanTerrainPreview(long window) {
        this.window = window;
        this.presenter = new VulkanPresenter(window, terrain);
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

    public static void upload(RenderChunk chunk, CompiledChunk compiled, RenderLayer layer, WorldRenderer mesh) {
        VulkanTerrainPreview preview = active;
        if (preview == null || compiled.isLayerEmpty(layer)) {
            return;
        }
        if (Config.isShaders() || Config.isMultiTexture()) {
            return;
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
        VulkanTerrainPreview preview = active;
        if (preview != null) {
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
        if (active != null) {
            invalidateTerrain();
            glfwHideWindow(active.window);
            active.windowVisible = false;
        }
    }

    public static Extraction extract(Minecraft mc, Entity view, double x, double y, double z, float partialTicks,
                                     int cloudTick, int[] lightmap) {
        VulkanTerrainPreview preview = active;
        if (preview == null) {
            return null;
        }
        preview.objects.reset();
        if (preview.atlasDirty) {
            preview.terrain.setAtlas(captureAtlas(mc.getTextureMapBlocks()));
            preview.terrain.setEnvironmentImages(loadEnvironmentImages());
            preview.atlasDirty = false;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer matrix = stack.mallocFloat(16);
            glGetFloatv(GL_PROJECTION_MATRIX, matrix);
            Matrix4f projection = new Matrix4f().set(matrix);
            glGetFloatv(GL_MODELVIEW_MATRIX, matrix);
            projection.mul(new Matrix4f().set(matrix));
            VulkanEnvironment.Frame environment = captureEnvironment(mc, view, x, y, z, partialTicks, cloudTick);
            return new Extraction(preview, x, y, z, view.getEyeHeight(), projection, lightmap.clone(), captureFog(mc),
                    environment, Config.isRenderRegions(), mc.displayWidth, mc.displayHeight);
        }
    }

    public static boolean beginObjects(int[] lightmap, int atlas) {
        VulkanTerrainPreview preview = active;
        if (preview == null || preview.pendingFrame == null) {
            return false;
        }
        preview.objects.begin(lightmap, atlas);
        return true;
    }

    public static void invalidateObjectTexture(int texture) {
        VulkanTerrainPreview preview = active;
        if (preview != null) {
            preview.objects.invalidateTexture(texture);
        }
    }

    private static VulkanEnvironment.Images loadEnvironmentImages() {
        try {
            return new VulkanEnvironment.Images(NativeImage.loadFromResourceLocation(RenderGlobal.LOCATION_CLOUDS_PNG),
                    NativeImage.loadFromResourceLocation(RenderGlobal.LOCATION_SUN_PNG),
                    NativeImage.loadFromResourceLocation(RenderGlobal.LOCATION_MOON_PHASES_PNG));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load Vulkan sky and cloud textures", e);
        }
    }

    private static VulkanEnvironment.Frame captureEnvironment(Minecraft mc, Entity view, double x, double y, double z,
                                                               float partialTicks, int cloudTick) {
        var world = mc.world;
        if (!world.provider.isSurfaceWorld()) {
            return null;
        }
        Vec3 sky = CustomColors.getSkyColor(world.getSkyColor(view, partialTicks), world, view.posX, view.posY + 1, view.posZ);
        float angle = world.getCelestialAngle(partialTicks);
        double radians = angle * Math.TAU;
        float rainVisibility = 1 - world.getRainStrength(partialTicks);
        float stars = Config.isStarsEnabled() ? world.getStarBrightness(partialTicks) * rainVisibility : 0;
        float[] sunrise = Config.isSunMoonEnabled() ? world.provider.calcSunriseSunsetColors(angle, partialTicks) : null;
        int mode = Config.isCloudsOff() || mc.gameSettings.renderDistanceChunks < 4 ? 0 : Config.isCloudsFancy() ? 2 : 1;
        Vec3 cloudColor = mode == 0 ? new Vec3(0, 0, 0) : world.getCloudColour(partialTicks);
        double period = mode == 2 ? 3072.0 : 2048.0;
        float cloudX = wrapCloudOffset(x + (cloudTick + partialTicks) * 0.03, period);
        float cloudZ = wrapCloudOffset(z + (mode == 2 ? 3.96 : 0.0), period);
        float cloudHeight = (float) (world.provider.getCloudHeight() + mc.gameSettings.ofCloudsHeight * 128 + 0.33 - y);
        return new VulkanEnvironment.Frame(Config.isSkyEnabled(), (float) sky.xCoord(), (float) sky.yCoord(), (float) sky.zCoord(),
                stars, sunrise == null ? 0 : sunrise[0], sunrise == null ? 0 : sunrise[1],
                sunrise == null ? 0 : sunrise[2], sunrise == null ? 0 : sunrise[3],
                (float) Math.sin(radians), (float) Math.cos(radians), Config.isSunMoonEnabled() ? rainVisibility : 0, world.getMoonPhase(),
                mode, (float) cloudColor.xCoord(), (float) cloudColor.yCoord(), (float) cloudColor.zCoord(), cloudX, cloudZ, cloudHeight);
    }

    private static float wrapCloudOffset(double position, double period) {
        return (float) (position - Math.floor(position / period) * period);
    }

    private static VulkanTerrainScene.Fog captureFog(Minecraft mc) {
        var renderer = mc.entityRenderer;
        int mode = 0;
        if (glIsEnabled(GL_FOG) && !(renderer.fogStandard && Config.isFogOff())) {
            mode = switch (glGetInteger(GL_FOG_MODE)) {
                case GL_LINEAR -> 1;
                case GL_EXP -> 2;
                case GL_EXP2 -> 3;
                default -> 0;
            };
        }
        return new VulkanTerrainScene.Fog(renderer.fogColorRed, renderer.fogColorGreen, renderer.fogColorBlue,
                mode, glGetFloat(GL_FOG_START), glGetFloat(GL_FOG_END), glGetFloat(GL_FOG_DENSITY));
    }

    private static VulkanTerrainScene.Atlas captureAtlas(TextureMap texture) {
        int activeUnit = glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        int previous = glGetInteger(GL_TEXTURE_BINDING_2D);
        List<ByteBuffer> levels = new ArrayList<>();
        try {
            GlStateManager.bindTexture(texture.getGlTextureId());
            int width = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH);
            int height = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT);
            if (width <= 0 || height <= 0) {
                throw new IllegalStateException("Block atlas has not been loaded");
            }
            int maximumLevels = 32 - Integer.numberOfLeadingZeros(Math.max(width, height));
            for (int level = 0; level < maximumLevels; level++) {
                int w = glGetTexLevelParameteri(GL_TEXTURE_2D, level, GL_TEXTURE_WIDTH);
                int h = glGetTexLevelParameteri(GL_TEXTURE_2D, level, GL_TEXTURE_HEIGHT);
                if (w == 0 || h == 0) {
                    break;
                }
                ByteBuffer pixels = memAlloc(Math.multiplyExact(Math.multiplyExact(w, h), 4));
                levels.add(pixels);
                glGetTexImage(GL_TEXTURE_2D, level, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            }
            return new VulkanTerrainScene.Atlas(width, height, levels.toArray(ByteBuffer[]::new));
        } catch (RuntimeException | Error e) {
            levels.forEach(pixels -> memFree(pixels));
            throw e;
        } finally {
            GlStateManager.bindTexture(previous);
            GlStateManager.setActiveTexture(activeUnit);
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
            if (preview.presenter.present(fog.red(), fog.green(), fog.blue())) {
                preview.frames++;
                preview.drawCalls += preview.terrain.drawCalls();
                ClientBenchmark.recordVulkanTerrainFrame(preview.terrain.drawCalls(), preview.terrain.quads(),
                        preview.terrain.cutoutQuads(), preview.terrain.translucentQuads());
                ClientBenchmark.recordVulkanEnvironmentFrame(preview.terrain.skyDrawn(), preview.terrain.cloudsDrawn());
                ClientBenchmark.recordVulkanObjectFrame(preview.objects.entityTriangles(), preview.objects.blockEntityTriangles(),
                        preview.objects.blockEntityGlyphs());
            }
        } finally {
            preview.terrain.setFrame(null);
        }
    }

    public static void shutdown() {
        VulkanTerrainPreview preview = active;
        active = null;
        if (preview != null) {
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
        }
    }

    private record Mesh(CompiledChunk compiled, VulkanTerrainMeshes.Slice slice) {
    }

    public static final class Extraction {
        private final VulkanTerrainPreview preview;
        private final double x;
        private final double y;
        private final double z;
        private final double eyeY;
        private final Matrix4f matrix;
        private final int[] lightmap;
        private final VulkanTerrainScene.Fog fog;
        private final VulkanEnvironment.Frame environment;
        private final boolean renderRegions;
        private final int width;
        private final int height;
        private final List<VulkanTerrainScene.Draw> opaque = new ArrayList<>();
        private final List<VulkanTerrainScene.Draw> translucent = new ArrayList<>();

        private Extraction(VulkanTerrainPreview preview, double x, double y, double z, float eyeHeight, Matrix4f matrix,
                           int[] lightmap, VulkanTerrainScene.Fog fog, VulkanEnvironment.Frame environment,
                           boolean renderRegions, int width, int height) {
            this.preview = preview;
            this.x = x;
            this.y = y;
            this.z = z;
            this.eyeY = y + eyeHeight;
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
                    if (layer == RenderLayer.TRANSLUCENT) {
                        double dx = origin.getX() + 8.0 - x;
                        double dy = origin.getY() + 8.0 - eyeY;
                        double dz = origin.getZ() + 8.0 - z;
                        // The legacy worker already sorts the quads within each uploaded section.
                        translucent.add(new VulkanTerrainScene.Draw(mesh.slice, offsetX, offsetY, offsetZ,
                                index, dx * dx + dy * dy + dz * dz));
                    } else {
                        opaque.add(new VulkanTerrainScene.Draw(mesh.slice, offsetX, offsetY, offsetZ, index, 0));
                    }
                }
            }
        }

        public void finish() {
            translucent.sort(FAR_TO_NEAR);
            preview.pendingFrame = new VulkanTerrainScene.Frame(matrix, lightmap, fog, environment,
                    List.copyOf(opaque), List.copyOf(translucent), width, height);
        }
    }
}
