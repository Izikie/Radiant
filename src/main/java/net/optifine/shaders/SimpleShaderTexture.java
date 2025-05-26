package net.optifine.shaders;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.*;
import net.optifine.Log;
import org.apache.commons.io.IOUtils;

import java.awt.image.BufferedImage;
import java.io.*;

public class SimpleShaderTexture extends AbstractTexture {
    private final String texturePath;
    private static final IMetadataSerializer METADATA_SERIALIZER = makeMetadataSerializer();

    public SimpleShaderTexture(String texturePath) {
        this.texturePath = texturePath;
    }

    public void loadTexture(IResourceManager resourceManager) throws IOException {
        this.deleteGlTexture();
        InputStream inputstream = Shaders.getShaderPackResourceStream(this.texturePath);

        if (inputstream == null) {
            throw new FileNotFoundException("Shader texture not found: " + this.texturePath);
        } else {
            try {
                BufferedImage bufferedimage = TextureUtil.readBufferedImage(inputstream);
                TextureMetadataSection texturemetadatasection = loadTextureMetadataSection(this.texturePath, new TextureMetadataSection(false, false, new IntArrayList()));
                TextureUtil.uploadTextureImageAllocate(this.getGlTextureId(), bufferedimage, texturemetadatasection.getTextureBlur(), texturemetadatasection.getTextureClamp());
            } finally {
                IOUtils.closeQuietly(inputstream);
            }
        }
    }

    public static TextureMetadataSection loadTextureMetadataSection(String texturePath, TextureMetadataSection def) {
        String s = texturePath + ".mcmeta";
        String s1 = "texture";
        InputStream inputstream = Shaders.getShaderPackResourceStream(s);

        if (inputstream != null) {
            BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(inputstream));
            TextureMetadataSection texturemetadatasection1;

            try {
                JsonObject jsonobject = JsonParser.parseReader(bufferedreader).getAsJsonObject();
                TextureMetadataSection texturemetadatasection = METADATA_SERIALIZER.parseMetadataSection(s1, jsonobject);

                if (texturemetadatasection == null) {
                    return def;
                }

                texturemetadatasection1 = texturemetadatasection;
            } catch (RuntimeException exception) {
                Log.warn("Error reading metadata: " + s);
                Log.warn(exception.getClass().getName() + ": " + exception.getMessage());
                return def;
            } finally {
                IOUtils.closeQuietly(bufferedreader);
                IOUtils.closeQuietly(inputstream);
            }

            return texturemetadatasection1;
        } else {
            return def;
        }
    }

    private static IMetadataSerializer makeMetadataSerializer() {
        IMetadataSerializer serializer = new IMetadataSerializer();
        serializer.registerMetadataSectionType(new TextureMetadataSectionSerializer(), TextureMetadataSection.class);
        serializer.registerMetadataSectionType(new FontMetadataSectionSerializer(), FontMetadataSection.class);
        serializer.registerMetadataSectionType(new AnimationMetadataSectionSerializer(), AnimationMetadataSection.class);
        serializer.registerMetadataSectionType(new PackMetadataSectionSerializer(), PackMetadataSection.class);
        serializer.registerMetadataSectionType(new LanguageMetadataSectionSerializer(), LanguageMetadataSection.class);
        return serializer;
    }
}
