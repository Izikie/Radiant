package net.minecraft.client.resources.model;

import com.google.common.collect.ComparisonChain;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.random.WeightedRandom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WeightedBakedModel implements IBakedModel {
    private final int totalWeight;
    private final List<MyWeighedRandomItem> models;
    private final IBakedModel baseModel;

    public WeightedBakedModel(List<MyWeighedRandomItem> p_i46073_1_) {
        this.models = p_i46073_1_;
        this.totalWeight = WeightedRandom.getTotalWeight(p_i46073_1_);
        this.baseModel = p_i46073_1_.getFirst().model;
    }

    @Override
    public List<BakedQuad> getFaceQuads(Direction facing) {
        return this.baseModel.getFaceQuads(facing);
    }

    @Override
    public List<BakedQuad> getGeneralQuads() {
        return this.baseModel.getGeneralQuads();
    }

    @Override
    public boolean isAmbientOcclusion() {
        return this.baseModel.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.baseModel.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return this.baseModel.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.baseModel.getParticleTexture();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return this.baseModel.getItemCameraTransforms();
    }

    public IBakedModel getAlternativeModel(long p_177564_1_) {
        return WeightedRandom.getRandomItem(this.models, Math.abs((int) p_177564_1_ >> 16) % this.totalWeight).model;
    }

    public static class Builder {
        private final List<MyWeighedRandomItem> listItems = new ArrayList<>();

        public Builder add(IBakedModel p_177677_1_, int p_177677_2_) {
            this.listItems.add(new MyWeighedRandomItem(p_177677_1_, p_177677_2_));
            return this;
        }

        public WeightedBakedModel build() {
            Collections.sort(this.listItems);
            return new WeightedBakedModel(this.listItems);
        }

        public IBakedModel first() {
            return this.listItems.getFirst().model;
        }
    }

    static class MyWeighedRandomItem extends WeightedRandom.Item implements Comparable<MyWeighedRandomItem> {
        protected final IBakedModel model;

        public MyWeighedRandomItem(IBakedModel p_i46072_1_, int p_i46072_2_) {
            super(p_i46072_2_);
            this.model = p_i46072_1_;
        }

        @Override
        public int compareTo(MyWeighedRandomItem p_compareTo_1_) {
            return ComparisonChain.start().compare(p_compareTo_1_.itemWeight, this.itemWeight).compare(this.getCountQuads(), p_compareTo_1_.getCountQuads()).result();
        }

        protected int getCountQuads() {
            int i = this.model.getGeneralQuads().size();

            for (Direction enumfacing : Direction.values()) {
                i += this.model.getFaceQuads(enumfacing).size();
            }

            return i;
        }

        public String toString() {
            return "MyWeighedRandomItem{weight=" + this.itemWeight + ", model=" + this.model + '}';
        }
    }
}
