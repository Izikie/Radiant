package net.minecraft.client.resources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.InsecureTextureException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.radiant.util.NativeImage;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SkinManager {
    private static final ExecutorService THREAD_POOL = new ThreadPoolExecutor(0, 2, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
    private final TextureManager textureManager;
    private final File skinCacheDir;
    private final MinecraftSessionService sessionService;
    private final LoadingCache<GameProfile, Map<Type, MinecraftProfileTexture>> skinCacheLoader;

    public SkinManager(TextureManager textureManager, File skinCacheDirectory, MinecraftSessionService sessionService) {
        this.textureManager = textureManager;
        this.skinCacheDir = skinCacheDirectory;
        this.sessionService = sessionService;
        this.skinCacheLoader = CacheBuilder.newBuilder().expireAfterAccess(15L, TimeUnit.SECONDS).build(new CacheLoader<>() {
            @Override
            public Map<Type, MinecraftProfileTexture> load(GameProfile profile) {
                return Minecraft.get().getSessionService().getTextures(profile, false);
            }
        });
    }

    public ResourceLocation loadSkin(MinecraftProfileTexture profileTexture, Type type) {
        return this.loadSkin(profileTexture, type, null);
    }

    public ResourceLocation loadSkin(MinecraftProfileTexture profileTexture, Type type, SkinAvailableCallback skinAvailableCallback) {
        ResourceLocation resourceLocation = new ResourceLocation("skins/" + profileTexture.getHash());
        ITextureObject iTextureObject = this.textureManager.getTexture(resourceLocation);

        if (iTextureObject != null) {
            if (skinAvailableCallback != null) {
                skinAvailableCallback.skinAvailable(type, resourceLocation, profileTexture);
            }
        } else {
            File file1 = new File(this.skinCacheDir, profileTexture.getHash().length() > 2 ? profileTexture.getHash().substring(0, 2) : "xx");
            File file2 = new File(file1, profileTexture.getHash());
            IImageBuffer imageBuffer = type == Type.SKIN ? new ImageBufferDownload() : null;

            ThreadDownloadImageData threadDownloadImageData = new ThreadDownloadImageData(file2, profileTexture.getUrl(), DefaultPlayerSkin.getDefaultSkinLegacy(), new IImageBuffer() {
                @Override
                public NativeImage parseUserSkin(NativeImage image) {
                    if (imageBuffer != null) {
                        image = imageBuffer.parseUserSkin(image);
                    }

                    return image;
                }

                @Override
                public void skinAvailable() {
                    if (imageBuffer != null) {
                        imageBuffer.skinAvailable();
                    }

                    if (skinAvailableCallback != null) {
                        skinAvailableCallback.skinAvailable(type, resourceLocation, profileTexture);
                    }
                }
            });

            this.textureManager.loadTexture(resourceLocation, threadDownloadImageData);
        }

        return resourceLocation;
    }

    public void loadProfileTextures(GameProfile profile, SkinAvailableCallback skinAvailableCallback, boolean requireSecure) {
        THREAD_POOL.submit(() -> {
            Map<Type, MinecraftProfileTexture> map = new EnumMap<>(Type.class);

            try {
                map.putAll(this.sessionService.getTextures(profile, requireSecure));
            } catch (InsecureTextureException _) {
            }

            if (map.isEmpty() && profile.getId().equals(Minecraft.get().getSession().getProfile().getId())) {
                profile.getProperties().clear();
                profile.getProperties().putAll(Minecraft.get().getProfileProperties());
                map.putAll(this.sessionService.getTextures(profile, false));
            }

            Minecraft.get().addScheduledTask(() -> {
                if (map.containsKey(Type.SKIN)) {
                    this.loadSkin(map.get(Type.SKIN), Type.SKIN, skinAvailableCallback);
                }

                if (map.containsKey(Type.CAPE)) {
                    this.loadSkin(map.get(Type.CAPE), Type.CAPE, skinAvailableCallback);
                }
            });
        });
    }

    public Map<Type, MinecraftProfileTexture> loadSkinFromCache(GameProfile profile) {
        return this.skinCacheLoader.getUnchecked(profile);
    }

    public interface SkinAvailableCallback {
        void skinAvailable(Type type, ResourceLocation location, MinecraftProfileTexture profileTexture);
    }
}
