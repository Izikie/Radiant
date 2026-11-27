package net.radiant.render.vulkan;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.RenderLayer;
import net.optifine.Config;
import net.radiant.render.opengl.OpenGlMipmapFilter;
import net.radiant.render.terrain.TerrainDrawSubmitter;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL13;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
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
    static final boolean COMPARE = Boolean.getBoolean("radiant.vulkan.compareTerrain");
    private static final Logger LOGGER = LoggerFactory.getLogger(VulkanTerrainPreview.class);
    private static final RenderLayer[] LAYERS = RenderLayer.values();
    private static VulkanTerrainPreview active;
    private static boolean manuallyClosed;
    private static boolean unsupportedSettings;

    private final long window;
    private final VulkanTerrainScene terrain = new VulkanTerrainScene();
    private final VulkanPresenter presenter;
    private final Map<RenderChunk, VulkanTerrainMeshes.Slice[]> sections = new IdentityHashMap<>();
    private boolean atlasDirty = true;
    private boolean windowVisible;
    private int sourceWidth;
    private int sourceHeight;
    private Extraction frame;
    private boolean frameStarted;
    private ItemFeatureRenderer.Batch itemBatch;
    private boolean specialItem;
    private float clearRed, clearGreen, clearBlue;
    private TerrainComparison comparison;
    private long frames;
    private long drawCalls;

    private VulkanTerrainPreview(long window, int width, int height) {
        this.window = window;
        this.sourceWidth = width;
        this.sourceHeight = height;
        this.presenter = new VulkanPresenter(window, terrain, COMPARE);
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
        if (active != null) {
            if (mc.displayWidth != active.sourceWidth || mc.displayHeight != active.sourceHeight) {
                active.sourceWidth = mc.displayWidth;
                active.sourceHeight = mc.displayHeight;
                glfwSetWindowSize(active.window, active.sourceWidth, active.sourceHeight);
            }
            if (!active.windowVisible) {
                glfwShowWindow(active.window);
                active.windowVisible = true;
            }
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
        VulkanTerrainPreview preview = new VulkanTerrainPreview(window, mc.displayWidth, mc.displayHeight);
        try {
            preview.presenter.initialize(Boolean.parseBoolean(System.getProperty("radiant.vulkan.validation", "false")));
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
        if (preview == null) {
            return;
        }
        if (Config.isShaders() || Config.isMultiTexture()) {
            return;
        }
        // An empty display-list upload does not target the chunk's layer list.
        // VBO uploads replace the layer buffer even when the upload is empty.
        if (!OpenGlHelper.useVbo() && compiled.isLayerEmpty(layer)) {
            return;
        }
        ByteBuffer vertices = mesh.getByteBuffer();
        int vertexCount = mesh.getVertexCount();
        if (mesh.getDrawMode() != GL_QUADS || mesh.getVertexFormat().getNextOffset() != VulkanTerrainMeshes.STRIDE
                || vertexCount % 4 != 0 || vertices.remaining() != vertexCount * VulkanTerrainMeshes.STRIDE) {
            throw new IllegalStateException("Unexpected chunk vertex buffer for Vulkan terrain");
        }
        VulkanTerrainMeshes.Slice[] layers = preview.sections.computeIfAbsent(chunk,
                unused -> new VulkanTerrainMeshes.Slice[LAYERS.length]);
        int index = layer.ordinal();
        VulkanTerrainMeshes.Slice previous = layers[index];
        layers[index] = vertexCount == 0 ? null : preview.terrain.upload(vertices);
        if (previous != null) {
            preview.terrain.retire(previous);
        }
    }

    public static void remove(RenderChunk chunk) {
        VulkanTerrainPreview preview = active;
        if (preview != null) {
            preview.retire(preview.sections.remove(chunk));
        }
    }

    private void retire(VulkanTerrainMeshes.Slice[] layers) {
        if (layers != null) {
            for (VulkanTerrainMeshes.Slice mesh : layers) {
                if (mesh != null) {
                    terrain.retire(mesh);
                }
            }
        }
    }

    public static void invalidateTerrain() {
        VulkanTerrainPreview preview = active;
        if (preview != null) {
            preview.frame = null;
            preview.sections.values().forEach(preview::retire);
            preview.sections.clear();
            preview.atlasDirty = true;
            preview.terrain.setAtlas(null);
            preview.terrain.clouds.invalidateTexture();
            preview.terrain.weather.invalidateTextures();
            preview.terrain.particles.invalidateTexture();
            preview.terrain.items.invalidateTextures();
            preview.terrain.border.invalidateTexture();
            preview.terrain.sun.invalidateTexture();
            preview.terrain.moon.invalidateTexture();
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
            active.frameStarted = false;
            invalidateTerrain();
            glfwHideWindow(active.window);
            active.windowVisible = false;
        }
    }

    public static void beginWorld(Minecraft mc) {
        openIfNeeded(mc);
        VulkanTerrainPreview preview = active;
        if (preview != null) {
            preview.clearRed = mc.entityRenderer.fogColorRed;
            preview.clearGreen = mc.entityRenderer.fogColorGreen;
            preview.clearBlue = mc.entityRenderer.fogColorBlue;
            preview.terrain.upperSky = null;
            preview.terrain.lowerSky = null;
            preview.terrain.twilight = null;
            preview.terrain.stars = null;
            preview.terrain.clouds.visible = false;
            preview.terrain.outline.bounds = null;
            preview.terrain.damage.vertices = null;
            preview.terrain.particles.clear();
            preview.terrain.items.clear();
            preview.terrain.weather.clear();
            preview.terrain.border.vertices = null;
            preview.terrain.sun.visible = false;
            preview.terrain.moon.visible = false;
            preview.terrain.sourceWidth = preview.sourceWidth;
            preview.terrain.sourceHeight = preview.sourceHeight;
            preview.frameStarted = true;
            preview.frame = null;
            if (preview.comparison != null) preview.comparison.end();
            if (COMPARE && preview.frames % 47 == 0 && OpenGlHelper.isFramebufferEnabled()) {
                if (preview.comparison == null) preview.comparison = new TerrainComparison();
                preview.comparison.begin(preview.sourceWidth, preview.sourceHeight);
                preview.comparison.copyBackground();
            }
        }
    }

    public static void skyPlane(boolean upper, int radius, Runnable drawReference) {
        VulkanTerrainPreview preview = active;
        if (preview == null || !preview.frameStarted) return;
        var plane = VulkanSkyBackground.capture(upper, radius);
        if (upper) preview.terrain.upperSky = plane;
        else preview.terrain.lowerSky = plane;
        replaySky(drawReference);
    }

    public static void submitBlockOutline(AxisAlignedBB bounds) {
        VulkanTerrainPreview preview = active;
        if (preview == null || !preview.frameStarted) return;
        preview.terrain.outline.capture(bounds);
        if (preview.comparison != null && preview.comparison.active()) {
            preview.comparison.drawOverlay(() -> RenderGlobal.drawSelectionBoundingBox(bounds));
        }
    }

    public static void submitBlockDamage(WorldRenderer mesh) {
        VulkanTerrainPreview preview = active;
        if (preview == null || !preview.frameStarted || mesh.getVertexCount() == 0) return;
        int[] vertices = mesh.getVertexState().getRawBuffer();
        preview.terrain.damage.capture(vertices);
        if (preview.comparison != null && preview.comparison.active()) {
            preview.comparison.drawOverlay(vertices, DefaultVertexFormats.BLOCK);
        }
    }

    public static void withItemSubmissions(boolean pickup, Runnable render) {
        VulkanTerrainPreview preview = active;
        if (preview == null || !preview.frameStarted) {
            render.run();
            return;
        }
        var previous = preview.itemBatch;
        preview.itemBatch = pickup ? preview.terrain.items.pickups : preview.terrain.items.drops;
        try {
            render.run();
        } finally {
            preview.itemBatch = previous;
        }
    }

    public static void withSpecialItem(Runnable render) {
        VulkanTerrainPreview preview = active;
        if (preview == null || preview.itemBatch == null) {
            render.run();
            return;
        }
        boolean previous = preview.specialItem;
        preview.specialItem = true;
        try {
            render.run();
        } finally {
            preview.specialItem = previous;
        }
    }

    public static boolean capturingSpecialItem() {
        return active != null && active.specialItem;
    }

    public static void submitSpecialItemModel(int[] vertices, int displayList) {
        VulkanTerrainPreview preview = active;
        if (preview == null || !preview.specialItem || vertices.length == 0) return;
        preview.ensureAtlas(Minecraft.get());
        if (preview.comparison != null && preview.comparison.active()) {
            preview.comparison.drawOverlay(() -> GlStateManager.callList(displayList));
        }
        preview.terrain.items.submitSpecial(preview.itemBatch, vertices.clone());
    }

    public static void itemTextureDeleted(int texture) {
        if (active != null) active.terrain.items.textureDeleted(texture);
    }

    public static void submitExperienceOrb(WorldRenderer mesh) {
        VulkanTerrainPreview preview = active;
        if (preview == null || preview.itemBatch == null || mesh.getVertexCount() == 0) return;
        preview.ensureAtlas(Minecraft.get());
        int[] vertices = mesh.getVertexState().getRawBuffer();
        if (preview.comparison != null && preview.comparison.active()) {
            preview.comparison.drawOverlay(vertices, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
        }
        preview.terrain.items.submitOrb(preview.itemBatch, vertices);
    }

    public static void submitItemFire(WorldRenderer mesh) {
        VulkanTerrainPreview preview = active;
        if (preview == null || preview.itemBatch == null || mesh.getVertexCount() == 0) return;
        preview.ensureAtlas(Minecraft.get());
        int[] vertices = mesh.getVertexState().getRawBuffer();
        if (preview.comparison != null && preview.comparison.active()) {
            preview.comparison.drawOverlay(vertices, DefaultVertexFormats.POSITION_TEX);
        }
        preview.terrain.items.submitFire(preview.itemBatch, vertices);
    }

    public static void submitItemShadow(WorldRenderer mesh) {
        VulkanTerrainPreview preview = active;
        if (preview == null || preview.itemBatch == null || mesh.getVertexCount() == 0) return;
        preview.ensureAtlas(Minecraft.get());
        int[] vertices = mesh.getVertexState().getRawBuffer();
        if (preview.comparison != null && preview.comparison.active()) {
            preview.comparison.drawOverlay(vertices, DefaultVertexFormats.POSITION_TEX_COLOR);
        }
        preview.terrain.items.submitShadow(preview.itemBatch, vertices);
    }

    public static void submitItemModel(WorldRenderer mesh, boolean glint) {
        VulkanTerrainPreview preview = active;
        if (preview == null || preview.itemBatch == null || mesh.getVertexCount() == 0) return;
        preview.ensureAtlas(Minecraft.get());
        int[] vertices = mesh.getVertexState().getRawBuffer();
        if (preview.comparison != null && preview.comparison.active()) {
            preview.comparison.drawOverlay(vertices, DefaultVertexFormats.ITEM);
        }
        preview.terrain.items.submit(preview.itemBatch, vertices, glint);
    }

    public static void submitParticles(boolean blockAtlas, boolean depthWrite, WorldRenderer mesh) {
        VulkanTerrainPreview preview = active;
        if (preview == null || !preview.frameStarted || mesh.getVertexCount() == 0) return;
        preview.ensureAtlas(Minecraft.get());
        int[] vertices = mesh.getVertexState().getRawBuffer();
        preview.terrain.particles.add(blockAtlas, depthWrite, vertices);
        if (preview.comparison != null && preview.comparison.active()) {
            preview.comparison.drawOverlay(vertices, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
        }
    }

    public static void worldBorder(WorldRenderer mesh) {
        VulkanTerrainPreview preview = active;
        if (preview == null || !preview.frameStarted || mesh.getVertexCount() == 0) return;
        int[] vertices = mesh.getVertexState().getRawBuffer();
        preview.terrain.border.capture(vertices);
        if (preview.comparison != null && preview.comparison.active()) {
            preview.comparison.drawOverlay(vertices, DefaultVertexFormats.POSITION_TEX);
        }
    }

    public static void precipitation(int texture, WorldRenderer mesh) {
        VulkanTerrainPreview preview = active;
        if (preview == null || !preview.frameStarted || mesh.getVertexCount() == 0) return;
        int[] vertices = mesh.getVertexState().getRawBuffer();
        preview.terrain.weather.add(texture, vertices);
        if (preview.comparison != null && preview.comparison.active()) preview.comparison.drawOverlay(vertices, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
    }

    public static void beginClouds(boolean afterTerrain) {
        if (active != null) active.terrain.clouds.afterTerrain = afterTerrain;
    }

    public static void clouds(byte[] vertices, boolean fancy, int colorPass, Runnable reference) {
        VulkanTerrainPreview preview = active;
        if (preview == null || !preview.frameStarted) return;
        if (colorPass >= 0) preview.terrain.clouds.capture(vertices, fancy, colorPass);
        if (preview.comparison != null && preview.comparison.active()) {
            preview.comparison.drawClouds(reference, preview.terrain.clouds.afterTerrain);
        }
    }

    public static void stars(byte[] positions, float brightness, Runnable drawReference) {
        if (active == null || !active.frameStarted) return;
        active.terrain.stars = VulkanSkyBackground.captureStars(positions, brightness);
        replaySky(drawReference);
    }

    public static void captureCelestialBody(boolean sun, ByteBuffer vertices) {
        if (active != null && active.frameStarted) {
            (sun ? active.terrain.sun : active.terrain.moon).capture(vertices);
        }
    }

    public static void captureTwilight(ByteBuffer vertices) {
        if (active != null && active.frameStarted) active.terrain.twilight = VulkanSkyBackground.captureTwilight(vertices);
    }

    public static void replaySky(Runnable drawReference) {
        if (active != null && active.frameStarted && active.comparison != null && active.comparison.active()) {
            active.comparison.drawSky(drawReference);
        }
    }

    static void drawLayer(Minecraft mc, double x, double y, double z, RenderLayer layer, List<RenderChunk> sections,
                          TerrainDrawSubmitter reference) {
        VulkanTerrainPreview preview = active;
        if (preview == null || !preview.windowVisible) return;
        if (preview.frame == null) {
            preview.frame = preview.extract(mc, x, y, z);
        }
        int filter = glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER);
        boolean mipmapped = filter != GL_NEAREST && filter != GL_LINEAR;
        for (RenderChunk chunk : sections) preview.frame.add(chunk, layer, mipmapped);
        if (preview.comparison != null && preview.comparison.active()) {
            preview.comparison.draw(reference, layer, sections);
        }
    }

    private void ensureAtlas(Minecraft mc) {
        if (atlasDirty) {
            terrain.setAtlas(captureAtlas(mc.getTextureMapBlocks()));
            atlasDirty = false;
        }
    }

    private Extraction extract(Minecraft mc, double x, double y, double z) {
        ensureAtlas(mc);
        return new Extraction(this, x, y, z, mc.entityRenderer.copyTerrainLightmap(), captureFog(mc),
                Config.isRenderRegions());
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
            return new VulkanTerrainScene.Atlas(width, height, levels.toArray(ByteBuffer[]::new),
                    OpenGlMipmapFilter.measure(width, height, levels.size()));
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
        if (!preview.frameStarted) return;
        preview.frameStarted = false;
        // A world frame with no submitted sections still replaces the previous terrain image.
        VulkanTerrainScene.Frame frame = preview.frame == null ? null : preview.frame.finish();
        preview.frame = null;
        var compare = preview.comparison;
        var capture = compare != null && compare.active()
                ? new VulkanPresenter.FrameCapture(preview.sourceWidth, preview.sourceHeight, compare::compare) : null;
        int[] itemLightmap = frame == null && !preview.terrain.items.isEmpty()
                ? Minecraft.get().entityRenderer.copyTerrainLightmap() : null;
        preview.terrain.setFrame(frame, itemLightmap);
        try {
            if (preview.presenter.present(preview.clearRed, preview.clearGreen, preview.clearBlue, capture)) {
                preview.frames++;
                preview.drawCalls += preview.terrain.drawCalls();
            }
        } finally {
            if (compare != null) compare.end();
            preview.terrain.setFrame(null, null);
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
            if (comparison != null) comparison.close();
            presenter.close();
        } finally {
            glfwDestroyWindow(window);
            LOGGER.info("Vulkan terrain preview presented {} frames and {} terrain draws with {} validation errors",
                    frames, drawCalls, presenter.validationErrors());
        }
    }

    private static final class Extraction {
        private final VulkanTerrainPreview preview;
        private final double x;
        private final double y;
        private final double z;

        private final int[] lightmap;
        private final VulkanTerrainScene.Fog fog;
        private final boolean renderRegions;
        private final List<VulkanTerrainScene.Draw> opaque = new ArrayList<>();
        private final List<VulkanTerrainScene.Draw> translucent = new ArrayList<>();

        private Extraction(VulkanTerrainPreview preview, double x, double y, double z,
                           int[] lightmap, VulkanTerrainScene.Fog fog, boolean renderRegions) {
            this.preview = preview;
            this.x = x;
            this.y = y;
            this.z = z;

            this.lightmap = lightmap;
            this.fog = fog;
            this.renderRegions = renderRegions;
        }

        void add(RenderChunk chunk, RenderLayer layer, boolean mipmapped) {
            // The shared submission selects visible layers. Each GL layer starts using its
            // uploaded vertices immediately, even while other layers of the section are rebuilding.
            VulkanTerrainMeshes.Slice[] layers = preview.sections.get(chunk);
            VulkanTerrainMeshes.Slice mesh = layers == null ? null : layers[layer.ordinal()];
            if (mesh == null) return;
            BlockPos origin = chunk.getPosition();
            float offsetX = (float) ((renderRegions ? chunk.regionX : origin.getX()) - x);
            float offsetY = (float) ((renderRegions ? 0 : origin.getY()) - y);
            float offsetZ = (float) ((renderRegions ? chunk.regionZ : origin.getZ()) - z);
            var draws = layer == RenderLayer.TRANSLUCENT ? translucent : opaque;
            Vector4f eyeZ = new Vector4f();
            Matrix4f transform = captureTransform(chunk, offsetX, offsetY, offsetZ, eyeZ);
            draws.add(new VulkanTerrainScene.Draw(mesh, transform, eyeZ, layer.ordinal(), mipmapped));
        }

        // A float-rounded translation before projection moves texture and coverage boundaries,
        // particularly for region-relative coordinates. Use the same stack operations as the draw.
        private Matrix4f captureTransform(RenderChunk chunk, float x, float y, float z, Vector4f eyeZ) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer matrix = stack.mallocFloat(16);
                glPushMatrix();
                try {
                    glTranslatef(x, y, z);
                    if (!renderRegions || !OpenGlHelper.useVbo()) {
                        chunk.multModelviewMatrix();
                    }
                    glGetFloatv(GL_MODELVIEW_MATRIX, matrix);
                    eyeZ.set(matrix.get(2), matrix.get(6), matrix.get(10), matrix.get(14));
                } finally {
                    glPopMatrix();
                }
                glMatrixMode(GL_PROJECTION);
                glPushMatrix();
                try {
                    glMultMatrixf(matrix);
                    glGetFloatv(GL_PROJECTION_MATRIX, matrix);
                    return new Matrix4f().set(matrix);
                } finally {
                    glPopMatrix();
                    glMatrixMode(GL_MODELVIEW);
                }
            }
        }

        VulkanTerrainScene.Frame finish() {
            return new VulkanTerrainScene.Frame(lightmap, fog, opaque, translucent);
        }
    }
}
