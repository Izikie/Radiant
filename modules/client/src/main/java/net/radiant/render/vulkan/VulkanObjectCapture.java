package net.radiant.render.vulkan;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

/** Extracts the legacy world-object draws on the client thread, before their buffers are recycled. */
public final class VulkanObjectCapture implements AutoCloseable {
    static final int STRIDE = 28;
    private static final int[] QUAD_INDICES = {0, 1, 2, 2, 3, 0};
    private static final Map<ModelRenderer, List<Mesh>> MODELS = new WeakHashMap<>();
    private static List<Mesh> compiling;
    private static VulkanObjectCapture active;

    private final Map<Integer, Image> textures = new HashMap<>();
    private final List<Image> retiredImages = new ArrayList<>();
    private final Image white = new Image(1, 1, memAlloc(4).putInt(0, -1));
    private final Image atlas = new Image(0, 0, null);
    private final List<Draw> draws = new ArrayList<>();
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f modelView = new Matrix4f();
    private final Matrix4f transform = new Matrix4f();
    private final Matrix3f normalMatrix = new Matrix3f();
    private final Vector3f light0 = new Vector3f();
    private final Vector3f light1 = new Vector3f();
    private final Vector3f normal = new Vector3f();
    private final Vector4f clip = new Vector4f();
    private final Vector4f color = new Vector4f();
    private final ByteBuffer glyph = ByteBuffer.allocate(4 * 20).order(ByteOrder.nativeOrder());
    private ByteBuffer vertices = memAlloc(64 * 1024);
    private int[] lightmap;
    private int atlasId;
    private int entityTriangles;
    private int blockEntityTriangles;
    private int blockEntityGlyphs;
    private boolean blockEntities;

    void reset() {
        releaseRetiredImages();
        draws.clear();
        vertices.clear();
        entityTriangles = 0;
        blockEntityTriangles = 0;
        blockEntityGlyphs = 0;
        blockEntities = false;
    }

