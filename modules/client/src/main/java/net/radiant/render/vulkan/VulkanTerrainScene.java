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
        if (atlas != null) {
            lightUpdates.record(stack, physicalDevice, device, commands, lightmap);
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
        pipeline = new VulkanTerrainPipeline(device);
        pipeline.initialize(physicalDevice, colorFormat, depthFormat());
        if (atlas != null) {
            pipeline.setMipmapFilter(mipWeights);
            pipeline.bindTextures(atlas, lightmap);
        }
    }

    void setFrame(Frame frame) {
        this.frame = frame;
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
            }
        }
        if (frame != null && atlas != null) {
            lightUpdates.update("lightmap", 0, 0, 16, 16, new int[][]{frame.lightmap});
            pipeline.updateFrame(frame.fog);
        }
    }

    @Override
    public void draw(MemoryStack stack, VkCommandBuffer commands, int width, int height) {
        if (frame == null && upperSky == null && lowerSky == null && twilight == null && !sun.visible && !moon.visible) return;
        float scale = Math.min((float) width / sourceWidth, (float) height / sourceHeight);
        int viewWidth = Math.clamp(Math.round(sourceWidth * scale), 1, width);
        int viewHeight = Math.clamp(Math.round(sourceHeight * scale), 1, height);
        VulkanGraphics.viewport(stack, commands, (width - viewWidth) / 2, (height - viewHeight) / 2, viewWidth, viewHeight);
        if (upperSky != null) skyPipeline.draw(stack, commands, upperSky);
        if (twilight != null) skyPipeline.drawTwilight(stack, commands, twilight);
        celestial.draw(stack, commands, sun);
        celestial.draw(stack, commands, moon);
        if (lowerSky != null) skyPipeline.draw(stack, commands, lowerSky);
        if (frame == null || atlas == null || (frame.opaque.isEmpty() && frame.translucent.isEmpty())) return;
        pipeline.begin(stack, commands);
        drawCalls = pipeline.draw(stack, commands, frame.opaque, false)
                + pipeline.draw(stack, commands, frame.translucent, true);
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
        if (celestial != null) celestial.close();
        setAtlas(null);
        lightUpdates.close();
        atlasUpdates.close();
        if (skyPipeline != null) skyPipeline.close();
        if (lightmap != null) lightmap.close();
        if (pipeline != null) pipeline.close();
        if (atlas != null) atlas.close();
        if (meshes != null) meshes.close();
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
