package net.minecraft.client.renderer.texture;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.TextureMetadataSection;
import net.minecraft.util.ResourceLocation;
import net.optifine.Config;
import net.optifine.EmissiveTextures;
import net.optifine.shaders.ShadersTex;
import net.radiant.util.NativeImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class SimpleTexture extends AbstractTexture {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleTexture.class);
    protected final ResourceLocation textureLocation;
    public ResourceLocation locationEmissive;
    public boolean isEmissive;

    public SimpleTexture(ResourceLocation textureResourceLocation) {
        this.textureLocation = textureResourceLocation;
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        this.deleteGlTexture();
        IResource iresource = resourceManager.getResource(this.textureLocation);

        try (InputStream inputstream = iresource.getInputStream()) {
            NativeImage bufferedimage = NativeImage.loadFromInputStream(inputstream);
            TextureMetadataSection metadata = readTextureMetadata(iresource);
            boolean flag = metadata != null && metadata.textureBlur();
            boolean flag1 = metadata != null && metadata.textureClamp();

            if (Config.isShaders()) {
                ShadersTex.loadSimpleTexture(this.getGlTextureId(), bufferedimage, flag, flag1, resourceManager, this.textureLocation, this.getMultiTexID());
            } else {
                TextureUtil.uploadTextureImageAllocate(this.getGlTextureId(), bufferedimage, flag, flag1);
            }

            if (EmissiveTextures.isActive()) {
                EmissiveTextures.loadTexture(this.textureLocation, this);
            }
        }
    }

    public static TextureMetadataSection readTextureMetadata(IResource resource) {
        if (resource.hasMetadata()) {
            try {
                return resource.getMetadata("texture");
            } catch (RuntimeException exception) {
                LOGGER.warn("Failed reading metadata of: {}", resource.getResourceLocation(), exception);
            }
        }
        return null;
    }
}
