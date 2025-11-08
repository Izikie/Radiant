package net.minecraft.world;

public enum LightType {
    SKY(15),
    BLOCK(0);

    public final int defaultLightValue;

    LightType(int p_i1961_3_) {
        this.defaultLightValue = p_i1961_3_;
    }
}
