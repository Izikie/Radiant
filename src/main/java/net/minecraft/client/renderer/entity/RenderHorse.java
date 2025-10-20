package net.minecraft.client.renderer.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelHorse;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.LayeredTexture;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class RenderHorse extends RenderLiving<EntityHorse> {
    private static final Map<String, ResourceLocation> field_110852_a = new HashMap<>();
    private static final ResourceLocation WHITE_HORSE_TEXTURES = new ResourceLocation("textures/entity/horse/horse_white.png");
    private static final ResourceLocation MULE_TEXTURES = new ResourceLocation("textures/entity/horse/mule.png");
    private static final ResourceLocation DONKEY_TEXTURES = new ResourceLocation("textures/entity/horse/donkey.png");
    private static final ResourceLocation ZOMBIE_HORSE_TEXTURES = new ResourceLocation("textures/entity/horse/horse_zombie.png");
    private static final ResourceLocation SKELETON_HORSE_TEXTURES = new ResourceLocation("textures/entity/horse/horse_skeleton.png");

    public RenderHorse(RenderManager rendermanagerIn, ModelHorse model, float shadowSizeIn) {
        super(rendermanagerIn, model, shadowSizeIn);
    }

    @Override
    protected void preRenderCallback(EntityHorse entitylivingbaseIn, float partialTickTime) {
        float f = 1.0F;
        int i = entitylivingbaseIn.getHorseType();

        if (i == 1) {
            f *= 0.87F;
        } else if (i == 2) {
            f *= 0.92F;
        }

        GlStateManager.scale(f, f, f);
        super.preRenderCallback(entitylivingbaseIn, partialTickTime);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityHorse entity) {
        if (!entity.func_110239_cn()) {
            return switch (entity.getHorseType()) {
                case 1 -> DONKEY_TEXTURES;
                case 2 -> MULE_TEXTURES;
                case 3 -> ZOMBIE_HORSE_TEXTURES;
                case 4 -> SKELETON_HORSE_TEXTURES;
                default -> WHITE_HORSE_TEXTURES;
            };
        } else {
            return this.func_110848_b(entity);
        }
    }

    private ResourceLocation func_110848_b(EntityHorse horse) {
        String s = horse.getHorseTexture();

        if (!horse.func_175507_cI()) {
            return null;
        } else {
            ResourceLocation resourcelocation = field_110852_a.get(s);

            if (resourcelocation == null) {
                resourcelocation = new ResourceLocation(s);
                Minecraft.get().getTextureManager().loadTexture(resourcelocation, new LayeredTexture(horse.getVariantTexturePaths()));
                field_110852_a.put(s, resourcelocation);
            }

            return resourcelocation;
        }
    }
}
