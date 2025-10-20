package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BreakingFour;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;

import java.util.ArrayList;
import java.util.List;

public class SimpleBakedModel implements IBakedModel {
    protected final List<BakedQuad> generalQuads;
    protected final List<List<BakedQuad>> faceQuads;
    protected final boolean ambientOcclusion;
    protected final boolean gui3d;
    protected final TextureAtlasSprite texture;
    protected final ItemCameraTransforms cameraTransforms;

    public SimpleBakedModel(List<BakedQuad> generalQuadsIn, List<List<BakedQuad>> faceQuadsIn, boolean ambientOcclusionIn, boolean gui3dIn, TextureAtlasSprite textureIn, ItemCameraTransforms cameraTransformsIn) {
        this.generalQuads = generalQuadsIn;
        this.faceQuads = faceQuadsIn;
        this.ambientOcclusion = ambientOcclusionIn;
        this.gui3d = gui3dIn;
        this.texture = textureIn;
        this.cameraTransforms = cameraTransformsIn;
    }

    @Override
    public List<BakedQuad> getFaceQuads(Direction facing) {
        return this.faceQuads.get(facing.ordinal());
    }

    @Override
    public List<BakedQuad> getGeneralQuads() {
        return this.generalQuads;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return this.ambientOcclusion;
    }

    @Override
    public boolean isGui3d() {
        return this.gui3d;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.texture;
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return this.cameraTransforms;
    }

    public static class Builder {
        private final List<BakedQuad> builderGeneralQuads;
        private final List<List<BakedQuad>> builderFaceQuads;
        private final boolean builderAmbientOcclusion;
        private TextureAtlasSprite builderTexture;
        private final boolean builderGui3d;
        private final ItemCameraTransforms builderCameraTransforms;

        public Builder(ModelBlock model) {
            this(model.isAmbientOcclusion(), model.isGui3d(), model.getAllTransforms());
        }

        public Builder(IBakedModel bakedModel, TextureAtlasSprite texture) {
            this(bakedModel.isAmbientOcclusion(), bakedModel.isGui3d(), bakedModel.getItemCameraTransforms());
            this.builderTexture = bakedModel.getParticleTexture();

            for (Direction enumfacing : Direction.values()) {
                this.addFaceBreakingFours(bakedModel, texture, enumfacing);
            }

            this.addGeneralBreakingFours(bakedModel, texture);
        }

        private void addFaceBreakingFours(IBakedModel bakedModel, TextureAtlasSprite texture, Direction facing) {
            for (BakedQuad bakedquad : bakedModel.getFaceQuads(facing)) {
                this.addFaceQuad(facing, new BreakingFour(bakedquad, texture));
            }
        }

        private void addGeneralBreakingFours(IBakedModel p_177647_1_, TextureAtlasSprite texture) {
            for (BakedQuad bakedquad : p_177647_1_.getGeneralQuads()) {
                this.addGeneralQuad(new BreakingFour(bakedquad, texture));
            }
        }

        private Builder(boolean ambientOcclusion, boolean gui3d, ItemCameraTransforms cameraTransforms) {
            this.builderGeneralQuads = new ArrayList<>();
            this.builderFaceQuads = new ArrayList<>(6);

            for (Direction enumfacing : Direction.values()) {
                this.builderFaceQuads.add(new ArrayList<>());
            }

            this.builderAmbientOcclusion = ambientOcclusion;
            this.builderGui3d = gui3d;
            this.builderCameraTransforms = cameraTransforms;
        }

        public Builder addFaceQuad(Direction facing, BakedQuad quad) {
            this.builderFaceQuads.get(facing.ordinal()).add(quad);
            return this;
        }

        public Builder addGeneralQuad(BakedQuad quad) {
            this.builderGeneralQuads.add(quad);
            return this;
        }

        public Builder setTexture(TextureAtlasSprite texture) {
            this.builderTexture = texture;
            return this;
        }

        public IBakedModel makeBakedModel() {
            if (this.builderTexture == null) {
                throw new RuntimeException("Missing particle!");
            } else {
                return new SimpleBakedModel(this.builderGeneralQuads, this.builderFaceQuads, this.builderAmbientOcclusion, this.builderGui3d, this.builderTexture, this.builderCameraTransforms);
            }
        }
    }
}
