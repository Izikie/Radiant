package net.radiant.render.vulkan;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.math.Vec3;

import net.radiant.util.NativeImage;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.vulkan.VK10.*;

final class VulkanEnvironment implements AutoCloseable {
    private static final int FRAME_BYTES = 128 + 9 * 16;
    private static final int POSITION_BYTES = 3 * Float.BYTES;
    private static final String VERTEX_SHADER = """
            #version 450
            layout(location = 0) out vec2 ndc;
            void main() {
                ndc = vec2(gl_VertexIndex == 1 ? 3.0 : -1.0, gl_VertexIndex == 2 ? 3.0 : -1.0);
                gl_Position = vec4(ndc, 1.0, 1.0);
            }
            """;
    private static final String STAR_VERTEX_SHADER = """
            #version 450
            layout(std140, set = 0, binding = 0) uniform Environment {
                mat4 viewProjection;
                mat4 inverseViewProjection;
                vec4 fogAndStars;
                vec4 skyColor;
                vec4 sunrise;
                vec4 celestial;
            } env;
            layout(location = 0) in vec3 position;
            layout(location = 0) noperspective out vec2 ndc;
            void main() {
                float s = env.celestial.x;
                float c = env.celestial.y;
                vec3 rotated = vec3(-s * position.y - c * position.z, c * position.y - s * position.z, position.x);
                vec4 clip = env.viewProjection * vec4(rotated, 1.0);
                gl_Position = vec4(clip.x, -clip.y, (clip.z + clip.w) * 0.5, clip.w);
                ndc = gl_Position.xy / gl_Position.w;
            }
            """;

    private final Matrix4f inverseViewProjection = new Matrix4f();
    private final VulkanTexture[] textures = new VulkanTexture[4];
    private Images pendingImages;
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    private VulkanBuffer uniformBuffer;
    private long descriptorLayout;
    private long descriptorPool;
    private long descriptorSet;
    private long layout;
    private long sky;
    private long clouds;
    private long stars;
    private VulkanBuffer starVertices;
    private int starVertexCount;

