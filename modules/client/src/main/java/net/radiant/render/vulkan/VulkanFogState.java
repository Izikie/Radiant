package net.radiant.render.vulkan;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

final class VulkanFogState {
    static final int BYTES = 112;
    private Matrix4f transform;
    private final Vector4f eyeZ = new Vector4f(), fogColor = new Vector4f(), fog = new Vector4f();

    void capture() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer values = stack.mallocFloat(16);
            glGetFloatv(GL_MODELVIEW_MATRIX, values);
            eyeZ.set(values.get(2), values.get(6), values.get(10), values.get(14));
            transform = VulkanSkyBackground.projectionModelView(values);
            glGetFloatv(GL_FOG_COLOR, values);
            fogColor.set(values);
            fogColor.w = glIsEnabled(GL_ALPHA_TEST) ? glGetFloat(GL_ALPHA_TEST_REF) : -1;
            fog.set(glIsEnabled(GL_FOG) ? glGetInteger(GL_FOG_MODE) : 0,
                    glGetFloat(GL_FOG_START), glGetFloat(GL_FOG_END), glGetFloat(GL_FOG_DENSITY));
        }
    }

    ByteBuffer constants(MemoryStack stack) {
        ByteBuffer constants = stack.malloc(BYTES);
        transform.get(0, constants);
        eyeZ.get(64, constants);
        fogColor.get(80, constants);
        fog.get(96, constants);
        return constants;
    }

    static String shaderHeader(VkDevice device) {
        return shaderHeader(device, "");
    }

    static String shaderHeader(VkDevice device, String extraConstants) {
        return "#version 450\n#define OPENGL_CLIP_DEPTH " + (device.getCapabilities().VK_EXT_depth_clip_control ? "1\n" : "0\n") + """
                layout(push_constant) uniform Draw {
                    mat4 transform;
                    vec4 eyeZ;
                    vec4 fogColorAndAlphaRef;
                    vec4 fog;
                """ + extraConstants + "} draw;\n";
    }

    static final String FRAGMENT_FOG = """
            vec3 applyFog(vec3 rgb, float eyeZ) {
                if (draw.fog.x == 0) return rgb;
                float d = abs(eyeZ);
                float f;
                if (draw.fog.x == 2048) f = exp(-draw.fog.w * d);
                else if (draw.fog.x == 2049) f = exp(-pow(draw.fog.w * d, 2.0));
                else f = (draw.fog.z - d) / max(draw.fog.z - draw.fog.y, 0.0001);
                return mix(draw.fogColorAndAlphaRef.rgb, rgb, clamp(f, 0.0, 1.0));
            }
            """;
}
