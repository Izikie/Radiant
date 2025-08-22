package net.optifine.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerMooshroomMushroom;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.optifine.*;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.shaders.MultiTexID;
import net.optifine.shaders.Shaders;
import net.radiant.lwjgl.opengl.GLContext;
import net.radiant.util.NativeImage;
import org.joml.Vector2i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;

public class TextureUtils {
	public static TextureAtlasSprite iconGrassTop;
	public static TextureAtlasSprite iconGrassSide;
	public static TextureAtlasSprite iconGrassSideOverlay;
	public static TextureAtlasSprite iconSnow;
	public static TextureAtlasSprite iconGrassSideSnowed;
	public static TextureAtlasSprite iconMyceliumSide;
	public static TextureAtlasSprite iconMyceliumTop;
	public static TextureAtlasSprite iconWaterStill;
	public static TextureAtlasSprite iconWaterFlow;
	public static TextureAtlasSprite iconLavaStill;
	public static TextureAtlasSprite iconLavaFlow;
	public static TextureAtlasSprite iconPortal;
	public static TextureAtlasSprite iconFireLayer0;
	public static TextureAtlasSprite iconFireLayer1;
	public static TextureAtlasSprite iconGlass;
	public static TextureAtlasSprite iconGlassPaneTop;
	public static TextureAtlasSprite iconCompass;
	public static TextureAtlasSprite iconClock;

	public static void update() {
		TextureMap texturemap = getTextureMapBlocks();

		if (texturemap != null) {
			String s = "minecraft:blocks/";
			iconGrassTop = texturemap.getSpriteSafe(s + "grass_top");
			iconGrassSide = texturemap.getSpriteSafe(s + "grass_side");
			iconGrassSideOverlay = texturemap.getSpriteSafe(s + "grass_side_overlay");
			iconSnow = texturemap.getSpriteSafe(s + "snow");
			iconGrassSideSnowed = texturemap.getSpriteSafe(s + "grass_side_snowed");
			iconMyceliumSide = texturemap.getSpriteSafe(s + "mycelium_side");
			iconMyceliumTop = texturemap.getSpriteSafe(s + "mycelium_top");
			iconWaterStill = texturemap.getSpriteSafe(s + "water_still");
			iconWaterFlow = texturemap.getSpriteSafe(s + "water_flow");
			iconLavaStill = texturemap.getSpriteSafe(s + "lava_still");
			iconLavaFlow = texturemap.getSpriteSafe(s + "lava_flow");
			iconFireLayer0 = texturemap.getSpriteSafe(s + "fire_layer_0");
			iconFireLayer1 = texturemap.getSpriteSafe(s + "fire_layer_1");
			iconPortal = texturemap.getSpriteSafe(s + "portal");
			iconGlass = texturemap.getSpriteSafe(s + "glass");
			iconGlassPaneTop = texturemap.getSpriteSafe(s + "glass_pane_top");
			String s1 = "minecraft:items/";
			iconCompass = texturemap.getSpriteSafe(s1 + "compass");
			iconClock = texturemap.getSpriteSafe(s1 + "clock");
		}
	}

	public static int ceilPowerOfTwo(int val) {
		int i;

		for (i = 1; i < val; i *= 2) {
		}

		return i;
	}

	public static ITextureObject getTexture(ResourceLocation loc) {
		ITextureObject itextureobject = Config.getTextureManager().getTexture(loc);

		if (itextureobject != null) {
			return itextureobject;
		} else if (!Config.hasResource(loc)) {
			return null;
		} else {
			SimpleTexture simpletexture = new SimpleTexture(loc);
			Config.getTextureManager().loadTexture(loc, simpletexture);
			return simpletexture;
		}
	}

	public static void resourcesReloaded(IResourceManager rm) {
		if (getTextureMapBlocks() != null) {
			Log.info("*** Reloading custom textures ***");
			CustomSky.reset();
			TextureAnimations.reset();
			update();
			NaturalTextures.update();
			BetterGrass.update();
			BetterSnow.update();
			TextureAnimations.update();
			CustomColors.update();
			CustomSky.update();
			RandomEntities.update();
			CustomItems.updateModels();
			CustomEntityModels.update();
			Shaders.resourcesReloaded();
			Lang.resourcesReloaded();
			Config.updateTexturePackClouds();
			SmartLeaves.updateLeavesModels();
			CustomPanorama.update();
			CustomGuis.update();
			LayerMooshroomMushroom.update();
			CustomLoadingScreens.update();
			CustomBlockLayers.update();
			Config.getTextureManager().tick();
		}
	}

	public static TextureMap getTextureMapBlocks() {
		return Minecraft.get().getTextureMapBlocks();
	}

