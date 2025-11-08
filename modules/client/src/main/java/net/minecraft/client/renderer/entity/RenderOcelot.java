package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.util.ResourceLocation;

public class RenderOcelot extends RenderLiving<EntityOcelot> {
    private static final ResourceLocation BLACK_OCELOT_TEXTURES = new ResourceLocation("textures/entity/cat/black.png");
    private static final ResourceLocation OCELOT_TEXTURES = new ResourceLocation("textures/entity/cat/ocelot.png");
    private static final ResourceLocation RED_OCELOT_TEXTURES = new ResourceLocation("textures/entity/cat/red.png");
    private static final ResourceLocation SIAMESE_OCELOT_TEXTURES = new ResourceLocation("textures/entity/cat/siamese.png");

    public RenderOcelot(RenderManager renderManagerIn, ModelBase modelBaseIn, float shadowSizeIn) {
        super(renderManagerIn, modelBaseIn, shadowSizeIn);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityOcelot entity) {
        return switch (entity.getTameSkin()) {
            case 1 -> BLACK_OCELOT_TEXTURES;
            case 2 -> RED_OCELOT_TEXTURES;
            case 3 -> SIAMESE_OCELOT_TEXTURES;
            default -> OCELOT_TEXTURES;
        };
    }

    @Override
    protected void preRenderCallback(EntityOcelot entitylivingbaseIn, float partialTickTime) {
        super.preRenderCallback(entitylivingbaseIn, partialTickTime);

        if (entitylivingbaseIn.isTamed()) {
            GlStateManager.scale(0.8F, 0.8F, 0.8F);
        }
    }
}
