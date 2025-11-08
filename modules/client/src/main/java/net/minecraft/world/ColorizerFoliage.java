package net.minecraft.world;

public class ColorizerFoliage {
    private static int[] foliageBuffer = new int[0x10000];

    public static void setFoliageBiomeColorizer(int[] p_77467_0_) {
        foliageBuffer = p_77467_0_;
    }

    public static int getFoliageColor(double p_77470_0_, double p_77470_2_) {
        p_77470_2_ = p_77470_2_ * p_77470_0_;
        int i = (int) ((1.0D - p_77470_0_) * 255.0D);
        int j = (int) ((1.0D - p_77470_2_) * 255.0D);
        return foliageBuffer[j << 8 | i];
    }

    public static int getFoliageColorPine() {
        return 0x619961;
    }

    public static int getFoliageColorBirch() {
        return 0x80a755;
    }

    public static int getFoliageColorBasic() {
        return 0x48b518;
    }
}
