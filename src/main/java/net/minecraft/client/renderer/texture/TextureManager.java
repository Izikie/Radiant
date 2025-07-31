package net.minecraft.client.renderer.texture;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.src.Config;
import net.minecraft.util.ResourceLocation;
import net.optifine.CustomGuis;
import net.optifine.EmissiveTextures;
import net.optifine.Log;
import net.optifine.RandomEntities;
import net.optifine.shaders.ShadersTex;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class TextureManager implements ITickable, IResourceManagerReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextureManager.class);
    private final Map<ResourceLocation, ITextureObject> mapTextureObjects = new HashMap<>();
    private final List<ITickable> listTickables = new ArrayList<>();
    private final Map<String, Integer> mapTextureCounters = new HashMap<>();
    private final IResourceManager theResourceManager;

	public TextureManager(IResourceManager resourceManager) {
        this.theResourceManager = resourceManager;
    }

    public void bindTexture(ResourceLocation resource) {
        if (Config.isRandomEntities()) {
            resource = RandomEntities.getTextureLocation(resource);
        }

        if (Config.isCustomGuis()) {
            resource = CustomGuis.getTextureLocation(resource);
        }

        ITextureObject itextureobject = this.mapTextureObjects.get(resource);

        if (EmissiveTextures.isActive()) {
            itextureobject = EmissiveTextures.getEmissiveTexture(itextureobject, this.mapTextureObjects);
        }

        if (itextureobject == null) {
            itextureobject = new SimpleTexture(resource);
            this.loadTexture(resource, itextureobject);
        }

        if (Config.isShaders()) {
            ShadersTex.bindTexture(itextureobject);
        } else {
            TextureUtil.bindTexture(itextureobject.getGlTextureId());
        }

    }

    public void loadTickableTexture(ResourceLocation textureLocation, ITickableTextureObject textureObj) {
        if (this.loadTexture(textureLocation, textureObj)) {
            this.listTickables.add(textureObj);
        }
    }

    public boolean loadTexture(ResourceLocation textureLocation, ITextureObject textureObj) {
        boolean flag = true;

        try {
            textureObj.loadTexture(this.theResourceManager);
        } catch (IOException exception) {
            LOGGER.warn("Failed to load texture: {}", textureLocation, exception);
            textureObj = TextureUtil.MISSING_TEXTURE;
            this.mapTextureObjects.put(textureLocation, textureObj);
            flag = false;
        } catch (Throwable throwable) {
            final ITextureObject textureObjf = textureObj;
            CrashReport report = CrashReport.makeCrashReport(throwable, "Registering texture");
            CrashReportCategory category = report.makeCategory("Resource location being registered");
            category.addCrashSection("Resource Location", textureLocation);
            category.addCrashSectionCallable("Texture Object Class", () -> textureObjf.getClass().getName());
            throw new ReportedException(report);
        }

        this.mapTextureObjects.put(textureLocation, textureObj);
        return flag;
    }

    public ITextureObject getTexture(ResourceLocation textureLocation) {
        return this.mapTextureObjects.get(textureLocation);
    }

    public ResourceLocation getDynamicTextureLocation(String name, DynamicTexture texture) {
        if (name.equals("logo")) {
            texture = Config.getMojangLogoTexture(texture);
        }

        Integer integer = this.mapTextureCounters.get(name);

        if (integer == null) {
            integer = 1;
        } else {
            integer = integer + 1;
        }

        this.mapTextureCounters.put(name, integer);
        ResourceLocation resourcelocation = new ResourceLocation(String.format("dynamic/%s_%d", name, integer));
        this.loadTexture(resourcelocation, texture);
        return resourcelocation;
    }

    public void tick() {
        for (ITickable itickable : this.listTickables) {
            itickable.tick();
        }
    }

    public void deleteTexture(ResourceLocation textureLocation) {
        ITextureObject itextureobject = this.getTexture(textureLocation);

        if (itextureobject != null) {
            this.mapTextureObjects.remove(textureLocation);
            TextureUtil.deleteTexture(itextureobject.getGlTextureId());
        }
    }

    public void onResourceManagerReload(IResourceManager resourceManager) {
        Log.info("*** Reloading textures ***");
        Log.info("Resource packs: " + Config.getResourcePackNames());
        Iterator<ResourceLocation> iterator = this.mapTextureObjects.keySet().iterator();

        while (iterator.hasNext()) {
            ResourceLocation resourcelocation = iterator.next();
            String s = resourcelocation.getResourcePath();

            if (s.startsWith("mcpatcher/") || s.startsWith("optifine/") || EmissiveTextures.isEmissive(resourcelocation)) {
                ITextureObject itextureobject = this.mapTextureObjects.get(resourcelocation);

                if (itextureobject instanceof AbstractTexture abstracttexture) {
                    abstracttexture.deleteGlTexture();
                }

                iterator.remove();
            }
        }

        EmissiveTextures.update();

        for (Entry<ResourceLocation, ITextureObject> o : new HashSet<>(this.mapTextureObjects.entrySet())) {
	        this.loadTexture(o.getKey(), o.getValue());
        }
    }

    public void reloadBannerTextures() {
        for (Entry<ResourceLocation, ITextureObject> o : new HashSet<>(this.mapTextureObjects.entrySet())) {
	        ResourceLocation resourcelocation = o.getKey();
            ITextureObject itextureobject = o.getValue();

            if (itextureobject instanceof LayeredColorMaskTexture) {
                this.loadTexture(resourcelocation, itextureobject);
            }
        }
    }
}
