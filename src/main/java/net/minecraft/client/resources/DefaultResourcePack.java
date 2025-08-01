package net.minecraft.client.resources;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.radiant.NativeImage;

import java.io.*;
import java.util.Map;
import java.util.Set;

public class DefaultResourcePack implements IResourcePack {
    public static final Set<String> DEFAULT_RESOURCE_DOMAINS = ImmutableSet.of("minecraft");
    private final Map<String, File> mapAssets;

    public DefaultResourcePack(Map<String, File> mapAssetsIn) {
        this.mapAssets = mapAssetsIn;
    }

    public InputStream getInputStream(ResourceLocation location) throws IOException {
        InputStream inputstream = this.getResourceStream(location);

        if (inputstream != null) {
            return inputstream;
        } else {
            InputStream inputstream1 = this.getInputStreamAssets(location);

            if (inputstream1 != null) {
                return inputstream1;
            } else {
                throw new FileNotFoundException(location.getResourcePath());
            }
        }
    }

    public InputStream getInputStreamAssets(ResourceLocation location) throws IOException {
        File file1 = this.mapAssets.get(location.toString());
        return file1 != null && file1.isFile() ? new FileInputStream(file1) : null;
    }

    private InputStream getResourceStream(ResourceLocation location) {
        String s = "/assets/" + location.getResourceDomain() + "/" + location.getResourcePath();
        return DefaultResourcePack.class.getResourceAsStream(s);
    }

    public boolean resourceExists(ResourceLocation location) {
        return this.getResourceStream(location) != null || this.mapAssets.containsKey(location.toString());
    }

    public Set<String> getResourceDomains() {
        return DEFAULT_RESOURCE_DOMAINS;
    }

    public <T extends IMetadataSection> T getPackMetadata(IMetadataSerializer metadataSerializer, String metadataSectionName) {
        try {
            InputStream inputstream = new FileInputStream(this.mapAssets.get("pack.mcmeta"));
            return AbstractResourcePack.readMetadata(metadataSerializer, inputstream, metadataSectionName);
        } catch (RuntimeException | FileNotFoundException exception) {
            return null;
        }
    }

    public NativeImage getPackImage() throws IOException {
        return TextureUtil.readNativeImage(getClass().getResourceAsStream("/" + (new ResourceLocation("pack.png")).getResourcePath()));
    }

    public String getPackName() {
        return "Default";
    }
}
