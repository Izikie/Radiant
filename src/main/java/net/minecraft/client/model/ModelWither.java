package net.minecraft.client.model;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.util.math.MathHelper;

public class ModelWither extends ModelBase {
    private final ModelRenderer[] bodyParts;
    private final ModelRenderer[] heads;

    public ModelWither(float p_i46302_1_) {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.bodyParts = new ModelRenderer[3];
        this.bodyParts[0] = new ModelRenderer(this, 0, 16);
        this.bodyParts[0].addBox(-10.0F, 3.9F, -0.5F, 20, 3, 3, p_i46302_1_);
        this.bodyParts[1] = (new ModelRenderer(this)).setTextureSize(this.textureWidth, this.textureHeight);
        this.bodyParts[1].setRotationPoint(-2.0F, 6.9F, -0.5F);
        this.bodyParts[1].setTextureOffset(0, 22).addBox(0.0F, 0.0F, 0.0F, 3, 10, 3, p_i46302_1_);
        this.bodyParts[1].setTextureOffset(24, 22).addBox(-4.0F, 1.5F, 0.5F, 11, 2, 2, p_i46302_1_);
        this.bodyParts[1].setTextureOffset(24, 22).addBox(-4.0F, 4.0F, 0.5F, 11, 2, 2, p_i46302_1_);
        this.bodyParts[1].setTextureOffset(24, 22).addBox(-4.0F, 6.5F, 0.5F, 11, 2, 2, p_i46302_1_);
        this.bodyParts[2] = new ModelRenderer(this, 12, 22);
        this.bodyParts[2].addBox(0.0F, 0.0F, 0.0F, 3, 6, 3, p_i46302_1_);
        this.heads = new ModelRenderer[3];
        this.heads[0] = new ModelRenderer(this, 0, 0);
        this.heads[0].addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8, p_i46302_1_);
        this.heads[1] = new ModelRenderer(this, 32, 0);
        this.heads[1].addBox(-4.0F, -4.0F, -4.0F, 6, 6, 6, p_i46302_1_);
        this.heads[1].rotationPointX = -8.0F;
        this.heads[1].rotationPointY = 4.0F;
        this.heads[2] = new ModelRenderer(this, 32, 0);
        this.heads[2].addBox(-4.0F, -4.0F, -4.0F, 6, 6, 6, p_i46302_1_);
        this.heads[2].rotationPointX = 10.0F;
        this.heads[2].rotationPointY = 4.0F;
    }

    public void render(Entity entityIn, float p_78088_2_, float p_78088_3_, float p_78088_4_, float p_78088_5_, float p_78088_6_, float scale) {
        this.setRotationAngles(p_78088_2_, p_78088_3_, p_78088_4_, p_78088_5_, p_78088_6_, scale, entityIn);

        for (ModelRenderer modelrenderer : this.heads) {
            modelrenderer.render(scale);
        }

        for (ModelRenderer modelrenderer1 : this.bodyParts) {
            modelrenderer1.render(scale);
        }
    }

    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        float f = MathHelper.cos(ageInTicks * 0.1F);
        this.bodyParts[1].rotateAngleX = (0.065F + 0.05F * f) * (float) Math.PI;
        this.bodyParts[2].setRotationPoint(-2.0F, 6.9F + MathHelper.cos(this.bodyParts[1].rotateAngleX) * 10.0F, -0.5F + MathHelper.sin(this.bodyParts[1].rotateAngleX) * 10.0F);
        this.bodyParts[2].rotateAngleX = (0.265F + 0.1F * f) * (float) Math.PI;
        this.heads[0].rotateAngleY = netHeadYaw / (180.0F / (float) Math.PI);
        this.heads[0].rotateAngleX = headPitch / (180.0F / (float) Math.PI);
    }

    public void setLivingAnimations(EntityLivingBase entitylivingbaseIn, float p_78086_2_, float p_78086_3_, float partialTickTime) {
        EntityWither entitywither = (EntityWither) entitylivingbaseIn;

        for (int i = 1; i < 3; ++i) {
            this.heads[i].rotateAngleY = (entitywither.func_82207_a(i - 1) - entitylivingbaseIn.renderYawOffset) / (180.0F / (float) Math.PI);
            this.heads[i].rotateAngleX = entitywither.func_82210_r(i - 1) / (180.0F / (float) Math.PI);
        }
    }

    public ModelRenderer[] getBodyParts() {
        return bodyParts;
    }

    public ModelRenderer[] getHeads() {
        return heads;
    }
}
