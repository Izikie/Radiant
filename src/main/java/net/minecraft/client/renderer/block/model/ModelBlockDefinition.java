package net.minecraft.client.renderer.block.model;

import com.google.gson.*;
import net.minecraft.client.resources.model.ModelRotation;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;

public class ModelBlockDefinition {
    static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ModelBlockDefinition.class, new Deserializer())
            .registerTypeAdapter(Variant.class, new Variant.Deserializer())
            .create();
    private final Map<String, Variants> mapVariants = new HashMap<>();

    public static ModelBlockDefinition parseFromReader(Reader p_178331_0_) {
        return GSON.fromJson(p_178331_0_, ModelBlockDefinition.class);
    }

    public ModelBlockDefinition(Collection<Variants> p_i46221_1_) {
        for (Variants modelblockdefinition$variants : p_i46221_1_) {
            this.mapVariants.put(modelblockdefinition$variants.name, modelblockdefinition$variants);
        }
    }

    public ModelBlockDefinition(List<ModelBlockDefinition> p_i46222_1_) {
        for (ModelBlockDefinition modelblockdefinition : p_i46222_1_) {
            this.mapVariants.putAll(modelblockdefinition.mapVariants);
        }
    }

    public Variants getVariants(String p_178330_1_) {
        Variants modelblockdefinition$variants = this.mapVariants.get(p_178330_1_);

        if (modelblockdefinition$variants == null) {
            throw new MissingVariantException();
        } else {
            return modelblockdefinition$variants;
        }
    }

    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (p_equals_1_ instanceof ModelBlockDefinition modelblockdefinition) {
            return this.mapVariants.equals(modelblockdefinition.mapVariants);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.mapVariants.hashCode();
    }

    public static class Deserializer implements JsonDeserializer<ModelBlockDefinition> {
        public ModelBlockDefinition deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject jsonobject = jsonElement.getAsJsonObject();
            List<Variants> list = this.parseVariantsList(ctx, jsonobject);
            return new ModelBlockDefinition(list);
        }

        protected List<Variants> parseVariantsList(JsonDeserializationContext ctx, JsonObject jsonObject) {
            JsonObject jsonobject = JsonUtils.getJsonObject(jsonObject, "variants");
            List<Variants> list = new ArrayList<>();

            for (Entry<String, JsonElement> entry : jsonobject.entrySet()) {
                list.add(this.parseVariants(ctx, entry));
            }

            return list;
        }

        protected Variants parseVariants(JsonDeserializationContext ctx, Entry<String, JsonElement> p_178335_2_) {
            String s = p_178335_2_.getKey();
            List<Variant> list = new ArrayList<>();
            JsonElement jsonelement = p_178335_2_.getValue();

            if (jsonelement.isJsonArray()) {
                for (JsonElement jsonelement1 : jsonelement.getAsJsonArray()) {
                    list.add(ctx.deserialize(jsonelement1, Variant.class));
                }
            } else {
                list.add(ctx.deserialize(jsonelement, Variant.class));
            }

            return new Variants(s, list);
        }
    }

    public class MissingVariantException extends RuntimeException {
    }

    public static class Variant {
        private final ResourceLocation modelLocation;
        private final ModelRotation modelRotation;
        private final boolean uvLock;
        private final int weight;

        public Variant(ResourceLocation modelLocationIn, ModelRotation modelRotationIn, boolean uvLockIn, int weightIn) {
            this.modelLocation = modelLocationIn;
            this.modelRotation = modelRotationIn;
            this.uvLock = uvLockIn;
            this.weight = weightIn;
        }

        public ResourceLocation getModelLocation() {
            return this.modelLocation;
        }

        public ModelRotation getRotation() {
            return this.modelRotation;
        }

        public boolean isUvLocked() {
            return this.uvLock;
        }

        public int getWeight() {
            return this.weight;
        }

        public boolean equals(Object p_equals_1_) {
            if (this == p_equals_1_) {
                return true;
            } else if (!(p_equals_1_ instanceof Variant modelblockdefinition$variant)) {
                return false;
            } else {
                return this.modelLocation.equals(modelblockdefinition$variant.modelLocation) && this.modelRotation == modelblockdefinition$variant.modelRotation && this.uvLock == modelblockdefinition$variant.uvLock;
            }
        }

        public int hashCode() {
            int i = this.modelLocation.hashCode();
            i = 31 * i + (this.modelRotation != null ? this.modelRotation.hashCode() : 0);
            i = 31 * i + (this.uvLock ? 1 : 0);
            return i;
        }

        public static class Deserializer implements JsonDeserializer<Variant> {
            public Variant deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
                JsonObject jsonobject = jsonElement.getAsJsonObject();
                String s = this.parseModel(jsonobject);
                ModelRotation modelrotation = this.parseRotation(jsonobject);
                boolean flag = this.parseUvLock(jsonobject);
                int i = this.parseWeight(jsonobject);
                return new Variant(this.makeModelLocation(s), modelrotation, flag, i);
            }

            private ResourceLocation makeModelLocation(String p_178426_1_) {
                ResourceLocation resourcelocation = new ResourceLocation(p_178426_1_);
                resourcelocation = new ResourceLocation(resourcelocation.getResourceDomain(), "block/" + resourcelocation.getResourcePath());
                return resourcelocation;
            }

            private boolean parseUvLock(JsonObject jsonObject) {
                return JsonUtils.getBoolean(jsonObject, "uvlock", false);
            }

            protected ModelRotation parseRotation(JsonObject jsonObject) {
                int i = JsonUtils.getInt(jsonObject, "x", 0);
                int j = JsonUtils.getInt(jsonObject, "y", 0);
                ModelRotation modelrotation = ModelRotation.getModelRotation(i, j);

                if (modelrotation == null) {
                    throw new JsonParseException("Invalid BlockModelRotation x: " + i + ", y: " + j);
                } else {
                    return modelrotation;
                }
            }

            protected String parseModel(JsonObject jsonObject) {
                return JsonUtils.getString(jsonObject, "model");
            }

            protected int parseWeight(JsonObject jsonObject) {
                return JsonUtils.getInt(jsonObject, "weight", 1);
            }
        }
    }

    public static class Variants {
        private final String name;
        private final List<Variant> listVariants;

        public Variants(String nameIn, List<Variant> listVariantsIn) {
            this.name = nameIn;
            this.listVariants = listVariantsIn;
        }

        public List<Variant> getVariants() {
            return this.listVariants;
        }

        public boolean equals(Object p_equals_1_) {
            if (this == p_equals_1_) {
                return true;
            } else if (!(p_equals_1_ instanceof Variants modelblockdefinition$variants)) {
                return false;
            } else {
                return this.name.equals(modelblockdefinition$variants.name) && this.listVariants.equals(modelblockdefinition$variants.listVariants);
            }
        }

        public int hashCode() {
            int i = this.name.hashCode();
            i = 31 * i + this.listVariants.hashCode();
            return i;
        }
    }
}