    void begin(int[] lightmap, int atlasId) {
        this.lightmap = lightmap;
        this.atlasId = atlasId;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer matrix = stack.mallocFloat(16);
            glGetFloatv(GL_PROJECTION_MATRIX, matrix);
            projection.set(matrix);
        }
        readLights();
        active = this;
    }

    public static void beginBlockEntities() {
        if (active != null) {
            active.blockEntities = true;
            active.readLights();
        }
    }

    public static void end() {
        active = null;
    }

    public static VulkanObjectCapture suspend() {
        VulkanObjectCapture capture = active;
        active = null;
        return capture;
    }

    public static void resume(VulkanObjectCapture capture) {
        active = capture;
    }

    private void readLights() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer light = stack.mallocFloat(4);
            glGetLightfv(GL_LIGHT0, GL_POSITION, light);
            light0.set(light.get(0), light.get(1), light.get(2)).normalize();
            glGetLightfv(GL_LIGHT1, GL_POSITION, light);
            light1.set(light.get(0), light.get(1), light.get(2)).normalize();
        }
    }

    public static void beginModel(ModelRenderer model) {
        if (VulkanTerrainPreview.ENABLED) {
            compiling = new ArrayList<>();
            MODELS.put(model, compiling);
        }
    }

    public static void endModel() {
        compiling = null;
    }

    public static void drawModel(ModelRenderer model) {
        if (active == null) {
            return;
        }
        List<Mesh> meshes = MODELS.get(model);
        if (meshes != null && !meshes.isEmpty()) {
            Material material = active.material();
            for (Mesh mesh : meshes) {
                active.append(mesh, material);
            }
        }
    }

    public static void draw(WorldRenderer source) {
        if (active == null && compiling == null) {
            return;
        }
        int mode = source.getDrawMode();
        int count = source.getVertexCount();
        if (triangleCount(mode, count) == 0) {
            return;
        }
        Layout layout = Layout.of(source.getVertexFormat());
        if (layout.position == -1) {
            return;
        }
        ByteBuffer data = source.getByteBuffer();
        if (compiling != null) {
            ByteBuffer copy = ByteBuffer.allocate(count * layout.stride).order(data.order());
            copy.put(0, data, 0, copy.capacity());
            compiling.add(new Mesh(copy, layout, mode, count));
        } else {
            active.append(new Mesh(data, layout, mode, count), active.material());
        }
    }

    public static void drawGlyph(float x, float y, float width, float skew, float u0, float v0, float u1, float v1) {
        if (active == null) {
            return;
        }
        ByteBuffer data = active.glyph.clear();
        putGlyphVertex(data, x + skew, y, u0, v0);
        putGlyphVertex(data, x - skew, y + 7.99f, u0, v1);
        putGlyphVertex(data, x + width + skew, y, u1, v0);
        putGlyphVertex(data, x + width - skew, y + 7.99f, u1, v1);
        active.append(new Mesh(data, Layout.GLYPH, GL_TRIANGLE_STRIP, 4), active.material());
        if (active.blockEntities) {
            active.blockEntityGlyphs++;
        }
    }

    private static void putGlyphVertex(ByteBuffer data, float x, float y, float u, float v) {
        data.putFloat(x).putFloat(y).putFloat(0).putFloat(u).putFloat(v);
    }

    private Material material() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer values = stack.mallocFloat(16);
            glGetFloatv(GL_MODELVIEW_MATRIX, values);
            modelView.set(values);
            projection.mul(modelView, transform);
            glGetFloatv(GL_CURRENT_COLOR, values);
            color.set(values);
        }
        Image image = white;
        int texture = GlStateManager.getBoundTexture();
        if (texture != 0 && glIsEnabled(GL_TEXTURE_2D)) {
            image = texture == atlasId ? atlas : texture(texture);
        }
        boolean blend = glIsEnabled(GL_BLEND);
        boolean depthWrite = glGetBoolean(GL_DEPTH_WRITEMASK);
        VulkanGraphics.Pass pass = !glIsEnabled(GL_DEPTH_TEST) ? VulkanGraphics.Pass.OBJECT_OVERLAY
                : blend ? (depthWrite ? VulkanGraphics.Pass.BLENDED_DEPTH_WRITE : VulkanGraphics.Pass.OBJECT_BLEND)
                : (depthWrite ? VulkanGraphics.Pass.OPAQUE : VulkanGraphics.Pass.OBJECT_READ_DEPTH);
        boolean lit = glIsEnabled(GL_LIGHTING);
        if (lit) {
            modelView.normal(normalMatrix);
        }
        return new Material(image, pass, glIsEnabled(GL_ALPHA_TEST) ? glGetFloat(GL_ALPHA_TEST_REF) : -1,
                lit, GlStateManager.isLightmapTextureEnabled());
    }

    private Image texture(int id) {
        Image cached = textures.get(id);
        if (cached != null) {
            return cached;
        }
        int width = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH);
        int height = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT);
        if (width <= 0 || height <= 0) {
            return white;
        }
        ByteBuffer pixels = memAlloc(Math.multiplyExact(Math.multiplyExact(width, height), 4));
        try {
            glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            Image image = new Image(width, height, pixels);
            textures.put(id, image);
            return image;
        } catch (RuntimeException | Error e) {
            memFree(pixels);
            throw e;
        }
    }

    void invalidateTexture(int id) {
        Image image = textures.remove(id);
        if (image != null) {
            image.deleted = true;
            // A frame being captured may still contain draws that reference the old image.
            retiredImages.add(image);
        }
    }

    private void releaseRetiredImages() {
        retiredImages.forEach(Image::releasePixels);
        retiredImages.clear();
    }

    private void append(Mesh mesh, Material material) {
        int triangles = triangleCount(mesh.mode, mesh.count);
        int firstVertex = vertices.position() / STRIDE;
        int required = vertices.position() + triangles * 3 * STRIDE;
        if (required > vertices.capacity()) {
            int position = vertices.position();
            vertices = memRealloc(vertices, Math.max(required, vertices.capacity() * 2)).clear().position(position);
        }
        for (int triangle = 0; triangle < triangles; triangle++) {
            for (int corner = 0; corner < 3; corner++) {
                writeVertex(mesh, material, vertexIndex(mesh.mode, triangle, corner));
            }
        }
        int last = draws.size() - 1;
        if (last >= 0 && draws.get(last).material.equals(material)) {
            Draw previous = draws.get(last);
            previous.vertexCount += triangles * 3;
        } else {
            draws.add(new Draw(material, firstVertex, triangles * 3));
        }
        if (blockEntities) {
            blockEntityTriangles += triangles;
        } else {
            entityTriangles += triangles;
        }
    }

    private void writeVertex(Mesh mesh, Material material, int index) {
        ByteBuffer data = mesh.data;
        Layout layout = mesh.layout;
        int base = index * layout.stride;
        int p = base + layout.position;
        clip.set(data.getFloat(p), data.getFloat(p + 4), data.getFloat(p + 8), 1).mul(transform);
        vertices.putFloat(clip.x).putFloat(clip.y).putFloat(clip.z).putFloat(clip.w);
        int uv = base + layout.uv;
        vertices.putFloat(layout.uv < 0 ? 0 : data.getFloat(uv));
        vertices.putFloat(layout.uv < 0 ? 0 : data.getFloat(uv + 4));

        int light = 0xffffff;
        if (material.lightmapped) {
            int coordinate = base + layout.light;
            int x = layout.light < 0 ? (int) OpenGlHelper.lastBrightnessX : data.getShort(coordinate);
            int y = layout.light < 0 ? (int) OpenGlHelper.lastBrightnessY : data.getShort(coordinate + 2);
            light = lightmap[Math.clamp(x / 16, 0, 15) + 16 * Math.clamp(y / 16, 0, 15)];
        }
        float shade = 1;
        if (material.lit && layout.normal >= 0) {
            int n = base + layout.normal;
            normal.set(data.get(n), data.get(n + 1), data.get(n + 2));
            if (normal.lengthSquared() > 0) {
                normal.mul(normalMatrix).normalize();
                shade = Math.min(1, 0.4f + 0.6f * (Math.max(normal.dot(light0), 0) + Math.max(normal.dot(light1), 0)));
            }
        }
        int rgba = base + layout.color;
        float red = layout.color < 0 ? color.x : Byte.toUnsignedInt(data.get(rgba)) / 255f;
        float green = layout.color < 0 ? color.y : Byte.toUnsignedInt(data.get(rgba + 1)) / 255f;
        float blue = layout.color < 0 ? color.z : Byte.toUnsignedInt(data.get(rgba + 2)) / 255f;
        vertices.put((byte) Math.round(red * ((light >> 16) & 255) * shade));
        vertices.put((byte) Math.round(green * ((light >> 8) & 255) * shade));
        vertices.put((byte) Math.round(blue * (light & 255) * shade));
        vertices.put(layout.color < 0 ? (byte) Math.round(color.w * 255) : data.get(rgba + 3));
    }

    private static int triangleCount(int mode, int count) {
        return switch (mode) {
            case GL_TRIANGLES -> count / 3;
            case GL_QUADS -> count / 4 * 2;
            case GL_TRIANGLE_STRIP, GL_TRIANGLE_FAN -> Math.max(0, count - 2);
            default -> 0;
        };
    }

    private static int vertexIndex(int mode, int triangle, int corner) {
        return switch (mode) {
            case GL_QUADS -> triangle / 2 * 4 + QUAD_INDICES[(triangle % 2) * 3 + corner];
            case GL_TRIANGLE_STRIP -> triangle + (corner == 2 ? 2 : (corner ^ (triangle & 1)));
            case GL_TRIANGLE_FAN -> corner == 0 ? 0 : triangle + corner;
            default -> triangle * 3 + corner;
        };
    }

    ByteBuffer vertices() {
        return vertices.duplicate().flip();
    }

    List<Draw> draws() {
        return draws;
    }

    Image atlas() {
        return atlas;
    }

    int entityTriangles() {
        return entityTriangles;
    }

    int blockEntityTriangles() {
        return blockEntityTriangles;
    }

    int blockEntityGlyphs() {
        return blockEntityGlyphs;
    }

    @Override
    public void close() {
        releaseRetiredImages();
        textures.values().forEach(Image::releasePixels);
        white.releasePixels();
        memFree(vertices);
    }

    private record Layout(int stride, int position, int uv, int color, int light, int normal) {
        private static final Layout GLYPH = new Layout(20, 0, 12, -1, -1, -1);

        private static Layout of(VertexFormat format) {
            int position = -1, uv = -1, color = -1, light = -1, normal = -1;
            List<VertexFormatElement> elements = format.getElements();
            for (int i = 0; i < elements.size(); i++) {
                VertexFormatElement element = elements.get(i);
                int offset = format.getOffset(i);
                switch (element.usage()) {
                    case POSITION -> {
                        if (element.type() == VertexFormatElement.EnumType.FLOAT && element.elementCount() == 3) {
                            position = offset;
                        }
                    }
                    case UV -> {
                        if (element.elementCount() == 2) {
                            if (element.index() == 0 && element.type() == VertexFormatElement.EnumType.FLOAT) {
                                uv = offset;
                            } else if (element.index() == 1 && element.type() == VertexFormatElement.EnumType.SHORT) {
                                light = offset;
                            }
                        }
                    }
                    case COLOR -> {
                        if (element.type() == VertexFormatElement.EnumType.UBYTE && element.elementCount() == 4) {
                            color = offset;
                        }
                    }
                    case NORMAL -> {
                        if (element.type() == VertexFormatElement.EnumType.BYTE && element.elementCount() == 3) {
                            normal = offset;
                        }
                    }
                    default -> { }
                }
            }
            return new Layout(format.getNextOffset(), position, uv, color, light, normal);
        }
    }

    private record Mesh(ByteBuffer data, Layout layout, int mode, int count) {
    }

    record Material(Image image, VulkanGraphics.Pass pass, float alphaCutoff, boolean lit, boolean lightmapped) {
    }

    static final class Draw {
        final Material material;
        final int firstVertex;
        int vertexCount;

        Draw(Material material, int firstVertex, int vertexCount) {
            this.material = material;
            this.firstVertex = firstVertex;
            this.vertexCount = vertexCount;
        }
    }

    static final class Image {
        final int width;
        final int height;
        ByteBuffer pixels;
        boolean deleted;

        Image(int width, int height, ByteBuffer pixels) {
            this.width = width;
            this.height = height;
            this.pixels = pixels;
        }

        void releasePixels() {
            memFree(pixels);
            pixels = null;
        }
    }
}
