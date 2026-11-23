package net.radiant.render.vulkan;

import net.minecraft.client.renderer.WorldRenderer;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public final class VulkanCloudMesh {
    static final int STRIDE = 24;
    private final byte[] vertices;

    private VulkanCloudMesh(byte[] vertices) {
        this.vertices = vertices;
    }

    int byteSize() {
        return vertices.length;
    }

    void copyTo(ByteBuffer target) {
        target.put(vertices);
    }

    public static final class Builder {
        private static final int[] TRIANGLE_CORNERS = {0, 1, 3, 1, 2, 3};
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final byte[] vertex = new byte[STRIDE];

        public void append(WorldRenderer mesh) {
            ByteBuffer source = mesh.getByteBuffer();
            int stride = mesh.getVertexFormat().getNextOffset();
            // Both cloud formats start with position, UV, and color. Fancy also stores a normal.
            for (int quad = 0; quad < mesh.getVertexCount(); quad += 4) {
                for (int corner : TRIANGLE_CORNERS) {
                    source.get((quad + corner) * stride, vertex, 0, STRIDE);
                    output.writeBytes(vertex);
                }
            }
        }

        public VulkanCloudMesh finish() {
            VulkanCloudMesh mesh = new VulkanCloudMesh(output.toByteArray());
            output.reset();
            return mesh;
        }
    }
}
