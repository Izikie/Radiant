package net.minecraft.client.renderer.block.model;

import com.google.gson.*;
import net.minecraft.client.renderer.GlStateManager;

import java.lang.reflect.Type;

public class ItemCameraTransforms {
    public static final ItemCameraTransforms DEFAULT = new ItemCameraTransforms();
    public static final float field_181690_b = 0.0F;
    public static final float field_181691_c = 0.0F;
    public static final float field_181692_d = 0.0F;
    public static final float field_181693_e = 0.0F;
    public static final float field_181694_f = 0.0F;
    public static final float field_181695_g = 0.0F;
    public static final float field_181696_h = 0.0F;
    public static final float field_181697_i = 0.0F;
    public static final float field_181698_j = 0.0F;
    public final ItemTransformVec3f thirdPerson;
    public final ItemTransformVec3f firstPerson;
    public final ItemTransformVec3f head;
    public final ItemTransformVec3f gui;
    public final ItemTransformVec3f ground;
    public final ItemTransformVec3f fixed;

    private ItemCameraTransforms() {
        this(ItemTransformVec3f.DEFAULT, ItemTransformVec3f.DEFAULT, ItemTransformVec3f.DEFAULT, ItemTransformVec3f.DEFAULT, ItemTransformVec3f.DEFAULT, ItemTransformVec3f.DEFAULT);
    }

    public ItemCameraTransforms(ItemCameraTransforms transforms) {
        this.thirdPerson = transforms.thirdPerson;
        this.firstPerson = transforms.firstPerson;
        this.head = transforms.head;
        this.gui = transforms.gui;
        this.ground = transforms.ground;
        this.fixed = transforms.fixed;
    }

    public ItemCameraTransforms(ItemTransformVec3f thirdPersonIn, ItemTransformVec3f firstPersonIn, ItemTransformVec3f headIn, ItemTransformVec3f guiIn, ItemTransformVec3f groundIn, ItemTransformVec3f fixedIn) {
        this.thirdPerson = thirdPersonIn;
        this.firstPerson = firstPersonIn;
        this.head = headIn;
        this.gui = guiIn;
        this.ground = groundIn;
        this.fixed = fixedIn;
    }

    public void applyTransform(TransformType type) {
        ItemTransformVec3f itemtransformvec3f = this.getTransform(type);

        if (itemtransformvec3f != ItemTransformVec3f.DEFAULT) {
            GlStateManager.translate(itemtransformvec3f.translation().x + field_181690_b, itemtransformvec3f.translation().y + field_181691_c, itemtransformvec3f.translation().z + field_181692_d);
            GlStateManager.rotate(itemtransformvec3f.rotation().y + field_181694_f, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(itemtransformvec3f.rotation().x + field_181693_e, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(itemtransformvec3f.rotation().z + field_181695_g, 0.0F, 0.0F, 1.0F);
            GlStateManager.scale(itemtransformvec3f.scale().x + field_181696_h, itemtransformvec3f.scale().y + field_181697_i, itemtransformvec3f.scale().z + field_181698_j);
        }
    }

    public ItemTransformVec3f getTransform(TransformType type) {
        return switch (type) {
            case THIRD_PERSON -> this.thirdPerson;
            case FIRST_PERSON -> this.firstPerson;
            case HEAD -> this.head;
            case GUI -> this.gui;
            case GROUND -> this.ground;
            case FIXED -> this.fixed;
            default -> ItemTransformVec3f.DEFAULT;
        };
    }

    public boolean func_181687_c(TransformType type) {
        return !this.getTransform(type).equals(ItemTransformVec3f.DEFAULT);
    }

    static class Deserializer implements JsonDeserializer<ItemCameraTransforms> {
        public ItemCameraTransforms deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject jsonobject = jsonElement.getAsJsonObject();
            ItemTransformVec3f itemtransformvec3f = this.func_181683_a(ctx, jsonobject, "thirdperson");
            ItemTransformVec3f itemtransformvec3f1 = this.func_181683_a(ctx, jsonobject, "firstperson");
            ItemTransformVec3f itemtransformvec3f2 = this.func_181683_a(ctx, jsonobject, "head");
            ItemTransformVec3f itemtransformvec3f3 = this.func_181683_a(ctx, jsonobject, "gui");
            ItemTransformVec3f itemtransformvec3f4 = this.func_181683_a(ctx, jsonobject, "ground");
            ItemTransformVec3f itemtransformvec3f5 = this.func_181683_a(ctx, jsonobject, "fixed");
            return new ItemCameraTransforms(itemtransformvec3f, itemtransformvec3f1, itemtransformvec3f2, itemtransformvec3f3, itemtransformvec3f4, itemtransformvec3f5);
        }

        private ItemTransformVec3f func_181683_a(JsonDeserializationContext ctx, JsonObject jsonObject, String p_181683_3_) {
            return jsonObject.has(p_181683_3_) ? (ItemTransformVec3f) ctx.deserialize(jsonObject.get(p_181683_3_), ItemTransformVec3f.class) : ItemTransformVec3f.DEFAULT;
        }
    }

    public enum TransformType {
        NONE,
        THIRD_PERSON,
        FIRST_PERSON,
        HEAD,
        GUI,
        GROUND,
        FIXED
    }
}
