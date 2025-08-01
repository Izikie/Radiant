package net.minecraft.client.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.radiant.NativeImage;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public abstract class AbstractResourcePack implements IResourcePack {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractResourcePack.class);
    public final File resourcePackFile;

    public AbstractResourcePack(File resourcePackFileIn) {
        this.resourcePackFile = resourcePackFileIn;
    }

    private static String locationToName(ResourceLocation location) {
        return String.format("%s/%s/%s", "assets", location.getResourceDomain(), location.getResourcePath());
    }

    protected static String getRelativeName(File p_110595_0_, File p_110595_1_) {
        return p_110595_0_.toURI().relativize(p_110595_1_.toURI()).getPath();
    }

    public InputStream getInputStream(ResourceLocation location) throws IOException {
        return this.getInputStreamByName(locationToName(location));
    }

    public boolean resourceExists(ResourceLocation location) {
        return this.hasResourceName(locationToName(location));
    }

    protected abstract InputStream getInputStreamByName(String name) throws IOException;

    protected abstract boolean hasResourceName(String name);

    protected void logNameNotLowercase(String name) {
        LOGGER.warn("ResourcePack: ignored non-lowercase namespace: {} in {}", new Object[]{name, this.resourcePackFile});
    }

    public <T extends IMetadataSection> T getPackMetadata(IMetadataSerializer metadataSerializer, String metadataSectionName) throws IOException {
        return readMetadata(metadataSerializer, this.getInputStreamByName("pack.mcmeta"), metadataSectionName);
    }

    static <T extends IMetadataSection> T readMetadata(IMetadataSerializer p_110596_0_, InputStream p_110596_1_, String p_110596_2_) {
        JsonObject jsonobject;
        BufferedReader bufferedreader = null;

        try {
            bufferedreader = new BufferedReader(new InputStreamReader(p_110596_1_, StandardCharsets.UTF_8));
            jsonobject = JsonParser.parseReader(bufferedreader).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new JsonParseException(exception);
        } finally {
            IOUtils.closeQuietly(bufferedreader);
        }

        return p_110596_0_.parseMetadataSection(p_110596_2_, jsonobject);
    }

    public NativeImage getPackImage() throws IOException {
        return TextureUtil.readNativeImage(this.getInputStreamByName("pack.png"));
    }

    public String getPackName() {
        return this.resourcePackFile.getName();
    }
}
