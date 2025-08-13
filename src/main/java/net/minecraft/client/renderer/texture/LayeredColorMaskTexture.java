package net.minecraft.client.renderer.texture;

import net.minecraft.block.material.MapColor;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.DyeColor;
import net.optifine.Config;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.optifine.shaders.ShadersTex;
import net.radiant.util.NativeImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class LayeredColorMaskTexture extends AbstractTexture {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayeredColorMaskTexture.class);
    private final ResourceLocation textureLocation;
    private final List<String> field_174949_h;
    private final List<DyeColor> field_174950_i;

    public LayeredColorMaskTexture(ResourceLocation textureLocationIn, List<String> p_i46101_2_, List<DyeColor> p_i46101_3_) {
        this.textureLocation = textureLocationIn;
        this.field_174949_h = p_i46101_2_;
        this.field_174950_i = p_i46101_3_;
    }

    public void loadTexture(IResourceManager resourceManager) throws IOException {
        this.deleteGlTexture();
        NativeImage image;

        try {
            NativeImage loadedImage = TextureUtil.readNativeImage(resourceManager.getResource(this.textureLocation).getInputStream());

            image = NativeImage.createBlankImage(loadedImage.getWidth(), loadedImage.getHeight());
            image.copyFrom(loadedImage);

            for (int j = 0; j < 17 && j < this.field_174949_h.size() && j < this.field_174950_i.size(); ++j) {
                String s = this.field_174949_h.get(j);
                MapColor mapcolor = this.field_174950_i.get(j).getMapColor();

                if (s != null) {
                    InputStream inputstream = resourceManager.getResource(new ResourceLocation(s)).getInputStream();
                    NativeImage bruhImage = TextureUtil.readNativeImage(inputstream);

                    if (bruhImage.getWidth() == image.getWidth() && bruhImage.getHeight() == image.getHeight()) {
                        for (int y = 0; y < bruhImage.getHeight(); ++y) {
                            for (int x = 0; x < bruhImage.getWidth(); ++x) {
                                int i1 = bruhImage.getPixel(x, y);

                                if ((i1 & 0xff000000) != 0) {
                                    int j1 = (i1 & 0xff0000) << 8 & 0xff000000;
                                    int k1 = loadedImage.getPixel(x, y);
                                    int l1 = MathHelper.mulColor(k1, mapcolor.colorValue) & 0xffffff;
                                    bruhImage.setPixel(x, y, j1 | l1);
                                }

                                image.overlayPixel(x, y, bruhImage.getPixel(x, y));
                            }
                        }
                    }
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Couldn't load layered image", exception);
            return;
        }

        if (Config.isShaders()) {
            ShadersTex.loadSimpleTexture(this.getGlTextureId(), image, false, false, resourceManager, this.textureLocation, this.getMultiTexID());
        } else {
            TextureUtil.uploadTextureImage(this.getGlTextureId(), image);
        }
    }
}
