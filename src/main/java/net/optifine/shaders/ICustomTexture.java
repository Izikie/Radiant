package net.optifine.shaders;

public interface ICustomTexture {
	int getTextureId();

	int textureUnit();

	void deleteTexture();

	int getTarget();
}
