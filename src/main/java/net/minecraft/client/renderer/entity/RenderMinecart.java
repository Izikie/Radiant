package net.minecraft.client.renderer.entity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelMinecart;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3;

public class RenderMinecart<T extends EntityMinecart> extends Render<T> {
    private static final ResourceLocation MINECART_TEXTURES = new ResourceLocation("textures/entity/minecart.png");
    protected final ModelBase modelMinecart = new ModelMinecart();

    public RenderMinecart(RenderManager renderManagerIn) {
        super(renderManagerIn);
        this.shadowSize = 0.5F;
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        this.bindEntityTexture(entity);
        long i = entity.getEntityId() * 493286711L;
        i = i * i * 4392167121L + i * 98761L;
        float f = (((i >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float f1 = (((i >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float f2 = (((i >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        GlStateManager.translate(f, f1, f2);
        double xPos = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double yPos = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double zPos = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        double d3 = 0.30000001192092896D;
        Vec3 vec3 = entity.func_70489_a(xPos, yPos, zPos);
        float f3 = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;

        if (vec3 != null) {
            Vec3 vec31 = entity.func_70495_a(xPos, yPos, zPos, d3);
            Vec3 vec32 = entity.func_70495_a(xPos, yPos, zPos, -d3);

            if (vec31 == null) {
                vec31 = vec3;
            }

            if (vec32 == null) {
                vec32 = vec3;
            }

            x += vec3.xCoord - xPos;
            y += (vec31.yCoord + vec32.yCoord) / 2.0D - yPos;
            z += vec3.zCoord - zPos;
            Vec3 vec33 = vec32.addVector(-vec31.xCoord, -vec31.yCoord, -vec31.zCoord);

            if (vec33.lengthVector() != 0.0D) {
                vec33 = vec33.normalize();
                entityYaw = (float) (Math.atan2(vec33.zCoord, vec33.xCoord) * 180.0D / Math.PI);
                f3 = (float) (Math.atan(vec33.yCoord) * 73.0D);
            }
        }

        GlStateManager.translate((float) x, (float) y + 0.375F, (float) z);
        GlStateManager.rotate(180.0F - entityYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-f3, 0.0F, 0.0F, 1.0F);
        float f5 = entity.getRollingAmplitude() - partialTicks;
        float f6 = entity.getDamage() - partialTicks;

        if (f6 < 0.0F) {
            f6 = 0.0F;
        }

        if (f5 > 0.0F) {
            GlStateManager.rotate(MathHelper.sin(f5) * f5 * f6 / 10.0F * entity.getRollingDirection(), 1.0F, 0.0F, 0.0F);
        }

        int j = entity.getDisplayTileOffset();
        IBlockState iblockstate = entity.getDisplayTile();

        if (iblockstate.getBlock().getRenderType() != -1) {
            GlStateManager.pushMatrix();
            this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            float f4 = 0.75F;
            GlStateManager.scale(f4, f4, f4);
            GlStateManager.translate(-0.5F, (j - 8) / 16.0F, 0.5F);
            this.func_180560_a(entity, partialTicks, iblockstate);
            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.bindEntityTexture(entity);
        }

        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        this.modelMinecart.render(entity, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return MINECART_TEXTURES;
    }

    protected void func_180560_a(T minecart, float partialTicks, IBlockState state) {
        GlStateManager.pushMatrix();
        Minecraft.get().getBlockRendererDispatcher().renderBlockBrightness(state, minecart.getBrightness(partialTicks));
        GlStateManager.popMatrix();
    }
}
