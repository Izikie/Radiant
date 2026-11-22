package net.radiant.render.vulkan;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.world.border.WorldBorderStage;
import net.optifine.Config;
import net.radiant.util.NativeImage;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_SRC_COLOR;
import static org.lwjgl.vulkan.VK10.VK_FILTER_LINEAR;
import static org.lwjgl.vulkan.VK10.VK_FILTER_NEAREST;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT;

/** Extracts legacy draw geometry on the client thread, before its buffers are recycled. */
public final class VulkanObjectCapture implements AutoCloseable {
    static final int STRIDE = 36;
    private static final float UNORM8_TO_UNORM16 = 65535.0f / 255.0f;
    private static final int[] QUAD_INDICES = {0, 1, 2, 2, 3, 0};
    private static final Map<ModelRenderer, List<Mesh>> MODELS = new WeakHashMap<>();
    private static final Map<Integer, CpuImage> CPU_TEXTURES = new HashMap<>();
    private static List<Mesh> compiling;
    private static VulkanObjectCapture active;

    private final Map<Integer, Image> textures = new HashMap<>();
    private final Set<Integer> missingTextureSources = new HashSet<>();
    private final List<Image> retiredImages = new ArrayList<>();
    private final Image white = new Image(1, 1, memAlloc(4).putInt(0, -1));
    private final Image atlas = new Image(0, 0, null);
    private final List<Draw> draws = new ArrayList<>();
    private final List<Draw> outlineDraws = new ArrayList<>();
    private final int[] layerStarts = new int[Layer.values().length];
    private final int[] cameraOverlayQuads = new int[CameraOverlay.values().length];
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f worldProjection = new Matrix4f();
    private final Matrix4f textureMatrix = new Matrix4f();
    private final Matrix4f modelView = new Matrix4f();
    private final Matrix4f transform = new Matrix4f();
    private final Matrix3f normalMatrix = new Matrix3f();
    private final Matrix4f meshModelView = new Matrix4f();
    private final Matrix4f meshTransform = new Matrix4f();
    private final Matrix3f meshNormalMatrix = new Matrix3f();
    private final Vector3f light0 = new Vector3f();
    private final Vector3f light1 = new Vector3f();
    private final Vector3f lightmapColor = new Vector3f();
    private final Vector3f normal = new Vector3f();
    private final Vector4f clip = new Vector4f();
    private final Vector4f color = new Vector4f();
    private final Vector4f entityTint = new Vector4f(0.0f);
    private final ByteBuffer glyph = ByteBuffer.allocate(4 * 20).order(ByteOrder.nativeOrder());
    private ByteBuffer vertices = memAlloc(64 * 1024);
    private int[] lightmap;
    private int atlasId;
    private int cpuTextureLoads;
    private int dynamicTextureLoads;
    private int entityTriangles;
    private int entityShadowQuads;
    private int entityFireQuads;
    private int entityHitboxSegments;
    private int entityLeashTriangles;
    private int entityNameTagGlyphs;
    private int entityTintTriangles;
    private int entityEyeTriangles;
    private int entityNameTagThroughWallGlyphs;
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
    private boolean entityLeash;
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
        entityHitboxSegments = 0;
        entityLeashTriangles = 0;
        entityNameTagGlyphs = 0;
        entityTintTriangles = 0;
        entityEyeTriangles = 0;
        entityNameTagThroughWallGlyphs = 0;
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

    void setWorldProjection(Matrix4fc matrix) {
        worldProjection.set(matrix);
    }

    void begin(int[] lightmap, int atlasId, Phase phase, Matrix4fc cameraProjection) {
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
        if (phase.layer == Layer.WORLD) {
            projection.set(worldProjection);
        } else if (cameraProjection != null) {
            projection.set(cameraProjection);
        }
        active = this;
    }

    void beginWorldBorder(int[] lightmap, WorldBorderStage stage) {
        begin(lightmap, 0, Phase.WORLD_BORDER, null);
        worldBorderStage = stage;
    }

