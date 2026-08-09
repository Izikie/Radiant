package net.minecraft.client.resources.data;

public record AnimationFrame(int frameIndex, int frameTime) {
    public AnimationFrame(int p_i1307_1_) {
        this(p_i1307_1_, -1);
    }

    public boolean hasNoTime() {
        return this.frameTime == -1;
    }
}
