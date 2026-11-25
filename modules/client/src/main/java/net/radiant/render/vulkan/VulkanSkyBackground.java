package net.radiant.render.vulkan;

import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanSkyBackground implements AutoCloseable {
    record State(Matrix4f transform, Vector4f eyeZ, Vector3f color, int cells,
                 Vector3f fogColor, float planeY, Vector4f fog, int width, int height) {}

    static State capture(boolean upper, int radius) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer matrix = stack.mallocFloat(16);
            glGetFloatv(GL_MODELVIEW_MATRIX, matrix);
            Vector4f eyeZ = new Vector4f(matrix.get(2), matrix.get(6), matrix.get(10), matrix.get(14));
            glMatrixMode(GL_PROJECTION);
            glPushMatrix();
            try {
                glMultMatrixf(matrix);
                glGetFloatv(GL_PROJECTION_MATRIX, matrix);
            } finally {
                glPopMatrix();
                glMatrixMode(GL_MODELVIEW);
            }
            FloatBuffer fogColor = stack.mallocFloat(4);
            glGetFloatv(GL_FOG_COLOR, fogColor);
            int cells = 2 * radius + 1;
            FloatBuffer color = stack.mallocFloat(4);
            glGetFloatv(GL_CURRENT_COLOR, color);
            Minecraft mc = Minecraft.get();
            Vector4f fog = new Vector4f(glGetInteger(GL_FOG_MODE), glGetFloat(GL_FOG_START),
                    glGetFloat(GL_FOG_END), glGetFloat(GL_FOG_DENSITY));
            return new State(new Matrix4f().set(matrix), eyeZ, new Vector3f(color), cells,
                    new Vector3f(fogColor), upper ? 16 : -16, fog, mc.displayWidth, mc.displayHeight);
        }
    }

    private static final String CONSTANTS = """
            layout(push_constant) uniform Sky {
                mat4 transform;
                vec4 eyeZ;
                vec3 color;
                int cells;
                vec3 fogColor;
                float planeY;
                vec4 fog;
            } sky;
            """;
    private static final String VERTEX = """
            layout(location=0) out float eyeZ;
            const ivec2 corners[6] = ivec2[](ivec2(0,0), ivec2(1,0), ivec2(0,1),
                                             ivec2(1,0), ivec2(1,1), ivec2(0,1));
            void main() {
                int cells = sky.cells;
                ivec2 cell = ivec2(gl_InstanceIndex / cells, gl_InstanceIndex % cells) - cells / 2;
                ivec2 corner = corners[gl_VertexIndex];
                if (sky.planeY < 0) corner.x = 1 - corner.x;
                vec2 xz = vec2(cell + corner) * 64.0;
                vec4 position = vec4(xz.x, sky.planeY, xz.y, 1);
                eyeZ = dot(sky.eyeZ, position);
                vec4 clip = sky.transform * position;
                #if !OPENGL_CLIP_DEPTH
                clip.z = (clip.z + clip.w) * 0.5;
                #endif
                gl_Position = clip;
            }
            """;
    private static final String FRAGMENT = """
            layout(location=0) in float eyeZ;
            layout(location=0) out vec4 color;
            void main() {
                float d = abs(eyeZ);
                float f;
                if (sky.fog.x == 2048.0) f = exp(-sky.fog.w * d); // GL_EXP
                else if (sky.fog.x == 2049.0) f = exp(-pow(sky.fog.w * d, 2.0)); // GL_EXP2
                else f = (sky.fog.z - d) / max(sky.fog.z - sky.fog.y, 0.0001);
                color = vec4(mix(sky.fogColor.rgb, sky.color.rgb, clamp(f, 0.0, 1.0)), 1);
            }
            """;
    private static final int STAGES = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;
    private final VkDevice device;
    private long layout, pipeline;

    VulkanSkyBackground(VkDevice device) {
        this.device = device;
    }

    void initialize(int format, int depthFormat) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPushConstantRange.Buffer range = VkPushConstantRange.calloc(1, stack);
            range.get(0).stageFlags(STAGES).size(128);
            LongBuffer handle = stack.mallocLong(1);
            VulkanPresenter.check(vkCreatePipelineLayout(device, VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType$Default().pPushConstantRanges(range), null, handle), "create sky pipeline layout");
            layout = handle.get(0);
            String header = "#version 450\n#define OPENGL_CLIP_DEPTH "
                    + (device.getCapabilities().VK_EXT_depth_clip_control ? "1\n" : "0\n");
            pipeline = VulkanGraphics.pipeline(stack, device, layout, format, depthFormat, VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
                    false, false, VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default(),
                    header + CONSTANTS + VERTEX, header + CONSTANTS + FRAGMENT);
        }
    }

    void draw(MemoryStack stack, VkCommandBuffer commands, State sky) {
        ByteBuffer constants = stack.malloc(128);
        sky.transform.get(0, constants);
        sky.eyeZ.get(64, constants);
        sky.color.get(80, constants);
        constants.putInt(92, sky.cells);
        sky.fogColor.get(96, constants);
        constants.putFloat(108, sky.planeY);
        sky.fog.get(112, constants);
        vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        vkCmdPushConstants(commands, layout, STAGES, 0, constants);
        vkCmdDraw(commands, 6, sky.cells * sky.cells, 0, 0);
    }

    @Override
    public void close() {
        if (pipeline != 0) vkDestroyPipeline(device, pipeline, null);
        if (layout != 0) vkDestroyPipelineLayout(device, layout, null);
    }
}
