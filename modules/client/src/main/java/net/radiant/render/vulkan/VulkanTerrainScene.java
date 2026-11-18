package net.radiant.render.vulkan;

import org.joml.Matrix4fc;
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
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    private VkQueue queue;
    private VkCommandBuffer commands;
    private int depthFormat;
    private VulkanTerrainMeshes meshes;
    private VulkanTerrainPipeline pipeline;
    private VulkanTexture atlas;
    private Atlas pendingAtlas;
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
            vkGetPhysicalDeviceFormatProperties(physicalDevice, VK_FORMAT_D32_SFLOAT, properties);
            depthFormat = (properties.optimalTilingFeatures() & VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0
                    ? VK_FORMAT_D32_SFLOAT : VK_FORMAT_D16_UNORM;
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
        pipeline = new VulkanTerrainPipeline(device);
        pipeline.initialize(physicalDevice, colorFormat, depthFormat());
        if (atlas != null) {
            pipeline.bindAtlas(atlas);
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
        Atlas snapshot = pendingAtlas;
        pendingAtlas = null;
        if (snapshot != null) {
            try (snapshot) {
                if (atlas != null) {
                    atlas.close();
                }
                atlas = new VulkanTexture(device);
                atlas.initialize(physicalDevice, queue, commands, snapshot.width, snapshot.height, snapshot.levels, false);
                pipeline.bindAtlas(atlas);
            }
        }
        if (frame != null && atlas != null) {
            pipeline.updateFrame(frame.viewProjection, frame.lightmap, frame.fog);
        }
    }

    @Override
    public void draw(MemoryStack stack, VkCommandBuffer commands, int width, int height) {
        if (frame == null || atlas == null || (frame.opaque.isEmpty() && frame.translucent.isEmpty())) {
            return;
        }
        float scale = Math.min((float) width / frame.width, (float) height / frame.height);
        int viewWidth = Math.clamp(Math.round(frame.width * scale), 1, width);
        int viewHeight = Math.clamp(Math.round(frame.height * scale), 1, height);
        VulkanGraphics.viewport(stack, commands, (width - viewWidth) / 2, (height - viewHeight) / 2, viewWidth, viewHeight);
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
        setAtlas(null);
        if (pipeline != null) pipeline.close();
        if (atlas != null) atlas.close();
        if (meshes != null) meshes.close();
    }

    record Frame(Matrix4fc viewProjection, int[] lightmap, Fog fog, List<Draw> opaque, List<Draw> translucent,
                 int width, int height) {
    }

    record Fog(float red, float green, float blue, float mode, float start, float end, float density) {
    }

    record Draw(VulkanTerrainMeshes.Slice mesh, float x, float y, float z, int layer, double distanceSquared) {
    }

    record Atlas(int width, int height, ByteBuffer[] levels) implements AutoCloseable {
        @Override
        public void close() {
            for (ByteBuffer level : levels) {
                memFree(level);
            }
        }
    }
}
