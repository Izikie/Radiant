package net.radiant.render.vulkan;

import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.EXTLineRasterization.VK_LINE_RASTERIZATION_MODE_BRESENHAM_EXT;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanBlockOutlinePipeline implements AutoCloseable {
    private static final int STAGES = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;
    private static final int BOUNDS_BYTES = 6 * Float.BYTES;

    static final class State {
        AxisAlignedBB bounds;
        private final VulkanFogState fog = new VulkanFogState();

        void capture(AxisAlignedBB bounds) {
            this.bounds = bounds;
            fog.capture();
        }
    }

    private final VkDevice device;
    private final boolean wideLines;
    private long layout, pipeline;
    private VulkanBuffer bounds;

    VulkanBlockOutlinePipeline(VkDevice device) {
        this.device = device;
        wideLines = device.getCapabilities().VK_EXT_line_rasterization;
    }

    void initialize(VkPhysicalDevice gpu, int colorFormat, int depthFormat) {
        bounds = VulkanBuffer.mapped(gpu, device, BOUNDS_BYTES, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var range = VkPushConstantRange.calloc(1, stack);
            range.get(0).stageFlags(STAGES).size(VulkanFogState.BYTES + 2 * Float.BYTES);
            var handle = stack.mallocLong(1);
            VulkanPresenter.check(vkCreatePipelineLayout(device, VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                    .pPushConstantRanges(range), null, handle), "create block outline layout");
            layout = handle.get(0);
            var stream = VkVertexInputBindingDescription.calloc(1, stack);
            stream.get(0).stride(BOUNDS_BYTES).inputRate(VK_VERTEX_INPUT_RATE_INSTANCE);
            var attrs = VkVertexInputAttributeDescription.calloc(2, stack);
            attrs.get(0).location(0).format(VK_FORMAT_R32G32B32_SFLOAT);
            attrs.get(1).location(1).offset(3 * Float.BYTES).format(VK_FORMAT_R32G32B32_SFLOAT);
            var inputs = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default()
                    .pVertexBindingDescriptions(stream).pVertexAttributeDescriptions(attrs);
            var rasterization = VkPipelineRasterizationStateCreateInfo.calloc(stack).sType$Default().lineWidth(wideLines ? 2 : 1);
            if (wideLines) {
                rasterization.pNext(VkPipelineRasterizationLineStateCreateInfoEXT.calloc(stack).sType$Default()
                        .lineRasterizationMode(VK_LINE_RASTERIZATION_MODE_BRESENHAM_EXT).address());
            }
            String header = VulkanFogState.shaderHeader(device, "vec2 viewport;");
            pipeline = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat, VK_PRIMITIVE_TOPOLOGY_LINE_LIST,
                    VulkanGraphics.Blend.TRANSLUCENT, false, VulkanGraphics.RGBA, rasterization, inputs,
                    header + "#define WIDE_LINES " + (wideLines ? "1\n" : "0\n") + VERTEX, header + VulkanFogState.FRAGMENT_FOG + """
                    layout(location=0) in float eyeZ;
                    layout(location=0) out vec4 color;
                    void main() {
                        color = vec4(applyFog(vec3(0), eyeZ), 0.4);
                    }
                    """);
        }
    }

    private static final String VERTEX = """
            layout(location=0) in vec3 lower;
            layout(location=1) in vec3 upper;
            layout(location=0) out float eyeZ;
            // Bottom loop, top loop, then verticals in RenderGlobal.drawSelectionBoundingBox order.
            const int corners[24] = int[](0,1, 1,5, 5,4, 4,0, 2,3, 3,7, 7,6, 6,2, 0,2, 1,3, 5,7, 4,6);
            vec4 position(int index) {
                int c = corners[index];
                return vec4(mix(lower, upper, notEqual(ivec3(c) & ivec3(1,2,4), ivec3(0))), 1);
            }
            void main() {
                int i = gl_VertexIndex;
                #if !WIDE_LINES
                i = (i / 4) * 2 + (i & 1);
                #endif
                vec4 p = position(i);
                eyeZ = dot(draw.eyeZ, p);
                vec4 clip = draw.transform * p;
                #if !WIDE_LINES
                // A two-pixel aliased line repeats the center line along its minor window axis.
                vec4 other = draw.transform * position(i ^ 1);
                vec2 delta = abs((clip.xy * other.w - other.xy * clip.w) * draw.viewport);
                vec2 axis = delta.x >= delta.y ? vec2(0,1) : vec2(1,0);
                clip.xy += axis * ((gl_VertexIndex & 2) == 0 ? -1.0 : 1.0) * clip.w / draw.viewport;
                #endif
                #if !OPENGL_CLIP_DEPTH
                clip.z = (clip.z + clip.w) * 0.5;
                #endif
                gl_Position = clip;
            }
            """;

    void prepare(State outline) {
        AxisAlignedBB box = outline.bounds;
        bounds.data().asFloatBuffer().put((float) box.minX).put((float) box.minY).put((float) box.minZ)
                .put((float) box.maxX).put((float) box.maxY).put((float) box.maxZ);
    }

    void draw(MemoryStack stack, VkCommandBuffer commands, State outline, int width, int height) {
        vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        vkCmdBindVertexBuffers(commands, 0, stack.longs(bounds.handle()), stack.longs(0));
        vkCmdPushConstants(commands, layout, STAGES, 0, outline.fog.constants(stack));
        vkCmdPushConstants(commands, layout, STAGES, VulkanFogState.BYTES, stack.floats(width, height));
        vkCmdDraw(commands, wideLines ? 24 : 48, 1, 0, 0);
    }

    @Override
    public void close() {
        if (pipeline != 0) vkDestroyPipeline(device, pipeline, null);
        if (layout != 0) vkDestroyPipelineLayout(device, layout, null);
        if (bounds != null) bounds.close();
    }
}
