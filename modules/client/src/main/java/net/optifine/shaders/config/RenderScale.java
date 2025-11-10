package net.optifine.shaders.config;

public record RenderScale(float scale, float offsetX, float offsetY) {
    public String toString() {
        return this.scale + ", " + this.offsetX + ", " + this.offsetY;
    }
}
