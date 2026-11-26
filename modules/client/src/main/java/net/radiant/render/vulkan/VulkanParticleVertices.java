package net.radiant.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static org.lwjgl.vulkan.VK10.*;

/** The 1.8.9 PARTICLE_POSITION_TEX_COLOR_LMAP stream used by particles and precipitation. */
final class VulkanParticleVertices {
    static final int STRIDE = 28;
    static final String SHADER = """
            layout(location=0) in vec3 position;
            layout(location=1) in vec2 texCoord;
            layout(location=2) in vec4 vertexColor;
            layout(location=3) in ivec2 lightUv;
            layout(location=0) out vec2 uv;
            layout(location=1) flat out vec4 tint;
            layout(location=2) out float eyeZ;
            layout(location=3) out vec2 lightCoord;
            void main() {
                uv = texCoord;
                tint = vertexColor;
                lightCoord = (vec2(lightUv) + 8.0) / 256.0;
                vec4 p = vec4(position, 1);
                eyeZ = dot(draw.eyeZ, p);
                vec4 clip = draw.transform * p;
                #if !OPENGL_CLIP_DEPTH
                clip.z = (clip.z + clip.w) * 0.5;
                #endif
                gl_Position = clip;
            }
            """;

    private VulkanParticleVertices() {}

    static VkPipelineVertexInputStateCreateInfo input(MemoryStack stack) {
        var stream = VkVertexInputBindingDescription.calloc(1, stack);
        stream.get(0).stride(STRIDE);
        var attrs = VkVertexInputAttributeDescription.calloc(4, stack);
        attrs.get(0).location(0).format(VK_FORMAT_R32G32B32_SFLOAT);
        attrs.get(1).location(1).offset(12).format(VK_FORMAT_R32G32_SFLOAT);
        attrs.get(2).location(2).offset(20).format(VK_FORMAT_R8G8B8A8_UNORM);
        attrs.get(3).location(3).offset(24).format(VK_FORMAT_R16G16_SINT);
        return VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default()
                .pVertexBindingDescriptions(stream).pVertexAttributeDescriptions(attrs);
    }
}
