package net.radiant.render.vulkan;

import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkClearAttachment;
import org.lwjgl.vulkan.VkClearRect;
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
    private final VulkanEnvironment environment = new VulkanEnvironment();
    private final VulkanObjectCapture objectCapture;
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    private VkQueue queue;
    private VkCommandBuffer commands;
    private int depthFormat;
    private VulkanTerrainMeshes meshes;
    private VulkanTerrainPipeline pipeline;
    private VulkanObjectRenderer objects;
    private VulkanEntityOutline outlines;
    private VulkanTexture atlas;
    private Atlas pendingAtlas;
    private Frame frame;
    private int drawCalls;
    private int quads;
    private int cutoutQuads;
    private int translucentQuads;
    private VulkanEnvironment.SkyMode skyModeDrawn;
    private boolean cloudsDrawn;

    VulkanTerrainScene(VulkanObjectCapture objectCapture) {
        this.objectCapture = objectCapture;
    }

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
        objects = new VulkanObjectRenderer(physicalDevice, device);
        outlines = new VulkanEntityOutline(physicalDevice, device);
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
        environment.setImages(null);
    }

    void setEnvironmentImages(VulkanEnvironment.Images images) {
        environment.setImages(images);
    }

    void updateSprite(String name, int x, int y, int width, int height, int[][] pixels) {
        atlasUpdates.update(name, x, y, width, height, pixels);
    }

    @Override
    public void recordUploads(MemoryStack stack, VkCommandBuffer commands) {
        if (atlas != null) {
            atlasUpdates.record(stack, physicalDevice, device, commands, atlas);
            objects.recordTextureUpdates(stack, commands);
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
        pipeline = new VulkanTerrainPipeline(device);
        pipeline.initialize(physicalDevice, colorFormat, depthFormat());
        environment.configureTarget(physicalDevice, device, colorFormat, depthFormat());
        objects.configureTarget(colorFormat, depthFormat());
        outlines.configureTarget(colorFormat, depthFormat());
        if (atlas != null) {
            pipeline.bindAtlas(atlas);
        }
    }

    @Override
    public void recordOffscreen(MemoryStack stack, VkCommandBuffer commands, int width, int height) {
        if (frame == null || atlas == null || objectCapture.outlineDraws().isEmpty()) {
            return;
        }
        View view = viewport(width, height);
        outlines.recordMask(stack, commands, width, height, view.width, view.height, objects, objectCapture, frame.fog);
    }

    void setFrame(Frame frame) {
        this.frame = frame;
        drawCalls = 0;
        quads = 0;
        cutoutQuads = 0;
        translucentQuads = 0;
        skyModeDrawn = null;
        cloudsDrawn = false;
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
            objects.prepare(queue, commands, objectCapture, atlas);
            if (frame.environment != null) {
                environment.prepare(queue, commands, frame.viewProjection, frame.fog, frame.environment, hasCustomSky());
            }
        }
    }

    @Override
    public void draw(MemoryStack stack, VkCommandBuffer commands, int width, int height) {
        if (frame == null || atlas == null) {
            return;
        }
        View view = viewport(width, height);
        VulkanGraphics.viewport(stack, commands, (width - view.width) / 2, (height - view.height) / 2, view.width, view.height);
        if (frame.environment != null) {
            Runnable customSky = hasCustomSky()
                    ? () -> objects.draw(stack, commands, objectCapture, frame.fog, VulkanObjectCapture.Layer.SKY) : null;
            environment.drawSky(stack, commands, frame.environment, customSky);
            skyModeDrawn = frame.environment.skyMode();
        }
        boolean drawClouds = frame.environment != null && frame.environment.cloudMode() != 0;
        boolean cloudsBeforeTerrain = drawClouds && frame.environment.cloudsBeforeTerrain();
        if (cloudsBeforeTerrain) {
            environment.drawClouds(stack, commands);
        }
        pipeline.begin(stack, commands);
        drawCalls = pipeline.draw(stack, commands, frame.opaque, false);
        objects.draw(stack, commands, objectCapture, frame.fog, VulkanObjectCapture.Layer.WORLD);
        pipeline.begin(stack, commands);
        drawCalls += pipeline.draw(stack, commands, frame.translucent, true);
        if (drawClouds && !cloudsBeforeTerrain) {
            environment.drawClouds(stack, commands);
        }
        cloudsDrawn = drawClouds;
        drawOverlay(stack, commands, width, height, VulkanObjectCapture.Layer.FIRST_PERSON);
        if (!objectCapture.outlineDraws().isEmpty()) {
            outlines.draw(stack, commands);
        }
        drawOverlay(stack, commands, width, height, VulkanObjectCapture.Layer.HUD);
        drawOverlay(stack, commands, width, height, VulkanObjectCapture.Layer.SCREEN);
        drawOverlay(stack, commands, width, height, VulkanObjectCapture.Layer.ACHIEVEMENT);
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

    private boolean hasCustomSky() {
        return objectCapture.drawStart(VulkanObjectCapture.Layer.SKY) != objectCapture.drawEnd(VulkanObjectCapture.Layer.SKY);
    }

    private View viewport(int width, int height) {
        float scale = Math.min((float) width / frame.width, (float) height / frame.height);
        return new View(Math.clamp(Math.round(frame.width * scale), 1, width),
                Math.clamp(Math.round(frame.height * scale), 1, height));
    }

    private record View(int width, int height) {
    }

    private void drawOverlay(MemoryStack stack, VkCommandBuffer commands, int width, int height, VulkanObjectCapture.Layer layer) {
        if (objectCapture.drawStart(layer) == objectCapture.drawEnd(layer)) {
            return;
        }
        // Each overlay starts with the legacy depth clear and composites onto the current color.
        VkClearAttachment.Buffer clear = VkClearAttachment.calloc(1, stack);
        clear.get(0).aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT).clearValue().depthStencil().depth(1.0f);
        VkClearRect.Buffer rect = VkClearRect.calloc(1, stack);
        rect.get(0).layerCount(1).rect().extent().set(width, height);
        vkCmdClearAttachments(commands, clear, rect);
        objects.draw(stack, commands, objectCapture, frame.fog, layer);
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

    boolean skyDrawn() {
        return skyModeDrawn != null && skyModeDrawn != VulkanEnvironment.SkyMode.FOG;
    }

    boolean endSkyDrawn() {
        return skyModeDrawn == VulkanEnvironment.SkyMode.END;
    }

    boolean fogBackgroundDrawn() {
        return skyModeDrawn == VulkanEnvironment.SkyMode.FOG;
    }

    boolean cloudsDrawn() {
        return cloudsDrawn;
    }

    @Override
    public void close() {
        setAtlas(null);
        atlasUpdates.close();
        environment.close();
        if (outlines != null) outlines.close();
        if (objects != null) objects.close();
        if (pipeline != null) pipeline.close();
        if (atlas != null) atlas.close();
        if (meshes != null) meshes.close();
    }

    record Frame(Matrix4fc viewProjection, int[] lightmap, Fog fog, VulkanEnvironment.Frame environment,
                 List<Draw> opaque, List<Draw> translucent,
                 int width, int height) {
    }

    record Fog(float red, float green, float blue, float mode, float terrainMode, float start, float end, float density) {
    }

    record Draw(VulkanTerrainMeshes.Slice mesh, float x, float y, float z, int layer) {
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
