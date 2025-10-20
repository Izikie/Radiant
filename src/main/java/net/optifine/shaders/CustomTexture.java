package net.optifine.shaders;

import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureUtil;

public record CustomTexture(int textureUnit, String path, ITextureObject texture) implements ICustomTexture {

	@Override
    public int getTextureId() {
		return this.texture.getGlTextureId();
	}

	@Override
    public void deleteTexture() {
		TextureUtil.deleteTexture(this.texture.getGlTextureId());
	}

	@Override
    public int getTarget() {
		return 3553;
	}

	public String toString() {
		return "textureUnit: " + this.textureUnit + ", path: " + this.path + ", glTextureId: " + this.getTextureId();
	}
}
