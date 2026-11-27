package net.radiant.render.vulkan;

import org.joml.Matrix4fc;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFormatProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanTerrainScene implements VulkanScene {
    private final VulkanAtlasUpdates atlasUpdates = new VulkanAtlasUpdates();
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    private VkQueue queue;
    private VkCommandBuffer commands;
    private int depthFormat;
    private VulkanTerrainMeshes meshes;
    private VulkanTerrainPipeline pipeline;
    private VulkanSkyBackground skyPipeline;
    private VulkanCelestialPipeline celestial;
    private VulkanCloudPipeline cloudPipeline;
    private VulkanBlockOutlinePipeline outlinePipeline;
    final VulkanBlockOutlinePipeline.State outline = new VulkanBlockOutlinePipeline.State();
    private VulkanBlockDamagePipeline damagePipeline;
    final VulkanBlockDamagePipeline.State damage = new VulkanBlockDamagePipeline.State();
    private ItemFeatureRenderer itemRenderer;
    final ItemFeatureRenderer.Submissions items = new ItemFeatureRenderer.Submissions();
    private VulkanParticlePipeline particlePipeline;
    final VulkanParticlePipeline.State particles = new VulkanParticlePipeline.State();
    private VulkanWorldBorderPipeline borderPipeline;
    final VulkanWorldBorderPipeline.State border = new VulkanWorldBorderPipeline.State();
    private VulkanWeatherPipeline weatherPipeline;
    final VulkanWeatherPipeline.State weather = new VulkanWeatherPipeline.State();
    final VulkanCloudPipeline.State clouds = new VulkanCloudPipeline.State();
    private VulkanBuffer quadIndices;
    VulkanSkyBackground.Stars stars;
    final VulkanCelestialPipeline.Body sun = new VulkanCelestialPipeline.Body(true);
    final VulkanCelestialPipeline.Body moon = new VulkanCelestialPipeline.Body(false);
    int sourceWidth, sourceHeight;
    VulkanSkyBackground.Twilight twilight;
    VulkanSkyBackground.State upperSky, lowerSky;
    private VulkanTexture atlas;
    private VulkanTexture lightmap;
    private final VulkanAtlasUpdates lightUpdates = new VulkanAtlasUpdates();
    private Atlas pendingAtlas;
    private float[] mipWeights;
    private Frame frame;
    private int[] itemLightmap;
    private int drawCalls;
    private int quads;
    private int cutoutQuads;
    private int translucentQuads;

    @Override
    public void initialize(VkPhysicalDevice physicalDevice, VkDevice device, VkQueue queue, VkCommandBuffer commands) {
        this.physicalDevice = physicalDevice;
        this.device = device;
        this.queue = queue;
        this.commands = commands;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkFormatProperties properties = VkFormatProperties.malloc(stack);
            for (int format : new int[]{VK_FORMAT_X8_D24_UNORM_PACK32, VK_FORMAT_D32_SFLOAT, VK_FORMAT_D16_UNORM}) {
                vkGetPhysicalDeviceFormatProperties(physicalDevice, format, properties);
                if ((properties.optimalTilingFeatures() & VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0) {
                    depthFormat = format;
                    break;
                }
            }
            if (depthFormat == VK_FORMAT_UNDEFINED) throw new IllegalStateException("No terrain depth format available");
        }
        lightmap = new VulkanTexture(device);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            lightmap.initialize(physicalDevice, queue, commands, 16, 16, new ByteBuffer[]{stack.calloc(1024)}, VK_FILTER_LINEAR);
        }
        quadIndices = VulkanQuadIndices.create(physicalDevice, device);
        meshes = new VulkanTerrainMeshes(physicalDevice, device);
    }

    VulkanTerrainMeshes.Slice upload(ByteBuffer data) {
        return meshes.upload(data);
    }

    void retire(VulkanTerrainMeshes.Slice slice) {
        meshes.retire(slice);
    }

    void setAtlas(Atlas snapshot) {
        if (pendingAtlas != null) {
            pendingAtlas.close();
        }
        pendingAtlas = snapshot;
        atlasUpdates.clear();
    }

    void updateSprite(String name, int x, int y, int width, int height, int[][] pixels) {
        atlasUpdates.update(name, x, y, width, height, pixels);
    }

    @Override
    public void recordUploads(MemoryStack stack, VkCommandBuffer commands) {
        lightUpdates.record(stack, physicalDevice, device, commands, lightmap);
        if (atlas != null) {
            atlasUpdates.record(stack, physicalDevice, device, commands, atlas);
        }
    }

    @Override
    public int depthFormat() {
        return depthFormat;
    }

    @Override
    public void configureTarget(int colorFormat) {
        if (pipeline != null) {
            pipeline.close();
        }
        if (skyPipeline != null) skyPipeline.close();
        skyPipeline = new VulkanSkyBackground(device);
        skyPipeline.initialize(physicalDevice, colorFormat, depthFormat());
        if (celestial != null) celestial.close();
        celestial = new VulkanCelestialPipeline(device);
        celestial.initialize(physicalDevice, colorFormat, depthFormat());
        if (cloudPipeline != null) cloudPipeline.close();
        cloudPipeline = new VulkanCloudPipeline(device);
        cloudPipeline.initialize(colorFormat, depthFormat());
        if (outlinePipeline != null) outlinePipeline.close();
        outlinePipeline = new VulkanBlockOutlinePipeline(device);
        outlinePipeline.initialize(physicalDevice, colorFormat, depthFormat());
        if (damagePipeline != null) damagePipeline.close();
        damagePipeline = new VulkanBlockDamagePipeline(device);
        damagePipeline.initialize(colorFormat, depthFormat());
        if (weatherPipeline != null) weatherPipeline.close();
        weatherPipeline = new VulkanWeatherPipeline(device);
        weatherPipeline.initialize(colorFormat, depthFormat(), lightmap);
        if (borderPipeline != null) borderPipeline.close();
        borderPipeline = new VulkanWorldBorderPipeline(device);
        borderPipeline.initialize(colorFormat, depthFormat());
        pipeline = new VulkanTerrainPipeline(device);
        pipeline.initialize(physicalDevice, colorFormat, depthFormat());
        if (itemRenderer != null) itemRenderer.close();
        itemRenderer = new ItemFeatureRenderer(device, colorFormat, depthFormat(), pipeline.descriptorLayout());
        if (particlePipeline != null) particlePipeline.close();
        particlePipeline = new VulkanParticlePipeline(device);
        particlePipeline.initialize(colorFormat, depthFormat(), pipeline.descriptorLayout());
        if (atlas != null) {
            pipeline.setMipmapFilter(mipWeights);
            pipeline.bindTextures(atlas, lightmap);
            damagePipeline.setAtlas(atlas);
            particlePipeline.setAtlas(atlas);
        }
    }

    void setFrame(Frame frame, int[] itemLightmap) {
        this.frame = frame;
        this.itemLightmap = itemLightmap;
        drawCalls = 0;
        quads = 0;
        cutoutQuads = 0;
        translucentQuads = 0;
    }

    @Override
    public void releaseCompletedResources() {
        meshes.releaseRetired();
    }

    @Override
    public void prepareFrame() {
        if (outline.bounds != null) outlinePipeline.prepare(outline);
        if (damage.vertices != null) damagePipeline.prepare(physicalDevice, damage);
        itemRenderer.prepare(physicalDevice, queue, commands, items);
        if (!particles.isEmpty()) particlePipeline.prepare(physicalDevice, queue, commands, particles);
        if (border.vertices != null) borderPipeline.prepare(physicalDevice, queue, commands, border);
        if (!weather.batches.isEmpty()) weatherPipeline.prepare(physicalDevice, queue, commands, weather);
        if (clouds.visible) cloudPipeline.prepare(physicalDevice, queue, commands, clouds);
        if (stars != null) skyPipeline.prepareStars(physicalDevice, stars);
        celestial.prepare(physicalDevice, queue, commands, sun);
        celestial.prepare(physicalDevice, queue, commands, moon);
        Atlas snapshot = pendingAtlas;
        pendingAtlas = null;
        if (snapshot != null) {
            try (snapshot) {
                if (atlas != null) {
                    atlas.close();
                }
                atlas = new VulkanTexture(device);
                atlas.initialize(physicalDevice, queue, commands, snapshot.width, snapshot.height, snapshot.levels, VK_FILTER_NEAREST);
                mipWeights = snapshot.mipWeights;
                pipeline.setMipmapFilter(mipWeights);
                pipeline.bindTextures(atlas, lightmap);
                damagePipeline.setAtlas(atlas);
                particlePipeline.setAtlas(atlas);
            }
        }
        int[] lighting = frame != null ? frame.lightmap : itemLightmap;
        if (lighting == null) lighting = particles.lightmap;
        if (lighting == null) lighting = weather.lightmap;
        if (lighting != null) lightUpdates.update("lightmap", 0, 0, 16, 16, new int[][]{lighting});
        if (frame != null && atlas != null) {
            pipeline.updateFrame(frame.fog);
        }
    }

    @Override
    public void draw(MemoryStack stack, VkCommandBuffer commands, int width, int height) {
        if (frame == null && upperSky == null && lowerSky == null && twilight == null && !sun.visible && !moon.visible && stars == null && !clouds.visible && outline.bounds == null && damage.vertices == null && items.isEmpty() && particles.isEmpty() && weather.batches.isEmpty() && border.vertices == null) return;
        float scale = Math.min((float) width / sourceWidth, (float) height / sourceHeight);
        int viewWidth = Math.clamp(Math.round(sourceWidth * scale), 1, width);
        int viewHeight = Math.clamp(Math.round(sourceHeight * scale), 1, height);
        VulkanGraphics.viewport(stack, commands, (width - viewWidth) / 2, (height - viewHeight) / 2, viewWidth, viewHeight);
        if (upperSky != null) skyPipeline.draw(stack, commands, upperSky);
        if (twilight != null) skyPipeline.drawTwilight(stack, commands, twilight);
        celestial.draw(stack, commands, sun);
        celestial.draw(stack, commands, moon);
        if (stars != null) skyPipeline.drawStars(stack, commands, quadIndices, stars);
        if (lowerSky != null) skyPipeline.draw(stack, commands, lowerSky);
        if (clouds.visible && !clouds.afterTerrain) cloudPipeline.draw(stack, commands, quadIndices, clouds);
        drawTerrainAndWeather(stack, commands, viewWidth, viewHeight);
        if (clouds.visible && clouds.afterTerrain) cloudPipeline.draw(stack, commands, quadIndices, clouds);
    }

    private void drawTerrainAndWeather(MemoryStack stack, VkCommandBuffer commands, int viewWidth, int viewHeight) {
        boolean terrain = frame != null && atlas != null;
        if (terrain && !frame.opaque.isEmpty()) {
            pipeline.begin(stack, commands, quadIndices);
            drawCalls = pipeline.draw(stack, commands, frame.opaque, false);
        }
        if (!items.drops.isEmpty() && atlas != null) itemRenderer.draw(stack, commands, pipeline, quadIndices, items.drops, 0);
        if (outline.bounds != null) outlinePipeline.draw(stack, commands, outline, viewWidth, viewHeight);
        if (damage.vertices != null && atlas != null) damagePipeline.draw(stack, commands, quadIndices, damage);
        if (!items.pickups.isEmpty() && atlas != null) {
            itemRenderer.draw(stack, commands, pipeline, quadIndices, items.pickups, items.drops.vertexCount());
        }
        if (!particles.isEmpty()) particlePipeline.draw(stack, commands, pipeline, quadIndices, particles);
        if (!weather.batches.isEmpty()) weatherPipeline.draw(stack, commands, quadIndices, weather);
        if (border.vertices != null) borderPipeline.draw(stack, commands, quadIndices, border);
        if (!terrain) return;
        if (!frame.translucent.isEmpty()) {
            pipeline.begin(stack, commands, quadIndices);
            drawCalls += pipeline.draw(stack, commands, frame.translucent, true);
        }
        for (Draw draw : frame.opaque) {
            quads += draw.mesh.quads();
            if (draw.layer != 0) {
                cutoutQuads += draw.mesh.quads();
            }
        }
        for (Draw draw : frame.translucent) {
            translucentQuads += draw.mesh.quads();
        }
        quads += translucentQuads;
    }

    int drawCalls() {
        return drawCalls;
    }

    int quads() {
        return quads;
    }

    int cutoutQuads() {
        return cutoutQuads;
    }

    int translucentQuads() {
        return translucentQuads;
    }

    @Override
    public void close() {
        if (itemRenderer != null) itemRenderer.close();
        if (particlePipeline != null) particlePipeline.close();
        if (damagePipeline != null) damagePipeline.close();
        if (outlinePipeline != null) outlinePipeline.close();
        if (celestial != null) celestial.close();
        if (cloudPipeline != null) cloudPipeline.close();
        if (weatherPipeline != null) weatherPipeline.close();
        if (borderPipeline != null) borderPipeline.close();
        setAtlas(null);
        lightUpdates.close();
        atlasUpdates.close();
        if (skyPipeline != null) skyPipeline.close();
        if (lightmap != null) lightmap.close();
        if (pipeline != null) pipeline.close();
        if (atlas != null) atlas.close();
        if (meshes != null) meshes.close();
        if (quadIndices != null) quadIndices.close();
    }

    record Frame(int[] lightmap, Fog fog, List<Draw> opaque, List<Draw> translucent) {
    }

    record Fog(float red, float green, float blue, float mode, float start, float end, float density) {
    }

    record Draw(VulkanTerrainMeshes.Slice mesh, Matrix4fc transform, Vector4fc eyeZ, int layer, boolean mipmapped) {
    }

    record Atlas(int width, int height, ByteBuffer[] levels, float[] mipWeights) implements AutoCloseable {
        @Override
        public void close() {
            for (ByteBuffer level : levels) {
                memFree(level);
            }
        }
    }
}