    void configureTarget(VkPhysicalDevice physicalDevice, VkDevice device, int colorFormat, int depthFormat) {
        destroyPipelines();
        this.physicalDevice = physicalDevice;
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (uniformBuffer == null) {
                uniformBuffer = VulkanBuffer.mapped(physicalDevice, device, FRAME_BYTES, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
                createDescriptors(stack);
                createStarVertices();
            }
            String defines = "#version 450\n#define SRGB_TARGET " + (VulkanGraphics.isSrgb(colorFormat) ? "1\n" : "0\n");
            String fragment = shaderSource();
            VkPipelineVertexInputStateCreateInfo vertices = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default();
            sky = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat, VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
                    VulkanGraphics.Pass.SKY, vertices, VERTEX_SHADER, defines + "#define CLOUD_PASS 0\n" + fragment);
            clouds = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat, VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
                    VulkanGraphics.Pass.BLENDED_DEPTH_WRITE, vertices, VERTEX_SHADER, defines + "#define CLOUD_PASS 1\n" + fragment);
            var binding = VkVertexInputBindingDescription.calloc(1, stack);
            binding.get(0).stride(POSITION_BYTES).inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
            var attribute = VkVertexInputAttributeDescription.calloc(1, stack);
            attribute.get(0).format(VK_FORMAT_R32G32B32_SFLOAT);
            vertices.pVertexBindingDescriptions(binding).pVertexAttributeDescriptions(attribute);
            stars = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat, VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
                    VulkanGraphics.Pass.SKY_ADDITIVE, vertices, STAR_VERTEX_SHADER, defines + "#define STAR_PASS 1\n" + fragment);
        }
    }

    private void createStarVertices() {
        WorldRenderer mesh = new WorldRenderer(16_384);
        RenderGlobal.renderStars(mesh);
        mesh.finishDrawing();
        ByteBuffer quads = mesh.getByteBuffer();
        int[] triangleCorners = {0, 1, 2, 2, 3, 0};
        starVertexCount = mesh.getVertexCount() / 4 * triangleCorners.length;
        starVertices = VulkanBuffer.mapped(physicalDevice, device, starVertexCount * POSITION_BYTES, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
        ByteBuffer triangles = starVertices.data();
        for (int quad = 0; quad < quads.limit(); quad += 4 * POSITION_BYTES) {
            for (int corner : triangleCorners) {
                int offset = quad + corner * POSITION_BYTES;
                triangles.putFloat(quads.getFloat(offset)).putFloat(quads.getFloat(offset + 4)).putFloat(quads.getFloat(offset + 8));
            }
        }
    }

    private static String shaderSource() {
        try (var input = VulkanEnvironment.class.getResourceAsStream("/assets/radiant/shaders/vulkan/environment.frag")) {
            if (input == null) {
                throw new IllegalStateException("Missing Vulkan environment shader");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read Vulkan environment shader", e);
        }
    }

    private void createDescriptors(MemoryStack stack) {
        LongBuffer handle = stack.mallocLong(1);
        VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(1 + textures.length, stack);
        for (int i = 0; i < bindings.capacity(); i++) {
            bindings.get(i).binding(i).descriptorCount(1).stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .descriptorType(i == 0 ? VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER : VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
        }
        bindings.get(0).stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT);
        VkDescriptorSetLayoutCreateInfo descriptorInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default().pBindings(bindings);
        VulkanPresenter.check(vkCreateDescriptorSetLayout(device, descriptorInfo, null, handle), "create environment descriptor layout");
        descriptorLayout = handle.get(0);
        VkDescriptorPoolSize.Buffer sizes = VkDescriptorPoolSize.calloc(2, stack);
        sizes.get(0).type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER).descriptorCount(1);
        sizes.get(1).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(textures.length);
        VkDescriptorPoolCreateInfo pool = VkDescriptorPoolCreateInfo.calloc(stack).sType$Default().maxSets(1).pPoolSizes(sizes);
        VulkanPresenter.check(vkCreateDescriptorPool(device, pool, null, handle), "create environment descriptor pool");
        descriptorPool = handle.get(0);
        VkDescriptorSetAllocateInfo allocate = VkDescriptorSetAllocateInfo.calloc(stack).sType$Default()
                .descriptorPool(descriptorPool).pSetLayouts(stack.longs(descriptorLayout));
        VulkanPresenter.check(vkAllocateDescriptorSets(device, allocate, handle), "allocate environment descriptor set");
        descriptorSet = handle.get(0);
        VkPipelineLayoutCreateInfo layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                .pSetLayouts(stack.longs(descriptorLayout));
        VulkanPresenter.check(vkCreatePipelineLayout(device, layoutInfo, null, handle), "create environment pipeline layout");
        layout = handle.get(0);
    }

    private void bindTextures() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc(1 + textures.length, stack);
            VkDescriptorBufferInfo.Buffer data = VkDescriptorBufferInfo.calloc(1, stack);
            data.get(0).buffer(uniformBuffer.handle()).range(FRAME_BYTES);
            writes.get(0).sType$Default().dstSet(descriptorSet).descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER).pBufferInfo(data);
            for (int i = 0; i < textures.length; i++) {
                VulkanTexture texture = textures[i];
                VkDescriptorImageInfo.Buffer dataImage = VkDescriptorImageInfo.calloc(1, stack);
                dataImage.get(0).sampler(texture.sampler()).imageView(texture.view())
                        .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                writes.get(i + 1).sType$Default().dstSet(descriptorSet).dstBinding(i + 1).descriptorCount(1)
                        .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).pImageInfo(dataImage);
            }
            vkUpdateDescriptorSets(device, writes, null);
        }
    }

    void setImages(Images images) {
        pendingImages = images;
    }

    void prepare(VkQueue queue, VkCommandBuffer commands, Matrix4fc viewProjection, VulkanTerrainScene.Fog fog, Frame frame) {
        if (pendingImages != null) {
            upload(0, pendingImages.clouds, queue, commands);
            upload(1, pendingImages.sun, queue, commands);
            upload(2, pendingImages.moon, queue, commands);
            upload(3, pendingImages.endSky, queue, commands);
            bindTextures();
            pendingImages = null;
        }
        ByteBuffer uniforms = uniformBuffer.data();
        viewProjection.get(0, uniforms);
        inverseViewProjection.set(viewProjection).invert().get(64, uniforms);
        uniforms.position(128);
        uniforms.putFloat(fog.red()).putFloat(fog.green()).putFloat(fog.blue()).putFloat(frame.stars);
        uniforms.putFloat(frame.skyRed).putFloat(frame.skyGreen).putFloat(frame.skyBlue).putFloat(frame.skyExtent);
        uniforms.putFloat(frame.sunriseRed).putFloat(frame.sunriseGreen).putFloat(frame.sunriseBlue).putFloat(frame.sunriseAlpha);
        uniforms.putFloat(frame.celestialSine).putFloat(frame.celestialCosine).putFloat(frame.celestialVisibility).putFloat(frame.moonPhase);
        uniforms.putFloat(frame.cloudRed).putFloat(frame.cloudGreen).putFloat(frame.cloudBlue).putFloat(frame.cloudMode);
        uniforms.putFloat(frame.cloudX).putFloat(frame.cloudZ).putFloat(frame.cloudHeight).putFloat(frame.skyMode.ordinal());
        Vec3 lower = frame.lowerSkyColor;
        uniforms.putFloat((float) lower.xCoord()).putFloat((float) lower.yCoord()).putFloat((float) lower.zCoord())
                .putFloat(frame.lowerSkyHeight);
        var skyFog = frame.skyFog;
        uniforms.putFloat(frame.skyFogMode).putFloat(skyFog == null ? 0 : skyFog.start())
                .putFloat(skyFog == null ? 0 : skyFog.end()).putFloat(skyFog == null ? 0 : skyFog.density());
        uniforms.putFloat(fog.mode()).putFloat(fog.start()).putFloat(fog.end()).putFloat(fog.density());
    }

    private void upload(int index, NativeImage image, VkQueue queue, VkCommandBuffer commands) {
        if (textures[index] != null) {
            textures[index].close();
        }
        VulkanTexture texture = new VulkanTexture(device);
        textures[index] = texture;
        texture.initialize(physicalDevice, queue, commands, image.getWidth(), image.getHeight(),
                new ByteBuffer[]{ByteBuffer.wrap(image.getData())}, false);
    }

    void drawSky(MemoryStack stack, VkCommandBuffer commands, Frame frame) {
        draw(stack, commands, sky);
        if (frame.stars > 0) {
            vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, stars);
            vkCmdBindVertexBuffers(commands, 0, stack.longs(starVertices.handle()), stack.longs(0));
            vkCmdDraw(commands, starVertexCount, 1, 0, 0);
        }
    }

    void drawClouds(MemoryStack stack, VkCommandBuffer commands) {
        draw(stack, commands, clouds);
    }

    private void draw(MemoryStack stack, VkCommandBuffer commands, long pipeline) {
        vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        vkCmdBindDescriptorSets(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, layout, 0, stack.longs(descriptorSet), null);
        vkCmdDraw(commands, 3, 1, 0, 0);
    }

    private void destroyPipelines() {
        if (stars != 0) {
            vkDestroyPipeline(device, stars, null);
            stars = 0;
        }
        if (sky != 0) {
            vkDestroyPipeline(device, sky, null);
            sky = 0;
        }
        if (clouds != 0) {
            vkDestroyPipeline(device, clouds, null);
            clouds = 0;
        }
    }

    @Override
    public void close() {
        pendingImages = null;
        destroyPipelines();
        if (starVertices != null) starVertices.close();
        if (layout != 0) vkDestroyPipelineLayout(device, layout, null);
        if (descriptorPool != 0) vkDestroyDescriptorPool(device, descriptorPool, null);
        if (descriptorLayout != 0) vkDestroyDescriptorSetLayout(device, descriptorLayout, null);
        for (VulkanTexture texture : textures) {
            if (texture != null) {
                texture.close();
            }
        }
        if (uniformBuffer != null) {
            uniformBuffer.close();
        }
    }

    record Images(NativeImage clouds, NativeImage sun, NativeImage moon, NativeImage endSky) {
    }

    enum SkyMode {
        OVERWORLD, END, FOG
    }

    record Frame(SkyMode skyMode, float skyExtent, float skyRed, float skyGreen, float skyBlue, float stars,
                 float sunriseRed, float sunriseGreen, float sunriseBlue, float sunriseAlpha,
                 float celestialSine, float celestialCosine, float celestialVisibility, int moonPhase,
                 int cloudMode, float cloudRed, float cloudGreen, float cloudBlue, float cloudX, float cloudZ,
                 float cloudHeight, float lowerSkyHeight, Vec3 lowerSkyColor, int skyFogMode,
                 GlStateManager.FogParameters skyFog) {
        static Frame dimensionSky(SkyMode mode, float red, float green, float blue) {
            return new Frame(mode, 0, red, green, blue, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, new Vec3(0, 0, 0), 0, null);
        }
    }
}
