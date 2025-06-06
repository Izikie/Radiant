package net.minecraft.world.gen.layer;

public class GenLayerAddSnow extends GenLayer {
    public GenLayerAddSnow(long p_i2121_1_, GenLayer p_i2121_3_) {
        super(p_i2121_1_);
        this.parent = p_i2121_3_;
    }

    public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
        int i = areaX - 1;
        int j = areaY - 1;
        int k = areaWidth + 2;
        int l = areaHeight + 2;
        int[] aint = this.parent.getInts(i, j, k, l);
        int[] aint1 = IntCache.getIntCache(areaWidth * areaHeight);

        for (int i1 = 0; i1 < areaHeight; ++i1) {
            for (int j1 = 0; j1 < areaWidth; ++j1) {
                int k1 = aint[j1 + 1 + (i1 + 1) * k];
                this.initChunkSeed((j1 + areaX), (i1 + areaY));

                if (k1 == 0) {
                    aint1[j1 + i1 * areaWidth] = 0;
                } else {
                    int l1 = this.nextInt(6);

                    if (l1 == 0) {
                        l1 = 4;
                    } else if (l1 <= 1) {
                        l1 = 3;
                    } else {
                        l1 = 1;
                    }

                    aint1[j1 + i1 * areaWidth] = l1;
                }
            }
        }

        return aint1;
    }
}
