package net.radiant.render.vulkan;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.world.border.WorldBorderStage;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.GL_MIRRORED_REPEAT;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_SRC_COLOR;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT;

/** Extracts legacy draw geometry on the client thread, before its buffers are recycled. */
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
    private final List<Draw> outlineDraws = new ArrayList<>();
    private final int[] layerStarts = new int[Layer.values().length];
    private final int[] cameraOverlayQuads = new int[CameraOverlay.values().length];
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f textureMatrix = new Matrix4f();
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
    private int entityShadowQuads;
    private int entityFireQuads;
    private int outlineTriangles;
    private int lightningTriangles;
    private int blockEntityTriangles;
    private int blockEntityGlyphs;
    private int particleTriangles;
    private int terrainParticleTriangles;
    private int translucentParticleTriangles;
    private int litParticleTriangles;
    private int rainQuads;
    private int snowQuads;
    private int selectionSegments;
    private int blockDamageQuads;
    private int worldBorderQuads;
    private WorldBorderStage worldBorderStage;
    private int handTriangles;
    private int heldItemTriangles;
    private int heldGlintTriangles;
    private int screenGlintTriangles;
    private int armorGlintTriangles;
    private int hudTriangles;
    private int hudItemTriangles;
    private int screenTriangles;
    private int screenItemTriangles;
    private int screenGlyphs;
    private boolean holdingItem;
    private Glint glint;
    private CameraOverlay cameraOverlay;
    private Phase phase;

    void reset() {
        releaseRetiredImages();
        draws.clear();
        outlineDraws.clear();
        vertices.clear();
        entityTriangles = 0;
        entityShadowQuads = 0;
        entityFireQuads = 0;
        outlineTriangles = 0;
        lightningTriangles = 0;
        blockEntityTriangles = 0;
        blockEntityGlyphs = 0;
        particleTriangles = 0;
        terrainParticleTriangles = 0;
        translucentParticleTriangles = 0;
        litParticleTriangles = 0;
        rainQuads = 0;
        snowQuads = 0;
        selectionSegments = 0;
        blockDamageQuads = 0;
        worldBorderQuads = 0;
        worldBorderStage = null;
        Arrays.fill(layerStarts, -1);
        Arrays.fill(cameraOverlayQuads, 0);
        layerStarts[Layer.WORLD.ordinal()] = 0;
        handTriangles = 0;
        heldItemTriangles = 0;
        heldGlintTriangles = 0;
        screenGlintTriangles = 0;
        armorGlintTriangles = 0;
        hudTriangles = 0;
        hudItemTriangles = 0;
        screenTriangles = 0;
        screenItemTriangles = 0;
        screenGlyphs = 0;
    }

    void begin(int[] lightmap, int atlasId, Phase phase) {
        this.lightmap = lightmap;
        this.atlasId = atlasId;
        this.phase = phase;
        cameraOverlay = null;
        glint = null;
        int layer = phase.layer.ordinal();
        if (layerStarts[layer] < 0) {
            layerStarts[layer] = draws.size();
        }
        if (phase == Phase.FIRST_PERSON) {
            holdingItem = false;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer matrix = stack.mallocFloat(16);
            glGetFloatv(GL_PROJECTION_MATRIX, matrix);
            projection.set(matrix);
        }
        if (phase != Phase.CAMERA_OVERLAYS && phase != Phase.WORLD_BORDER) {
            readLights();
        }
        active = this;
    }

    void beginWorldBorder(int[] lightmap, WorldBorderStage stage) {
        begin(lightmap, 0, Phase.WORLD_BORDER);
        worldBorderStage = stage;
    }

    public static void beginBlockEntities() {
        if (active != null) {
            active.phase = Phase.BLOCK_ENTITIES;
            active.readLights();
        }
    }

    public static void beginParticleBillboards() {
        if (active != null) {
            active.phase = Phase.PARTICLE_BILLBOARDS;
        }
    }

    public static void beginBlockEntityDamage() {
        if (active != null && active.phase == Phase.BLOCK_ENTITIES) {
            active.phase = Phase.BLOCK_DAMAGE;
        }
    }

    public static void beginPrecipitationBatch(boolean snow) {
        if (active != null) {
            active.phase = snow ? Phase.SNOW : Phase.RAIN;
        }
    }

    public static void setFirstPersonItem(boolean holdingItem) {
        if (active != null && active.phase == Phase.FIRST_PERSON) {
            active.holdingItem = holdingItem;
        }
    }

    public static void setCameraOverlay(CameraOverlay overlay) {
        if (active != null && active.phase == Phase.CAMERA_OVERLAYS) {
            active.cameraOverlay = overlay;
        }
    }

    public static void refreshLights() {
        if (active != null) {
            active.readLights();
        }
    }

    public static Glint beginGlint(Glint type) {
        if (active == null) {
            return null;
        }
        Glint previous = active.glint;
        active.glint = type;
        return previous;
    }

    public static void endGlint(Glint previous) {
        if (active != null) {
            active.glint = previous;
        }
    }

    public static void end() {
        active = null;
    }

    public static void beginEntityOutlines() {
        if (active != null) {
            active.phase = Phase.ENTITY_OUTLINES;
        }
    }

    public static void beginEntityShadow() {
        if (active != null && active.phase == Phase.ENTITIES) {
            active.phase = Phase.ENTITY_SHADOWS;
        }
    }

    public static void endEntityShadow() {
        if (active != null && active.phase == Phase.ENTITY_SHADOWS) {
            active.phase = Phase.ENTITIES;
        }
    }

    public static void beginEntityFire() {
        if (active != null && active.phase == Phase.ENTITIES) {
            active.phase = Phase.ENTITY_FIRE;
        }
    }

    public static void endEntityFire() {
        if (active != null && active.phase == Phase.ENTITY_FIRE) {
            active.phase = Phase.ENTITIES;
        }
    }

    public static void beginLightning() {
        if (active != null && active.phase == Phase.ENTITIES) {
            active.phase = Phase.LIGHTNING;
        }
    }

    public static void endLightning() {
        if (active != null && active.phase == Phase.LIGHTNING) {
            active.phase = Phase.ENTITIES;
        }
    }

    public static void endEntityOutlines() {
        if (active != null && active.phase == Phase.ENTITY_OUTLINES) {
            active.phase = Phase.ENTITIES;
        }
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
        boolean selection = active != null && active.phase == Phase.SELECTION;
        if (selection ? lineCount(mode, count) == 0 : triangleCount(mode, count) == 0) {
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
        switch (active.phase) {
            case BLOCK_ENTITIES -> active.blockEntityGlyphs++;
            case SCREEN -> active.screenGlyphs++;
            default -> { }
        }
    }

    private static void putGlyphVertex(ByteBuffer data, float x, float y, float u, float v) {
        data.putFloat(x).putFloat(y).putFloat(0).putFloat(u).putFloat(v);
    }

    private Material material() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer values = stack.mallocFloat(16);
            if (phase.layer.ui) {
                glGetFloatv(GL_PROJECTION_MATRIX, values);
                projection.set(values);
            }
            if (capturesTextureMatrix()) {
                glGetFloatv(GL_TEXTURE_MATRIX, values);
                textureMatrix.set(values);
            }
            glGetFloatv(GL_MODELVIEW_MATRIX, values);
            modelView.set(values);
            projection.mul(modelView, transform);
            glGetFloatv(GL_CURRENT_COLOR, values);
            color.set(Math.clamp(values.get(0), 0, 1), Math.clamp(values.get(1), 0, 1),
                    Math.clamp(values.get(2), 0, 1), Math.clamp(values.get(3), 0, 1));
        }
        if (phase == Phase.ENTITY_OUTLINES) {
            return new Material(white, new PipelineKey(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, VulkanGraphics.Pass.SKY.state, true),
                    -1, false, false, false);
        }
        Image image = white;
        int texture = GlStateManager.getBoundTexture();
        if (texture != 0 && glIsEnabled(GL_TEXTURE_2D)) {
            image = texture == atlasId ? atlas : texture(texture);
        }
        boolean blend = glIsEnabled(GL_BLEND);
        boolean depthWrite = glGetBoolean(GL_DEPTH_WRITEMASK);
        boolean depthTest = glIsEnabled(GL_DEPTH_TEST);
        VulkanGraphics.State state;
        if (glint != null || phase.layer.ui || phase == Phase.CAMERA_OVERLAYS || phase == Phase.SELECTION
                || phase == Phase.BLOCK_DAMAGE || phase == Phase.WORLD_BORDER || phase == Phase.LIGHTNING
                || phase == Phase.ENTITY_SHADOWS || phase == Phase.ENTITY_FIRE) {
            state = VulkanOverlayState.capture(depthTest, depthWrite, blend);
        } else {
            VulkanGraphics.Pass pass = !depthTest ? VulkanGraphics.Pass.OBJECT_OVERLAY
                    : blend ? (depthWrite ? VulkanGraphics.Pass.BLENDED_DEPTH_WRITE : VulkanGraphics.Pass.OBJECT_BLEND)
                    : (depthWrite ? VulkanGraphics.Pass.OPAQUE : VulkanGraphics.Pass.OBJECT_READ_DEPTH);
            state = pass.state;
        }
        boolean lit = glIsEnabled(GL_LIGHTING);
        if (lit) {
            modelView.normal(normalMatrix);
        }
        int topology = phase == Phase.SELECTION ? VK_PRIMITIVE_TOPOLOGY_LINE_LIST : VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
        VulkanGraphics.Blend factors = state.blend();
        boolean glintColorBlend = glint != null && factors != null && factors.sourceColor() == VK_BLEND_FACTOR_SRC_COLOR
                && factors.destinationColor() == VK_BLEND_FACTOR_ONE;
        return new Material(image, new PipelineKey(topology, state, false), glIsEnabled(GL_ALPHA_TEST) ? glGetFloat(GL_ALPHA_TEST_REF) : -1,
                lit, GlStateManager.isLightmapTextureEnabled(), glintColorBlend);
    }

    private boolean capturesTextureMatrix() {
        return glint != null || phase.layer.ui || phase == Phase.BLOCK_DAMAGE;
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
            Image image = new Image(width, height, pixels, addressMode(GL_TEXTURE_WRAP_S), addressMode(GL_TEXTURE_WRAP_T));
            textures.put(id, image);
            return image;
        } catch (RuntimeException | Error e) {
            memFree(pixels);
            throw e;
        }
    }

    private static int addressMode(int parameter) {
        return switch (glGetTexParameteri(GL_TEXTURE_2D, parameter)) {
            case GL_REPEAT -> VK_SAMPLER_ADDRESS_MODE_REPEAT;
            case GL_MIRRORED_REPEAT -> VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT;
            default -> VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
        };
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
        boolean lines = material.pipeline.topology == VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
        int primitiveCount = lines ? lineCount(mesh.mode, mesh.count) : triangleCount(mesh.mode, mesh.count);
        int verticesPerPrimitive = lines ? 2 : 3;
        int vertexCount = primitiveCount * verticesPerPrimitive;
        int firstVertex = vertices.position() / STRIDE;
        int required = vertices.position() + vertexCount * STRIDE;
        if (required > vertices.capacity()) {
            int position = vertices.position();
            vertices = memRealloc(vertices, Math.max(required, vertices.capacity() * 2)).clear().position(position);
        }
        for (int primitive = 0; primitive < primitiveCount; primitive++) {
            for (int corner = 0; corner < verticesPerPrimitive; corner++) {
                writeVertex(mesh, material, vertexIndex(mesh.mode, primitive, corner));
            }
        }
        boolean outline = phase == Phase.ENTITY_OUTLINES;
        List<Draw> target = outline ? outlineDraws : draws;
        int last = target.size() - 1;
        boolean layerBoundary = !outline && target.size() == layerStarts[phase.layer.ordinal()];
        if (last >= 0 && !layerBoundary && target.get(last).firstVertex + target.get(last).vertexCount == firstVertex
                && target.get(last).material.equals(material)) {
            Draw previous = target.get(last);
            previous.vertexCount += vertexCount;
        } else {
            target.add(new Draw(material, firstVertex, vertexCount));
        }
        int triangles = primitiveCount;
        if (glint == Glint.ARMOR && phase == Phase.ENTITIES) {
            armorGlintTriangles += triangles;
        } else if (glint == Glint.ITEM) {
            if (phase == Phase.FIRST_PERSON) {
                heldGlintTriangles += triangles;
            } else if (phase == Phase.SCREEN) {
                screenGlintTriangles += triangles;
            }
        }
        switch (phase) {
            case ENTITIES -> entityTriangles += triangles;
            case ENTITY_SHADOWS -> {
                entityTriangles += triangles;
                entityShadowQuads += triangles / 2;
            }
            case ENTITY_FIRE -> {
                entityTriangles += triangles;
                entityFireQuads += triangles / 2;
            }
            case ENTITY_OUTLINES -> outlineTriangles += triangles;
            case LIGHTNING -> lightningTriangles += triangles;
            case BLOCK_ENTITIES -> blockEntityTriangles += triangles;
            case LIT_PARTICLES -> {
                particleTriangles += triangles;
                litParticleTriangles += triangles;
            }
            case PARTICLE_BILLBOARDS -> {
                particleTriangles += triangles;
                if (material.image == atlas) {
                    terrainParticleTriangles += triangles;
                }
                if (material.pipeline.state == VulkanGraphics.Pass.OBJECT_BLEND.state) {
                    translucentParticleTriangles += triangles;
                }
            }
            case RAIN -> rainQuads += triangles / 2;
            case SNOW -> snowQuads += triangles / 2;
            case SELECTION -> selectionSegments += primitiveCount;
            case BLOCK_DAMAGE -> blockDamageQuads += triangles / 2;
            case WORLD_BORDER -> worldBorderQuads += triangles / 2;
            case FIRST_PERSON -> {
                if (holdingItem) {
                    heldItemTriangles += triangles;
                } else {
                    handTriangles += triangles;
                }
            }
            case CAMERA_OVERLAYS -> {
                if (cameraOverlay != null) {
                    cameraOverlayQuads[cameraOverlay.ordinal()] += triangles / 2;
                }
            }
            case HUD -> {
                hudTriangles += triangles;
                if (material.image == atlas) {
                    hudItemTriangles += triangles;
                }
            }
            case SCREEN -> {
                screenTriangles += triangles;
                if (material.image == atlas) {
                    screenItemTriangles += triangles;
                }
            }
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
        float u = layout.uv < 0 ? 0 : data.getFloat(uv);
        float v = layout.uv < 0 ? 0 : data.getFloat(uv + 4);
        if (capturesTextureMatrix() && layout.uv >= 0) {
            vertices.putFloat(textureMatrix.m00() * u + textureMatrix.m10() * v + textureMatrix.m30());
            vertices.putFloat(textureMatrix.m01() * u + textureMatrix.m11() * v + textureMatrix.m31());
        } else {
            vertices.putFloat(u).putFloat(v);
        }

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

    private static int lineCount(int mode, int count) {
        return switch (mode) {
            case GL_LINES -> count / 2;
            case GL_LINE_STRIP -> Math.max(0, count - 1);
            default -> 0;
        };
    }

    private static int triangleCount(int mode, int count) {
        return switch (mode) {
            case GL_TRIANGLES -> count / 3;
            case GL_QUADS -> count / 4 * 2;
            case GL_TRIANGLE_STRIP, GL_TRIANGLE_FAN -> Math.max(0, count - 2);
            default -> 0;
        };
    }

    private static int vertexIndex(int mode, int primitive, int corner) {
        return switch (mode) {
            case GL_LINES -> primitive * 2 + corner;
            case GL_LINE_STRIP -> primitive + corner;
            case GL_QUADS -> primitive / 2 * 4 + QUAD_INDICES[(primitive % 2) * 3 + corner];
            case GL_TRIANGLE_STRIP -> primitive + (corner == 2 ? 2 : (corner ^ (primitive & 1)));
            case GL_TRIANGLE_FAN -> corner == 0 ? 0 : primitive + corner;
            default -> primitive * 3 + corner;
        };
    }

    ByteBuffer vertices() {
        return vertices.duplicate().flip();
    }

    List<Draw> draws() {
        return draws;
    }

    List<Draw> outlineDraws() {
        return outlineDraws;
    }

    int drawStart(Layer layer) {
        return drawBoundary(layer.ordinal());
    }

    int drawEnd(Layer layer) {
        return drawBoundary(layer.ordinal() + 1);
    }

    private int drawBoundary(int firstLayer) {
        for (int i = firstLayer; i < layerStarts.length; i++) {
            if (layerStarts[i] >= 0) {
                return layerStarts[i];
            }
        }
        return draws.size();
    }

    Image atlas() {
        return atlas;
    }

    int entityTriangles() {
        return entityTriangles;
    }

    int entityShadowQuads() {
        return entityShadowQuads;
    }

    int entityFireQuads() {
        return entityFireQuads;
    }

    int outlineTriangles() {
        return outlineTriangles;
    }

    int lightningTriangles() {
        return lightningTriangles;
    }

    int blockEntityTriangles() {
        return blockEntityTriangles;
    }

    int blockEntityGlyphs() {
        return blockEntityGlyphs;
    }

    int particleTriangles() {
        return particleTriangles;
    }

    int terrainParticleTriangles() {
        return terrainParticleTriangles;
    }

    int translucentParticleTriangles() {
        return translucentParticleTriangles;
    }

    int litParticleTriangles() {
        return litParticleTriangles;
    }

    int rainQuads() {
        return rainQuads;
    }

    int snowQuads() {
        return snowQuads;
    }

    int selectionSegments() {
        return selectionSegments;
    }

    int blockDamageQuads() {
        return blockDamageQuads;
    }

    int worldBorderQuads() {
        return worldBorderQuads;
    }

    WorldBorderStage worldBorderStage() {
        return worldBorderStage;
    }

    int handTriangles() {
        return handTriangles;
    }

    int heldItemTriangles() {
        return heldItemTriangles;
    }

    int heldGlintTriangles() {
        return heldGlintTriangles;
    }

    int screenGlintTriangles() {
        return screenGlintTriangles;
    }

    int armorGlintTriangles() {
        return armorGlintTriangles;
    }

    int cameraOverlayQuads(CameraOverlay overlay) {
        return cameraOverlayQuads[overlay.ordinal()];
    }

    int hudTriangles() {
        return hudTriangles;
    }

    int hudItemTriangles() {
        return hudItemTriangles;
    }

    int screenTriangles() {
        return screenTriangles;
    }

    int screenItemTriangles() {
        return screenItemTriangles;
    }

    int screenGlyphs() {
        return screenGlyphs;
    }

    @Override
    public void close() {
        releaseRetiredImages();
        textures.values().forEach(Image::releasePixels);
        white.releasePixels();
        memFree(vertices);
    }

    enum Phase {
        ENTITIES, ENTITY_SHADOWS, ENTITY_FIRE, ENTITY_OUTLINES, LIGHTNING, BLOCK_ENTITIES, LIT_PARTICLES, PARTICLE_BILLBOARDS,
        RAIN, SNOW, SELECTION, BLOCK_DAMAGE, WORLD_BORDER,
        FIRST_PERSON(Layer.FIRST_PERSON), CAMERA_OVERLAYS(Layer.FIRST_PERSON), HUD(Layer.HUD), SCREEN(Layer.SCREEN);

        private final Layer layer;

        Phase() {
            this(Layer.WORLD);
        }

        Phase(Layer layer) {
            this.layer = layer;
        }
    }

    public enum CameraOverlay {
        BLOCK, WATER, FIRE
    }

    public enum Glint {
        ITEM, ARMOR
    }

    enum Layer {
        WORLD(false), FIRST_PERSON(false), HUD(true), SCREEN(true);

        private final boolean ui;

        Layer(boolean ui) {
            this.ui = ui;
        }
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

    record PipelineKey(int topology, VulkanGraphics.State state, boolean outlineMask) {
    }

    record Material(Image image, PipelineKey pipeline, float alphaCutoff, boolean lit, boolean lightmapped,
                    boolean glintColorBlend) {
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
        final int addressModeU;
        final int addressModeV;
        ByteBuffer pixels;
        boolean deleted;

        Image(int width, int height, ByteBuffer pixels) {
            this(width, height, pixels, VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE, VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
        }

        Image(int width, int height, ByteBuffer pixels, int addressModeU, int addressModeV) {
            this.width = width;
            this.height = height;
            this.pixels = pixels;
            this.addressModeU = addressModeU;
            this.addressModeV = addressModeV;
        }

        void releasePixels() {
            memFree(pixels);
            pixels = null;
        }
    }
}
