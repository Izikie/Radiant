package net.optifine.texture;

import org.lwjgl.opengl.GL11;

public enum TextureType {
    TEXTURE_1D(3552),
    TEXTURE_2D(GL11.GL_TEXTURE_2D),
    TEXTURE_3D(32879),
    TEXTURE_RECTANGLE(34037);

    private final int id;

    TextureType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
