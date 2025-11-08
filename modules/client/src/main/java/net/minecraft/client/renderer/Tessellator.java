package net.minecraft.client.renderer;

import net.optifine.SmartAnimations;

public class Tessellator {
    private final WorldRenderer worldRenderer;
    private final WorldVertexBufferUploader vboUploader = new WorldVertexBufferUploader();
    private static final Tessellator INSTANCE = new Tessellator(2097152);

    public static Tessellator get() {
        return INSTANCE;
    }

    public Tessellator(int bufferSize) {
        this.worldRenderer = new WorldRenderer(bufferSize);
    }

    public void draw() {
        if (this.worldRenderer.animatedSprites != null) {
            SmartAnimations.spritesRendered(this.worldRenderer.animatedSprites);
        }

        this.worldRenderer.finishDrawing();
        this.vboUploader.draw(this.worldRenderer);
    }

    public WorldRenderer getWorldRenderer() {
        return this.worldRenderer;
    }
}
