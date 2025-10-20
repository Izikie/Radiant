package net.minecraft.client.renderer.block.model;

import com.google.gson.*;
import net.minecraft.util.json.JsonUtils;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;

public class ModelBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelBlock.class);
    static final Gson SERIALIZER = new GsonBuilder()
            .registerTypeAdapter(ModelBlock.class, new Deserializer())
            .registerTypeAdapter(BlockPart.class, new BlockPart.Deserializer())
            .registerTypeAdapter(BlockPartFace.class, new BlockPartFace.Deserializer())
            .registerTypeAdapter(BlockFaceUV.class, new BlockFaceUV.Deserializer())
            .registerTypeAdapter(ItemTransformVec3f.class, new ItemTransformVec3f.Deserializer())
            .registerTypeAdapter(ItemCameraTransforms.class, new ItemCameraTransforms.Deserializer())
            .create();
    private final List<BlockPart> elements;
    private final boolean gui3d;
    private final boolean ambientOcclusion;
    private final ItemCameraTransforms cameraTransforms;
    public String name;
    protected final Map<String, String> textures;
    protected ModelBlock parent;
    protected ResourceLocation parentLocation;

    public static ModelBlock deserialize(Reader readerIn) {
        return SERIALIZER.fromJson(readerIn, ModelBlock.class);
    }

    public static ModelBlock deserialize(String jsonString) {
        return deserialize(new StringReader(jsonString));
    }

    protected ModelBlock(List<BlockPart> elementsIn, Map<String, String> texturesIn, boolean ambientOcclusionIn, boolean gui3dIn, ItemCameraTransforms cameraTransformsIn) {
        this(null, elementsIn, texturesIn, ambientOcclusionIn, gui3dIn, cameraTransformsIn);
    }

    protected ModelBlock(ResourceLocation parentLocationIn, Map<String, String> texturesIn, boolean ambientOcclusionIn, boolean gui3dIn, ItemCameraTransforms cameraTransformsIn) {
        this(parentLocationIn, Collections.emptyList(), texturesIn, ambientOcclusionIn, gui3dIn, cameraTransformsIn);
    }

    private ModelBlock(ResourceLocation parentLocationIn, List<BlockPart> elementsIn, Map<String, String> texturesIn, boolean ambientOcclusionIn, boolean gui3dIn, ItemCameraTransforms cameraTransformsIn) {
        this.name = "";
        this.elements = elementsIn;
        this.ambientOcclusion = ambientOcclusionIn;
        this.gui3d = gui3dIn;
        this.textures = texturesIn;
        this.parentLocation = parentLocationIn;
        this.cameraTransforms = cameraTransformsIn;
    }

    public List<BlockPart> getElements() {
        return this.hasParent() ? this.parent.getElements() : this.elements;
    }

    private boolean hasParent() {
        return this.parent != null;
    }

    public boolean isAmbientOcclusion() {
        return this.hasParent() ? this.parent.isAmbientOcclusion() : this.ambientOcclusion;
    }

    public boolean isGui3d() {
        return this.gui3d;
    }

    public boolean isResolved() {
        return this.parentLocation == null || this.parent != null && this.parent.isResolved();
    }

    public void getParentFromMap(Map<ResourceLocation, ModelBlock> p_178299_1_) {
        if (this.parentLocation != null) {
            this.parent = p_178299_1_.get(this.parentLocation);
        }
    }

    public boolean isTexturePresent(String textureName) {
        return !"missingno".equals(this.resolveTextureName(textureName));
    }

    public String resolveTextureName(String textureName) {
        if (!this.startsWithHash(textureName)) {
            textureName = '#' + textureName;
        }

        return this.resolveTextureName(textureName, new Bookkeep(this));
    }

    private String resolveTextureName(String textureName, Bookkeep p_178302_2_) {
        if (this.startsWithHash(textureName)) {
            if (this == p_178302_2_.modelExt) {
                LOGGER.warn("Unable to resolve texture due to upward reference: {} in {}", textureName, this.name);
                return "missingno";
            } else {
                String s = this.textures.get(textureName.substring(1));

                if (s == null && this.hasParent()) {
                    s = this.parent.resolveTextureName(textureName, p_178302_2_);
                }

                p_178302_2_.modelExt = this;

                if (s != null && this.startsWithHash(s)) {
                    s = p_178302_2_.model.resolveTextureName(s, p_178302_2_);
                }

                return s != null && !this.startsWithHash(s) ? s : "missingno";
            }
        } else {
            return textureName;
        }
    }

    private boolean startsWithHash(String hash) {
        return hash.charAt(0) == 35;
    }

    public ResourceLocation getParentLocation() {
        return this.parentLocation;
    }

    public void setParentLocation(ResourceLocation parentLocation) {
        this.parentLocation = parentLocation;
    }

    public ModelBlock getRootModel() {
        return this.hasParent() ? this.parent.getRootModel() : this;
    }

    public ItemCameraTransforms getAllTransforms() {
        ItemTransformVec3f itemtransformvec3f = this.getTransform(ItemCameraTransforms.TransformType.THIRD_PERSON);
        ItemTransformVec3f itemtransformvec3f1 = this.getTransform(ItemCameraTransforms.TransformType.FIRST_PERSON);
        ItemTransformVec3f itemtransformvec3f2 = this.getTransform(ItemCameraTransforms.TransformType.HEAD);
        ItemTransformVec3f itemtransformvec3f3 = this.getTransform(ItemCameraTransforms.TransformType.GUI);
        ItemTransformVec3f itemtransformvec3f4 = this.getTransform(ItemCameraTransforms.TransformType.GROUND);
        ItemTransformVec3f itemtransformvec3f5 = this.getTransform(ItemCameraTransforms.TransformType.FIXED);
        return new ItemCameraTransforms(itemtransformvec3f, itemtransformvec3f1, itemtransformvec3f2, itemtransformvec3f3, itemtransformvec3f4, itemtransformvec3f5);
    }

    private ItemTransformVec3f getTransform(ItemCameraTransforms.TransformType type) {
        return this.parent != null && !this.cameraTransforms.func_181687_c(type) ? this.parent.getTransform(type) : this.cameraTransforms.getTransform(type);
    }

    public Map<String, String> getTextures() {
        return textures;
    }

    public static void checkModelHierarchy(Map<ResourceLocation, ModelBlock> p_178312_0_) {
        for (ModelBlock modelblock : p_178312_0_.values()) {
            try {
                ModelBlock modelblock1 = modelblock.parent;

                for (ModelBlock modelblock2 = modelblock1.parent; modelblock1 != modelblock2; modelblock2 = modelblock2.parent.parent) {
                    modelblock1 = modelblock1.parent;
                }

                throw new Deserializer.LoopException();
            } catch (NullPointerException exception) {
            }
        }
    }

    static final class Bookkeep {
        public final ModelBlock model;
        public ModelBlock modelExt;

        private Bookkeep(ModelBlock p_i46223_1_) {
            this.model = p_i46223_1_;
        }
    }

    public static class Deserializer implements JsonDeserializer<ModelBlock> {
        @Override
        public ModelBlock deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject jsonobject = jsonElement.getAsJsonObject();
            List<BlockPart> list = this.getModelElements(ctx, jsonobject);
            String s = this.getParent(jsonobject);
            boolean flag = StringUtils.isEmpty(s);
            boolean flag1 = list.isEmpty();

            if (flag1 && flag) {
                throw new JsonParseException("BlockModel requires either elements or parent, found neither");
            } else if (!flag && !flag1) {
                throw new JsonParseException("BlockModel requires either elements or parent, found both");
            } else {
                Map<String, String> map = this.getTextures(jsonobject);
                boolean flag2 = this.getAmbientOcclusionEnabled(jsonobject);
                ItemCameraTransforms itemcameratransforms = ItemCameraTransforms.DEFAULT;

                if (jsonobject.has("display")) {
                    JsonObject jsonobject1 = JsonUtils.getJsonObject(jsonobject, "display");
                    itemcameratransforms = ctx.deserialize(jsonobject1, ItemCameraTransforms.class);
                }

                return flag1 ? new ModelBlock(new ResourceLocation(s), map, flag2, true, itemcameratransforms) : new ModelBlock(list, map, flag2, true, itemcameratransforms);
            }
        }

        private Map<String, String> getTextures(JsonObject jsonObject) {
            Map<String, String> map = new HashMap<>();

            if (jsonObject.has("textures")) {
                JsonObject jsonobject = jsonObject.getAsJsonObject("textures");

                for (Entry<String, JsonElement> entry : jsonobject.entrySet()) {
                    map.put(entry.getKey(), entry.getValue().getAsString());
                }
            }

            return map;
        }

        private String getParent(JsonObject jsonObject) {
            return JsonUtils.getString(jsonObject, "parent", "");
        }

        protected boolean getAmbientOcclusionEnabled(JsonObject jsonObject) {
            return JsonUtils.getBoolean(jsonObject, "ambientocclusion", true);
        }

        protected List<BlockPart> getModelElements(JsonDeserializationContext ctx, JsonObject jsonObject) {
            List<BlockPart> list = new ArrayList<>();

            if (jsonObject.has("elements")) {
                for (JsonElement jsonelement : JsonUtils.getJsonArray(jsonObject, "elements")) {
                    list.add(ctx.deserialize(jsonelement, BlockPart.class));
                }
            }

            return list;
        }

        public static class LoopException extends RuntimeException {
        }
    }
}
