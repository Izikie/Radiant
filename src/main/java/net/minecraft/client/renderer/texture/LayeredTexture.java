package net.minecraft.client.renderer.texture;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.src.Config;
import net.minecraft.util.ResourceLocation;
import net.optifine.shaders.ShadersTex;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.awt.image.BufferedImage;
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
        BufferedImage bufferedimage = null;

        try {
            for (String s : this.layeredTextureNames) {
                if (s != null) {
                    InputStream inputstream = resourceManager.getResource(new ResourceLocation(s)).getInputStream();
                    BufferedImage bufferedimage1 = TextureUtil.readBufferedImage(inputstream);

                    if (bufferedimage == null) {
                        bufferedimage = new BufferedImage(bufferedimage1.getWidth(), bufferedimage1.getHeight(), 2);
                    }

                    bufferedimage.getGraphics().drawImage(bufferedimage1, 0, 0, null);
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
