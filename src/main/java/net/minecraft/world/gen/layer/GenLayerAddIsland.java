package net.minecraft.world.gen.layer;

public class GenLayerAddIsland extends GenLayer {
    public GenLayerAddIsland(long p_i2119_1_, GenLayer p_i2119_3_) {
        super(p_i2119_1_);
        this.parent = p_i2119_3_;
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
                int k1 = aint[j1 + (i1) * k];
                int l1 = aint[j1 + 2 + (i1) * k];
                int i2 = aint[j1 + (i1 + 2) * k];
                int j2 = aint[j1 + 2 + (i1 + 2) * k];
                int k2 = aint[j1 + 1 + (i1 + 1) * k];
                this.initChunkSeed((j1 + areaX), (i1 + areaY));

                if (k2 != 0 || k1 == 0 && l1 == 0 && i2 == 0 && j2 == 0) {
                    if (k2 > 0 && (k1 == 0 || l1 == 0 || i2 == 0 || j2 == 0)) {
                        if (this.nextInt(5) == 0) {
                            if (k2 == 4) {
                                aint1[j1 + i1 * areaWidth] = 4;
                            } else {
                                aint1[j1 + i1 * areaWidth] = 0;
                            }
                        } else {
                            aint1[j1 + i1 * areaWidth] = k2;
                        }
                    } else {
                        aint1[j1 + i1 * areaWidth] = k2;
                    }
                } else {
                    int l2 = 1;
                    int i3 = 1;

                    if (k1 != 0 && this.nextInt(l2++) == 0) {
                        i3 = k1;
                    }

                    if (l1 != 0 && this.nextInt(l2++) == 0) {
                        i3 = l1;
                    }

                    if (i2 != 0 && this.nextInt(l2++) == 0) {
                        i3 = i2;
                    }

                    if (j2 != 0 && this.nextInt(l2++) == 0) {
                        i3 = j2;
                    }

                    if (this.nextInt(3) == 0) {
                        aint1[j1 + i1 * areaWidth] = i3;
                    } else if (i3 == 4) {
                        aint1[j1 + i1 * areaWidth] = 4;
                    } else {
                        aint1[j1 + i1 * areaWidth] = 0;
                    }
                }
            }
        }

        return aint1;
    }
}