	public static void registerResourceListener() {
		IResourceManager iresourcemanager = Config.getResourceManager();

		if (iresourcemanager instanceof IReloadableResourceManager ireloadableresourcemanager) {
			IResourceManagerReloadListener iresourcemanagerreloadlistener = TextureUtils::resourcesReloaded;
			ireloadableresourcemanager.registerReloadListener(iresourcemanagerreloadlistener);
		}

		ITickableTextureObject itickabletextureobject = new ITickableTextureObject() {
			public void tick() {
				TextureAnimations.updateAnimations();
			}

			public void loadTexture(IResourceManager var1) {
			}

			public int getGlTextureId() {
				return 0;
			}

			public void setBlurMipmap(boolean p_174936_1, boolean p_174936_2) {
			}

			public void restoreLastBlurMipmap() {
			}

			public MultiTexID getMultiTexID() {
				return null;
			}
		};
		ResourceLocation resourcelocation = new ResourceLocation("optifine/TickableTextures");
		Config.getTextureManager().loadTickableTexture(resourcelocation, itickabletextureobject);
	}

	public static String fixResourcePath(String path, String basePath) {
		String s = "assets/minecraft/";

		if (path.startsWith(s)) {
			path = path.substring(s.length());
			return path;
		} else if (path.startsWith("./")) {
			path = path.substring(2);

			if (!basePath.endsWith("/")) {
				basePath = basePath + "/";
			}

			path = basePath + path;
			return path;
		} else {
			if (path.startsWith("/~")) {
				path = path.substring(1);
			}

			String s1 = "mcpatcher/";

			if (path.startsWith("~/")) {
				path = path.substring(2);
				path = s1 + path;
				return path;
			} else if (path.startsWith("/")) {
				path = s1 + path.substring(1);
				return path;
			} else {
				return path;
			}
		}
	}

	public static String getBasePath(String path) {
		int i = path.lastIndexOf(47);
		return i < 0 ? "" : path.substring(0, i);
	}

	public static void applyAnisotropicLevel() {
		if (GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic) {
			float f = GL11.glGetFloat(34047);
			float f1 = Config.getAnisotropicFilterLevel();
			f1 = Math.min(f1, f);
			GL11.glTexParameterf(GL11.GL_TEXTURE_2D, 34046, f1);
		}
	}

	public static void bindTexture(int glTexId) {
		GlStateManager.bindTexture(glTexId);
	}

	public static boolean isPowerOfTwo(int x) {
		int i = MathHelper.roundUpToPowerOfTwo(x);
		return i == x;
	}

	public static NativeImage scaleImage(NativeImage bi, int w2) {
		int i = bi.getWidth();
		int j = bi.getHeight();

		int k = j * w2 / i;

		return bi.resized(w2, k);
	}

	public static int scaleToGrid(int size, int sizeGrid) {
		if (size == sizeGrid) {
			return size;
		} else {
			int i;

			for (i = size / sizeGrid * sizeGrid; i < size; i += sizeGrid) {
			}

			return i;
		}
	}

	public static int scaleToMin(int size, int sizeMin) {
		if (size >= sizeMin) {
			return size;
		} else {
			int i;

			for (i = sizeMin / size * size; i < sizeMin; i += size) {
			}

			return i;
		}
	}

	public static Vector2i getImageSize(InputStream in, String suffix) {
		try {
			NativeImage image = NativeImage.loadFromInputStream(in);
			return new Vector2i(image.getWidth(), image.getHeight());
		} catch (IOException _) {}

		return null;
	}

	public static void saveGlTexture(String name, int textureId, int mipmapLevels, int width, int height) {
		bindTexture(textureId);
		GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
		File file1 = new File(name);
		File file2 = file1.getParentFile();

		if (file2 != null) {
			file2.mkdirs();
		}

		for (int i = 0; i < 16; ++i) {
			File file3 = new File(name + "_" + i + ".png");
			file3.delete();
		}

		for (int i1 = 0; i1 <= mipmapLevels; ++i1) {
			File file4 = new File(name + "_" + i1 + ".png");
			int j = width >> i1;
			int k = height >> i1;
			int l = j * k;
			IntBuffer intbuffer = BufferUtils.createIntBuffer(l);
			int[] aint = new int[l];
			GL11.glGetTexImage(GL11.GL_TEXTURE_2D, i1, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, intbuffer);
			intbuffer.get(aint);
			NativeImage image = NativeImage.createBlankImage(j, k);
			image.setRGB(0, 0, j, k, aint, 0, j);

			try {
				image.saveToFile(file4, NativeImage.FileFormat.PNG);
				Log.info("Exported: " + file4);
			} catch (Exception exception) {
				Log.warn("Error writing: " + file4);
				Log.warn(exception.getClass().getName() + ": " + exception.getMessage());
			}
		}
	}

	public static int getGLMaximumTextureSize() {
		for (int i = 65536; i > 0; i >>= 1) {
			GlStateManager.glTexImage2D(32868, 0, 6408, i, i, 0, 6408, 5121, null);
			int k = GlStateManager.glGetTexLevelParameteri(32868, 0, 4096);

			if (k != 0) {
				return i;
			}
		}

		return -1;
	}
}
