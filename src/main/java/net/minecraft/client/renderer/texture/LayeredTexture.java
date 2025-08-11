package net.minecraft.client.renderer.texture;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.src.Config;
import net.minecraft.util.ResourceLocation;
import net.optifine.shaders.ShadersTex;
import net.radiant.util.NativeImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class LayeredTexture extends AbstractTexture {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayeredTexture.class);
    public final List<String> layeredTextureNames;
    private ResourceLocation textureLocation;

    public LayeredTexture(String... textureNames) {
        this.layeredTextureNames = Arrays.asList(textureNames);

        if (textureNames.length > 0 && textureNames[0] != null) {
            this.textureLocation = new ResourceLocation(textureNames[0]);
        }
    }

    public void loadTexture(IResourceManager resourceManager) throws IOException {
        this.deleteGlTexture();
        NativeImage bufferedimage = null;

        try {
            for (String s : this.layeredTextureNames) {
                if (s != null) {
                    InputStream inputstream = resourceManager.getResource(new ResourceLocation(s)).getInputStream();
                    NativeImage bufferedimage1 = TextureUtil.readNativeImage(inputstream);

                    if (bufferedimage == null) {
                        bufferedimage = NativeImage.createBlankImage(bufferedimage1.getWidth(), bufferedimage1.getHeight());
                    }

                    bufferedimage.copyFrom(bufferedimage1);
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Couldn't load layered image", exception);
            return;
        }

        if (Config.isShaders()) {
            ShadersTex.loadSimpleTexture(this.getGlTextureId(), bufferedimage, false, false, resourceManager, this.textureLocation, this.getMultiTexID());
        } else {
            TextureUtil.uploadTextureImage(this.getGlTextureId(), bufferedimage);
        }
    }
}