    public static void beginBlockEntities() {
        if (active != null) {
            active.phase = Phase.BLOCK_ENTITIES;
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

    public static void setEntityTint(FloatBuffer color) {
        if (active != null) {
            active.entityTint.set(color.get(0), color.get(1), color.get(2), color.get(3));
        }
    }

    public static void clearEntityTint() {
        if (active != null) {
            active.entityTint.zero();
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

    public static void beginEntityHitbox() {
        if (active != null && active.phase == Phase.ENTITIES) {
            active.phase = Phase.ENTITY_HITBOX;
        }
    }

    public static void endEntityHitbox() {
        if (active != null && active.phase == Phase.ENTITY_HITBOX) {
            active.phase = Phase.ENTITIES;
        }
    }

    public static void beginEntityNameTag() {
        if (active != null && active.phase == Phase.ENTITIES) {
            active.phase = Phase.ENTITY_NAME_TAG;
        }
    }

    public static void endEntityNameTag() {
        if (active != null && active.phase == Phase.ENTITY_NAME_TAG) {
            active.phase = Phase.ENTITIES;
        }
    }

    public static void beginEntityLeash() {
        if (active != null) {
            active.entityLeash = true;
        }
    }

    public static void endEntityLeash() {
        if (active != null) {
            active.entityLeash = false;
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

    public static void beginModel(ModelRenderer model) {
        if (VulkanTerrainPreview.ENABLED) {
            GlStateManager.beginModelViewList();
            compiling = new ArrayList<>();
            MODELS.put(model, compiling);
        }
    }

    public static void endModel() {
        if (compiling != null) {
            compiling = null;
            GlStateManager.endModelViewList();
        }
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
        boolean lines = active != null && active.phase.drawsLines();
        if (lines ? lineCount(mode, count) == 0 : triangleCount(mode, count) == 0) {
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
            Matrix4fc local = GlStateManager.modelViewMatrix();
            Matrix4f localTransform = (local.properties() & Matrix4fc.PROPERTY_IDENTITY) != 0 ? null : new Matrix4f(local);
            compiling.add(new Mesh(copy, layout, mode, count, localTransform));
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
        Material material = active.material();
        active.append(new Mesh(data, Layout.GLYPH, GL_TRIANGLE_STRIP, 4), material);
        switch (active.phase) {
            case ENTITY_NAME_TAG -> {
                active.entityNameTagGlyphs++;
                if (!material.pipeline.state.depthTest()) {
                    active.entityNameTagThroughWallGlyphs++;
                }
            }
            case BLOCK_ENTITIES -> active.blockEntityGlyphs++;
            case SCREEN -> active.screenGlyphs++;
            default -> { }
        }
    }

    private static void putGlyphVertex(ByteBuffer data, float x, float y, float u, float v) {
        data.putFloat(x).putFloat(y).putFloat(0).putFloat(u).putFloat(v);
    }

    private Material material() {
        if (phase.layer.ui) {
            projection.set(GlStateManager.projectionMatrix());
        }
        if (capturesTextureMatrix()) {
            textureMatrix.set(GlStateManager.textureMatrix());
        }
        GlStateManager.copySubmittedColor(color);
        color.set(Math.clamp(color.x, 0, 1), Math.clamp(color.y, 0, 1),
                Math.clamp(color.z, 0, 1), Math.clamp(color.w, 0, 1));
        modelView.set(GlStateManager.modelViewMatrix());
        projection.mul(modelView, transform);
        if (phase == Phase.ENTITY_OUTLINES) {
            return new Material(white, new PipelineKey(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, VulkanGraphics.Pass.SKY.state, true, 1.0f),
                    -1, false, false, false, false, false);
        }
        Image image = white;
        int texture = GlStateManager.getBoundTexture();
        var draw = GlStateManager.drawState();
        if (texture != 0 && draw.textured()) {
            image = texture == atlasId ? atlas : texture(texture);
        }
        boolean emissiveEyes = phase == Phase.ENTITIES && Config.getRenderGlobal().renderOverlayEyes;
        VulkanGraphics.State state = VulkanOverlayState.capture(draw);
        boolean lit = draw.lit();
        if (lit) {
            GlStateManager.copyLightDirection(0, light0);
            GlStateManager.copyLightDirection(1, light1);
            light0.normalize();
            light1.normalize();
            modelView.normal(normalMatrix);
        }
        int topology = phase.drawsLines() ? VK_PRIMITIVE_TOPOLOGY_LINE_LIST : VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
        VulkanGraphics.Blend factors = state.blend();
        boolean glintColorBlend = glint != null && factors != null && factors.sourceColor() == VK_BLEND_FACTOR_SRC_COLOR
                && factors.destinationColor() == VK_BLEND_FACTOR_ONE;
        // The legacy block selection pass requests a two-pixel outline.
        float lineWidth = phase == Phase.SELECTION ? 2.0f : 1.0f;
        return new Material(image, new PipelineKey(topology, state, false, lineWidth), draw.alphaCutoff(),
                lit, GlStateManager.isLightmapTextureEnabled(), glintColorBlend, emissiveEyes,
                image == atlas && !GlStateManager.isTextureMipmapped(texture));
    }

    private boolean capturesTextureMatrix() {
        return glint != null || phase.layer.ui || phase == Phase.BLOCK_DAMAGE;
    }

    static void registerTexture(int id, NativeImage image, boolean blur, boolean clamp) {
        CPU_TEXTURES.put(id, new CpuImage(image.getWidth(), image.getHeight(), image.getData().clone(), false, blur, clamp));
    }

    static void registerTexture(int id, int width, int height, int[] argb) {
        CpuImage source = CPU_TEXTURES.get(id);
        if (source == null || !source.dynamic || source.width != width || source.height != height) {
            source = new CpuImage(width, height, new byte[Math.multiplyExact(Math.multiplyExact(width, height), 4)], true, false, false);
            CPU_TEXTURES.put(id, source);
        }
        VulkanAtlasUpdates.writeRgba(ByteBuffer.wrap(source.rgba), argb, 0, width * height);
    }

    static void forgetTexture(int id) {
        CPU_TEXTURES.remove(id);
    }

    static boolean setTextureClamped(int id, boolean clamp) {
        CpuImage source = CPU_TEXTURES.get(id);
        if (source == null || source.clamp == clamp) {
            return false;
        }
        CPU_TEXTURES.put(id, new CpuImage(source.width, source.height, source.rgba, source.dynamic, source.blur, clamp));
        return true;
    }

    private Image texture(int id) {
        Image cached = textures.get(id);
        if (cached != null) {
            return cached;
        }
        CpuImage source = CPU_TEXTURES.get(id);
        if (source == null) {
            missingTextureSources.add(id);
            return white;
        }
        ByteBuffer pixels = memAlloc(source.rgba.length);
        try {
            pixels.put(source.rgba).flip();
            int addressMode = source.clamp ? VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE : VK_SAMPLER_ADDRESS_MODE_REPEAT;
            int filter = source.blur ? VK_FILTER_LINEAR : VK_FILTER_NEAREST;
            Image image = new Image(source.width, source.height, pixels, filter, addressMode, addressMode);
            textures.put(id, image);
            cpuTextureLoads++;
            if (source.dynamic) {
                dynamicTextureLoads++;
            }
            return image;
        } catch (RuntimeException | Error e) {
            memFree(pixels);
            throw e;
        }
    }

    int cpuTextureLoads() {
        return cpuTextureLoads;
    }

    int missingTextureSources() {
        return missingTextureSources.size();
    }

    int dynamicTextureLoads() {
        return dynamicTextureLoads;
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
        Matrix4fc vertexTransform = transform;
        Matrix3f vertexNormal = normalMatrix;
        if (mesh.localTransform != null) {
            modelView.mul(mesh.localTransform, meshModelView);
            vertexTransform = projection.mul(meshModelView, meshTransform);
            if (material.lit) {
                vertexNormal = meshModelView.normal(meshNormalMatrix);
            }
        }
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
                writeVertex(mesh, material, vertexIndex(mesh.mode, primitive, corner), vertexTransform, vertexNormal);
            }
        }
        List<Draw> target = phase == Phase.ENTITY_OUTLINES ? outlineDraws : draws;
        int last = target.size() - 1;
        boolean layerBoundary = target == draws && target.size() == layerStarts[phase.layer.ordinal()];
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
            case ENTITIES -> {
                entityTriangles += triangles;
                if (material.emissiveEyes) {
                    entityEyeTriangles += triangles;
                }
                if (entityTint.w > 0) {
                    entityTintTriangles += triangles;
                }
                if (entityLeash) {
                    entityLeashTriangles += triangles;
                }
            }
            case ENTITY_SHADOWS -> {
                entityTriangles += triangles;
                entityShadowQuads += triangles / 2;
            }
            case ENTITY_FIRE -> {
                entityTriangles += triangles;
                entityFireQuads += triangles / 2;
            }
            case ENTITY_HITBOX -> entityHitboxSegments += primitiveCount;
            case ENTITY_NAME_TAG -> entityTriangles += triangles;
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
                if (material.pipeline.state.blend() != null && !material.pipeline.state.depthWrite()) {
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

    private void writeVertex(Mesh mesh, Material material, int index, Matrix4fc vertexTransform, Matrix3f vertexNormal) {
        ByteBuffer data = mesh.data;
        Layout layout = mesh.layout;
        int base = index * layout.stride;
        int p = base + layout.position;
        clip.set(data.getFloat(p), data.getFloat(p + 4), data.getFloat(p + 8), 1).mul(vertexTransform);
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

        if (material.lightmapped) {
            int coordinate = base + layout.light;
            float x = layout.light < 0 ? OpenGlHelper.lastBrightnessX : data.getShort(coordinate);
            float y = layout.light < 0 ? OpenGlHelper.lastBrightnessY : data.getShort(coordinate + 2);
            sampleLightmap(x, y);
        } else {
            lightmapColor.set(255.0f);
        }
        float shade = 1;
        if (material.lit && layout.normal >= 0) {
            int n = base + layout.normal;
            normal.set(data.get(n), data.get(n + 1), data.get(n + 2));
            if (normal.lengthSquared() > 0) {
                normal.mul(vertexNormal).normalize();
                shade = 0.4f + 0.6f * (Math.max(normal.dot(light0), 0) + Math.max(normal.dot(light1), 0));
            }
        }
        int rgba = base + layout.color;
        float red = layout.color < 0 ? color.x : Byte.toUnsignedInt(data.get(rgba)) / 255f;
        float green = layout.color < 0 ? color.y : Byte.toUnsignedInt(data.get(rgba + 1)) / 255f;
        float blue = layout.color < 0 ? color.z : Byte.toUnsignedInt(data.get(rgba + 2)) / 255f;
        float colorScale = (1.0f - entityTint.w) * UNORM8_TO_UNORM16;
        // Fixed-function lighting clamps the lit vertex color before the texture combiners.
        vertices.putShort((short) Math.round(Math.min(red * shade, 1.0f) * lightmapColor.x * colorScale));
        vertices.putShort((short) Math.round(Math.min(green * shade, 1.0f) * lightmapColor.y * colorScale));
        vertices.putShort((short) Math.round(Math.min(blue * shade, 1.0f) * lightmapColor.z * colorScale));
        if (entityTint.w > 0) {
            // The fixed-function combiner interpolates RGB before lightmapping and keeps texture alpha.
            vertices.putShort((short) 65535);
            vertices.put((byte) Math.round(entityTint.x * entityTint.w * lightmapColor.x));
            vertices.put((byte) Math.round(entityTint.y * entityTint.w * lightmapColor.y));
            vertices.put((byte) Math.round(entityTint.z * entityTint.w * lightmapColor.z));
            vertices.put((byte) 0);
        } else {
            float alpha = layout.color < 0 ? color.w * 255.0f : Byte.toUnsignedInt(data.get(rgba + 3));
            vertices.putShort((short) Math.round(alpha * UNORM8_TO_UNORM16));
            vertices.putInt(0);
        }
    }

    private void sampleLightmap(float x, float y) {
        // The lightmap texture transform places multiples of 16 at texel centers.
        x = Math.clamp(x / 16.0f, 0.0f, 15.0f);
        y = Math.clamp(y / 16.0f, 0.0f, 15.0f);
        int left = (int) x;
        int top = (int) y;
        float u = x - left;
        float v = y - top;
        int topLeft = lightmap[left + 16 * top];
        if (u == 0.0f && v == 0.0f) {
            lightmapColor.set((topLeft >>> 16) & 255, (topLeft >>> 8) & 255, topLeft & 255);
            return;
        }

        int right = Math.min(left + 1, 15);
        int bottom = Math.min(top + 1, 15);
        int topRight = lightmap[right + 16 * top];
        int bottomLeft = lightmap[left + 16 * bottom];
        int bottomRight = lightmap[right + 16 * bottom];
        lightmapColor.set(interpolateLightChannel(topLeft, topRight, bottomLeft, bottomRight, 16, u, v),
                interpolateLightChannel(topLeft, topRight, bottomLeft, bottomRight, 8, u, v),
                interpolateLightChannel(topLeft, topRight, bottomLeft, bottomRight, 0, u, v));
    }

    private static float interpolateLightChannel(int topLeft, int topRight, int bottomLeft, int bottomRight,
                                                 int shift, float u, float v) {
        int leftTop = (topLeft >>> shift) & 255;
        int leftBottom = (bottomLeft >>> shift) & 255;
        float top = leftTop + (((topRight >>> shift) & 255) - leftTop) * u;
        float bottom = leftBottom + (((bottomRight >>> shift) & 255) - leftBottom) * u;
        return top + (bottom - top) * v;
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

    int entityHitboxSegments() {
        return entityHitboxSegments;
    }

    int entityLeashTriangles() {
        return entityLeashTriangles;
    }

    int entityNameTagGlyphs() {
        return entityNameTagGlyphs;
    }

    int entityTintTriangles() {
        return entityTintTriangles;
    }

    int entityEyeTriangles() {
        return entityEyeTriangles;
    }

    int entityNameTagThroughWallGlyphs() {
        return entityNameTagThroughWallGlyphs;
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
        ENTITIES, ENTITY_SHADOWS, ENTITY_FIRE, ENTITY_HITBOX, ENTITY_OUTLINES, ENTITY_NAME_TAG, LIGHTNING, BLOCK_ENTITIES,
        LIT_PARTICLES, PARTICLE_BILLBOARDS,
        RAIN, SNOW, SELECTION, BLOCK_DAMAGE, WORLD_BORDER,
        FIRST_PERSON(Layer.FIRST_PERSON), CAMERA_OVERLAYS(Layer.FIRST_PERSON), HUD(Layer.HUD), SCREEN(Layer.SCREEN),
        ACHIEVEMENT(Layer.ACHIEVEMENT);

        private final Layer layer;

        Phase() {
            this(Layer.WORLD);
        }

        Phase(Layer layer) {
            this.layer = layer;
        }

        private boolean drawsLines() {
            return this == SELECTION || this == ENTITY_HITBOX;
        }
    }

    public enum CameraOverlay {
        BLOCK, WATER, FIRE
    }

    public enum Glint {
        ITEM, ARMOR
    }

    enum Layer {
        WORLD(false), FIRST_PERSON(false), HUD(true), SCREEN(true), ACHIEVEMENT(true);

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

    private record Mesh(ByteBuffer data, Layout layout, int mode, int count, Matrix4f localTransform) {
        private Mesh(ByteBuffer data, Layout layout, int mode, int count) {
            this(data, layout, mode, count, null);
        }
    }

    record PipelineKey(int topology, VulkanGraphics.State state, boolean outlineMask, float lineWidth) {
    }

    record Material(Image image, PipelineKey pipeline, float alphaCutoff, boolean lit, boolean lightmapped,
                    boolean glintColorBlend, boolean emissiveEyes, boolean atlasBaseLevel) {
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

    private record CpuImage(int width, int height, byte[] rgba, boolean dynamic, boolean blur, boolean clamp) {
    }

    static final class Image {
        final int width;
        final int height;
        final int filter;
        final int addressModeU;
        final int addressModeV;
        ByteBuffer pixels;
        boolean deleted;

        Image(int width, int height, ByteBuffer pixels) {
            this(width, height, pixels, VK_FILTER_NEAREST, VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE, VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
        }

        Image(int width, int height, ByteBuffer pixels, int filter, int addressModeU, int addressModeV) {
            this.width = width;
            this.height = height;
            this.pixels = pixels;
            this.filter = filter;
            this.addressModeU = addressModeU;
            this.addressModeV = addressModeV;
        }

        void releasePixels() {
            memFree(pixels);
            pixels = null;
        }
    }
}
