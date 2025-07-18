package net.optifine;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockStem;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.src.Config;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.optifine.config.ConnectedParser;
import net.optifine.config.MatchBlock;
import net.optifine.render.RenderEnv;
import net.optifine.util.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class CustomColors {
	public static final Random RANDOM = new Random();
	private static final CustomColorFader SKY_COLOR_FADER = new CustomColorFader();
	private static final CustomColorFader FOG_COLOR_FADER = new CustomColorFader();
	private static final CustomColorFader UNDERWATER_COLOR_FADER = new CustomColorFader();
	private static final CustomColorFader UNDERLAVA_COLOR_FADER = new CustomColorFader();
	private static final IBlockState BLOCK_STATE_DIRT = Blocks.DIRT.getDefaultState();
	private static final IBlockState BLOCK_STATE_WATER = Blocks.WATER.getDefaultState();
	private static String paletteFormatDefault = "vanilla";
	private static CustomColormap waterColors = null;
	private static final IColorizer COLORIZER_WATER = new IColorizer() {
		public int getColor(IBlockState blockState, IBlockAccess blockAccess, BlockPos blockPos) {
			BiomeGenBase biomegenbase = CustomColors.getColorBiome(blockAccess, blockPos);
			return CustomColors.waterColors != null ? CustomColors.waterColors.getColor(biomegenbase, blockPos) : (biomegenbase.waterColorMultiplier);
		}

		public boolean isColorConstant() {
			return false;
		}
	};
	private static CustomColormap foliagePineColors = null;
	private static final IColorizer COLORIZER_FOLIAGE_PINE = new IColorizer() {
		public int getColor(IBlockState blockState, IBlockAccess blockAccess, BlockPos blockPos) {
			return CustomColors.foliagePineColors != null ? CustomColors.foliagePineColors.getColor(blockAccess, blockPos) : ColorizerFoliage.getFoliageColorPine();
		}

		public boolean isColorConstant() {
			return CustomColors.foliagePineColors == null;
		}
	};
	private static CustomColormap foliageBirchColors = null;
	private static final IColorizer COLORIZER_FOLIAGE_BIRCH = new IColorizer() {
		public int getColor(IBlockState blockState, IBlockAccess blockAccess, BlockPos blockPos) {
			return CustomColors.foliageBirchColors != null ? CustomColors.foliageBirchColors.getColor(blockAccess, blockPos) : ColorizerFoliage.getFoliageColorBirch();
		}

		public boolean isColorConstant() {
			return CustomColors.foliageBirchColors == null;
		}
	};
	private static CustomColormap swampFoliageColors = null;
	private static final IColorizer COLORIZER_FOLIAGE = new IColorizer() {
		public int getColor(IBlockState blockState, IBlockAccess blockAccess, BlockPos blockPos) {
			BiomeGenBase biomegenbase = CustomColors.getColorBiome(blockAccess, blockPos);
			return CustomColors.swampFoliageColors != null && biomegenbase == BiomeGenBase.SWAMPLAND ? CustomColors.swampFoliageColors.getColor(biomegenbase, blockPos) : biomegenbase.getFoliageColorAtPos(blockPos);
		}

		public boolean isColorConstant() {
			return false;
		}
	};
	private static CustomColormap swampGrassColors = null;
	private static final IColorizer COLORIZER_GRASS = new IColorizer() {
		public int getColor(IBlockState blockState, IBlockAccess blockAccess, BlockPos blockPos) {
			BiomeGenBase biomegenbase = CustomColors.getColorBiome(blockAccess, blockPos);
			return CustomColors.swampGrassColors != null && biomegenbase == BiomeGenBase.SWAMPLAND ? CustomColors.swampGrassColors.getColor(biomegenbase, blockPos) : biomegenbase.getGrassColorAtPos(blockPos);
		}

		public boolean isColorConstant() {
			return false;
		}
	};
	private static CustomColormap[] colorsBlockColormaps = null;
	private static CustomColormap[][] blockColormaps = null;
	private static CustomColormap skyColors = null;
	private static CustomColormap fogColors = null;
	private static CustomColormap underwaterColors = null;
	private static CustomColormap underlavaColors = null;
	private static LightMapPack[] lightMapPacks = null;
	private static int lightmapMinDimensionId = 0;
	private static CustomColormap redstoneColors = null;
	private static CustomColormap xpOrbColors = null;
	private static int xpOrbTime = -1;
	private static CustomColormap durabilityColors = null;
	private static CustomColormap stemColors = null;
	private static CustomColormap stemMelonColors = null;
	private static CustomColormap stemPumpkinColors = null;
	private static CustomColormap myceliumParticleColors = null;
	private static boolean useDefaultGrassFoliageColors = true;
	private static int particleWaterColor = -1;
	private static int particlePortalColor = -1;
	private static int lilyPadColor = -1;
	private static int expBarTextColor = -1;
	private static int bossTextColor = -1;
	private static int signTextColor = -1;
	private static Vec3 fogColorNether = null;
	private static Vec3 fogColorEnd = null;
	private static Vec3 skyColorEnd = null;
	private static int[] spawnEggPrimaryColors = null;
	private static int[] spawnEggSecondaryColors = null;
	private static float[][] wolfCollarColors = null;
	private static float[][] sheepColors = null;
	private static int[] textColors = null;
	private static int[] mapColorsOriginal = null;
	private static int[] potionColors = null;

	public static void update() {
		paletteFormatDefault = "vanilla";
		waterColors = null;
		foliageBirchColors = null;
		foliagePineColors = null;
		swampGrassColors = null;
		swampFoliageColors = null;
		skyColors = null;
		fogColors = null;
		underwaterColors = null;
		underlavaColors = null;
		redstoneColors = null;
		xpOrbColors = null;
		xpOrbTime = -1;
		durabilityColors = null;
		stemColors = null;
		myceliumParticleColors = null;
		lightMapPacks = null;
		particleWaterColor = -1;
		particlePortalColor = -1;
		lilyPadColor = -1;
		expBarTextColor = -1;
		bossTextColor = -1;
		signTextColor = -1;
		fogColorNether = null;
		fogColorEnd = null;
		skyColorEnd = null;
		colorsBlockColormaps = null;
		blockColormaps = null;
		useDefaultGrassFoliageColors = true;
		spawnEggPrimaryColors = null;
		spawnEggSecondaryColors = null;
		wolfCollarColors = null;
		sheepColors = null;
		textColors = null;
		setMapColors(mapColorsOriginal);
		potionColors = null;
		paletteFormatDefault = getValidProperty("mcpatcher/color.properties", "palette.format", CustomColormap.FORMAT_STRINGS, "vanilla");
		String s = "mcpatcher/colormap/";
		String[] astring = new String[]{"water.png", "watercolorX.png"};
		waterColors = getCustomColors(s, astring, 256, 256);
		updateUseDefaultGrassFoliageColors();

		if (Config.isCustomColors()) {
			String[] astring1 = new String[]{"pine.png", "pinecolor.png"};
			foliagePineColors = getCustomColors(s, astring1, 256, 256);
			String[] astring2 = new String[]{"birch.png", "birchcolor.png"};
			foliageBirchColors = getCustomColors(s, astring2, 256, 256);
			String[] astring3 = new String[]{"swampgrass.png", "swampgrasscolor.png"};
			swampGrassColors = getCustomColors(s, astring3, 256, 256);
			String[] astring4 = new String[]{"swampfoliage.png", "swampfoliagecolor.png"};
			swampFoliageColors = getCustomColors(s, astring4, 256, 256);
			String[] astring5 = new String[]{"sky0.png", "skycolor0.png"};
			skyColors = getCustomColors(s, astring5, 256, 256);
			String[] astring6 = new String[]{"fog0.png", "fogcolor0.png"};
			fogColors = getCustomColors(s, astring6, 256, 256);
			String[] astring7 = new String[]{"underwater.png", "underwatercolor.png"};
			underwaterColors = getCustomColors(s, astring7, 256, 256);
			String[] astring8 = new String[]{"underlava.png", "underlavacolor.png"};
			underlavaColors = getCustomColors(s, astring8, 256, 256);
			String[] astring9 = new String[]{"redstone.png", "redstonecolor.png"};
			redstoneColors = getCustomColors(s, astring9, 16, 1);
			xpOrbColors = getCustomColors(s + "xporb.png", -1, -1);
			durabilityColors = getCustomColors(s + "durability.png", -1, -1);
			String[] astring10 = new String[]{"stem.png", "stemcolor.png"};
			stemColors = getCustomColors(s, astring10, 8, 1);
			stemPumpkinColors = getCustomColors(s + "pumpkinstem.png", 8, 1);
			stemMelonColors = getCustomColors(s + "melonstem.png", 8, 1);
			String[] astring11 = new String[]{"myceliumparticle.png", "myceliumparticlecolor.png"};
			myceliumParticleColors = getCustomColors(s, astring11, -1, -1);
			Pair<LightMapPack[], Integer> pair = parseLightMapPacks();
			lightMapPacks = pair.getLeft();
			lightmapMinDimensionId = pair.getRight();
			readColorProperties("mcpatcher/color.properties");
			blockColormaps = readBlockColormaps(new String[]{s + "custom/", s + "blocks/"}, colorsBlockColormaps, 256, 256);
			updateUseDefaultGrassFoliageColors();
		}
	}

	private static String getValidProperty(String fileName, String key, String[] validValues, String valDef) {
		try {
			ResourceLocation resourcelocation = new ResourceLocation(fileName);
			InputStream inputstream = Config.getResourceStream(resourcelocation);

			if (inputstream == null) {
				return valDef;
			} else {
				Properties properties = new PropertiesOrdered();
				properties.load(inputstream);
				inputstream.close();
				String s = properties.getProperty(key);

				if (s == null) {
					return valDef;
				} else {
					List<String> list = Arrays.asList(validValues);

					if (!list.contains(s)) {
						warn("Invalid value: " + key + "=" + s);
						warn("Expected values: " + Config.arrayToString(validValues));
						return valDef;
					} else {
						dbg(key + "=" + s);
						return s;
					}
				}
			}
		} catch (FileNotFoundException exception) {
			return valDef;
		} catch (IOException exception) {
			exception.printStackTrace();
			return valDef;
		}
	}

	private static Pair<LightMapPack[], Integer> parseLightMapPacks() {
		String s = "mcpatcher/lightmap/world";
		String s1 = ".png";
		String[] astring = ResUtils.collectFiles(s, s1);
		Int2ObjectMap<String> map = new Int2ObjectOpenHashMap<>();

		for (String s2 : astring) {
			String s3 = StrUtils.removePrefixSuffix(s2, s, s1);
			int j = Config.parseInt(s3, Integer.MIN_VALUE);

			if (j == Integer.MIN_VALUE) {
				warn("Invalid dimension ID: " + s3 + ", path: " + s2);
			} else {
				map.put(j, s2);
			}
		}

		int[] keys = map.keySet().toIntArray();
		Arrays.sort(keys);

		if (keys.length == 0) {
			return new ImmutablePair<>(null, 0);
		} else {
			int j1 = keys[0];
			int k1 = keys[keys.length - 1];
			int k = k1 - j1 + 1;
			CustomColormap[] acustomcolormap = new CustomColormap[k];

			for (int integer : keys) {
				String s4 = map.get(integer);
				CustomColormap customcolormap = getCustomColors(s4, -1, -1);

				if (customcolormap != null) {
					if (customcolormap.getWidth() < 16) {
						warn("Invalid lightmap width: " + customcolormap.getWidth() + ", path: " + s4);
					} else {
						int i1 = integer - j1;
						acustomcolormap[i1] = customcolormap;
					}
				}
			}

			LightMapPack[] alightmappack = new LightMapPack[acustomcolormap.length];

			for (int l1 = 0; l1 < acustomcolormap.length; ++l1) {
				CustomColormap customcolormap3 = acustomcolormap[l1];

				if (customcolormap3 != null) {
					String s5 = customcolormap3.name;
					String s6 = customcolormap3.basePath;
					CustomColormap customcolormap1 = getCustomColors(s6 + "/" + s5 + "_rain.png", -1, -1);
					CustomColormap customcolormap2 = getCustomColors(s6 + "/" + s5 + "_thunder.png", -1, -1);
					LightMap lightmap = new LightMap(customcolormap3);
					LightMap lightmap1 = customcolormap1 != null ? new LightMap(customcolormap1) : null;
					LightMap lightmap2 = customcolormap2 != null ? new LightMap(customcolormap2) : null;
					LightMapPack lightmappack = new LightMapPack(lightmap, lightmap1, lightmap2);
					alightmappack[l1] = lightmappack;
				}
			}

			return new ImmutablePair<>(alightmappack, j1);
		}
	}

	private static int getTextureHeight(String path, int defHeight) {
		try {
			InputStream inputstream = Config.getResourceStream(new ResourceLocation(path));

			if (inputstream == null) {
				return defHeight;
			} else {
				BufferedImage bufferedimage = ImageIO.read(inputstream);
				inputstream.close();
				return bufferedimage == null ? defHeight : bufferedimage.getHeight();
			}
		} catch (IOException exception) {
			return defHeight;
		}
	}

	private static void readColorProperties(String fileName) {
		try {
			ResourceLocation resourcelocation = new ResourceLocation(fileName);
			InputStream inputstream = Config.getResourceStream(resourcelocation);

			if (inputstream == null) {
				return;
			}

			dbg("Loading " + fileName);
			Properties properties = new PropertiesOrdered();
			properties.load(inputstream);
			inputstream.close();
			particleWaterColor = readColor(properties, new String[]{"particle.water", "drop.water"});
			particlePortalColor = readColor(properties, "particle.portal");
			lilyPadColor = readColor(properties, "lilypad");
			expBarTextColor = readColor(properties, "text.xpbar");
			bossTextColor = readColor(properties, "text.boss");
			signTextColor = readColor(properties, "text.sign");
			fogColorNether = readColorVec3(properties, "fog.nether");
			fogColorEnd = readColorVec3(properties, "fog.end");
			skyColorEnd = readColorVec3(properties, "sky.end");
			colorsBlockColormaps = readCustomColormaps(properties, fileName);
			spawnEggPrimaryColors = readSpawnEggColors(properties, fileName, "egg.shell.", "Spawn egg shell");
			spawnEggSecondaryColors = readSpawnEggColors(properties, fileName, "egg.spots.", "Spawn egg spot");
			wolfCollarColors = readDyeColors(properties, fileName, "collar.", "Wolf collar");
			sheepColors = readDyeColors(properties, fileName, "sheep.", "Sheep");
			textColors = readTextColors(properties, fileName, "text.code.", "Text");
			int[] aint = readMapColors(properties, fileName, "map.", "Map");

			if (aint != null) {
				if (mapColorsOriginal == null) {
					mapColorsOriginal = getMapColors();
				}

				setMapColors(aint);
			}

			potionColors = readPotionColors(properties, fileName, "potion.", "Potion");
			xpOrbTime = Config.parseInt(properties.getProperty("xporb.time"), -1);
		} catch (FileNotFoundException exception) {
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}

	private static CustomColormap[] readCustomColormaps(Properties props, String fileName) {
		List list = new ArrayList<>();
		String s = "palette.block.";
		Map map = new HashMap<>();

		for (Object o : props.keySet()) {
			String s1 = (String) o;
			String s2 = props.getProperty(s1);

			if (s1.startsWith(s)) {
				map.put(s1, s2);
			}
		}

		String[] astring = (String[]) map.keySet().toArray(new String[0]);

		for (String s6 : astring) {
			String s3 = props.getProperty(s6);
			dbg("Block palette: " + s6 + " = " + s3);
			String s4 = s6.substring(s.length());
			String s5 = TextureUtils.getBasePath(fileName);
			s4 = TextureUtils.fixResourcePath(s4, s5);
			CustomColormap customcolormap = getCustomColors(s4, 256, 256);

			if (customcolormap == null) {
				warn("Colormap not found: " + s4);
			} else {
				ConnectedParser connectedparser = new ConnectedParser("CustomColors");
				MatchBlock[] amatchblock = connectedparser.parseMatchBlocks(s3);

				if (amatchblock != null && amatchblock.length > 0) {
					for (MatchBlock matchblock : amatchblock) {
						customcolormap.addMatchBlock(matchblock);
					}

					list.add(customcolormap);
				} else {
					warn("Invalid match blocks: " + s3);
				}
			}
		}

		if (list.isEmpty()) {
			return null;
		} else {
			return (CustomColormap[]) list.toArray(new CustomColormap[0]);
		}
	}

	private static CustomColormap[][] readBlockColormaps(String[] basePaths, CustomColormap[] basePalettes, int width, int height) {
		String[] astring = ResUtils.collectFiles(basePaths, new String[]{".properties"});
		Arrays.sort(astring);
		List list = new ArrayList<>();

		for (String s : astring) {
			dbg("Block colormap: " + s);

			try {
				ResourceLocation resourcelocation = new ResourceLocation("minecraft", s);
				InputStream inputstream = Config.getResourceStream(resourcelocation);

				if (inputstream == null) {
					warn("File not found: " + s);
				} else {
					Properties properties = new PropertiesOrdered();
					properties.load(inputstream);
					inputstream.close();
					CustomColormap customcolormap = new CustomColormap(properties, s, width, height, paletteFormatDefault);

					if (customcolormap.isValid(s) && customcolormap.isValidMatchBlocks(s)) {
						addToBlockList(customcolormap, list);
					}
				}
			} catch (FileNotFoundException exception) {
				warn("File not found: " + s);
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}

		if (basePalettes != null) {
			for (CustomColormap customcolormap1 : basePalettes) {
				addToBlockList(customcolormap1, list);
			}
		}

		if (list.isEmpty()) {
			return null;
		} else {
			return blockListToArray(list);
		}
	}

	private static void addToBlockList(CustomColormap cm, List blockList) {
		int[] aint = cm.getMatchBlockIds();

		if (aint != null && aint.length > 0) {
			for (int j : aint) {
				if (j < 0) {
					warn("Invalid block ID: " + j);
				} else {
					addToList(cm, blockList, j);
				}
			}
		} else {
			warn("No match blocks: " + Config.arrayToString(aint));
		}
	}

	private static void addToList(CustomColormap cm, List lists, int id) {
		while (id >= lists.size()) {
			lists.add(null);
		}

		List list = (List) lists.get(id);

		if (list == null) {
			list = new ArrayList<>();
			list.set(id, list);
		}

		list.add(cm);
	}

	private static CustomColormap[][] blockListToArray(List lists) {
		CustomColormap[][] acustomcolormap = new CustomColormap[lists.size()][];

		for (int i = 0; i < lists.size(); ++i) {
			List list = (List) lists.get(i);

			if (list != null) {
				CustomColormap[] acustomcolormap1 = (CustomColormap[]) list.toArray(new CustomColormap[0]);
				acustomcolormap[i] = acustomcolormap1;
			}
		}

		return acustomcolormap;
	}

	private static int readColor(Properties props, String[] names) {
		for (String s : names) {
			int j = readColor(props, s);

			if (j >= 0) {
				return j;
			}
		}

		return -1;
	}

	private static int readColor(Properties props, String name) {
		String s = props.getProperty(name);

		if (s == null) {
			return -1;
		} else {
			s = s.trim();
			int i = parseColor(s);

			if (i < 0) {
				warn("Invalid color: " + name + " = " + s);
			} else {
				dbg(name + " = " + s);
			}
			return i;
		}
	}

	private static int parseColor(String str) {
		if (str == null) {
			return -1;
		} else {
			str = str.trim();

			try {
				return Integer.parseInt(str, 16) & 16777215;
			} catch (NumberFormatException exception) {
				return -1;
			}
		}
	}

	private static Vec3 readColorVec3(Properties props, String name) {
		int i = readColor(props, name);

		if (i < 0) {
			return null;
		} else {
			int j = i >> 16 & 255;
			int k = i >> 8 & 255;
			int l = i & 255;
			float f = j / 255.0F;
			float f1 = k / 255.0F;
			float f2 = l / 255.0F;
			return new Vec3(f, f1, f2);
		}
	}

	private static CustomColormap getCustomColors(String basePath, String[] paths, int width, int height) {
		for (String path : paths) {
			String s = path;
			s = basePath + s;
			CustomColormap customcolormap = getCustomColors(s, width, height);

			if (customcolormap != null) {
				return customcolormap;
			}
		}

		return null;
	}

	public static CustomColormap getCustomColors(String pathImage, int width, int height) {
		try {
			ResourceLocation resourcelocation = new ResourceLocation(pathImage);

			if (!Config.hasResource(resourcelocation)) {
				return null;
			} else {
				dbg("Colormap " + pathImage);
				Properties properties = new PropertiesOrdered();
				String s = StrUtils.replaceSuffix(pathImage, ".png", ".properties");
				ResourceLocation resourcelocation1 = new ResourceLocation(s);

				if (Config.hasResource(resourcelocation1)) {
					InputStream inputstream = Config.getResourceStream(resourcelocation1);
					properties.load(inputstream);
					inputstream.close();
					dbg("Colormap properties: " + s);
				} else {
					properties.put("format", paletteFormatDefault);
					properties.put("source", pathImage);
					s = pathImage;
				}

				CustomColormap customcolormap = new CustomColormap(properties, s, width, height, paletteFormatDefault);
				return !customcolormap.isValid(s) ? null : customcolormap;
			}
		} catch (Exception exception) {
			exception.printStackTrace();
			return null;
		}
	}

	public static void updateUseDefaultGrassFoliageColors() {
		useDefaultGrassFoliageColors = foliageBirchColors == null && foliagePineColors == null && swampGrassColors == null && swampFoliageColors == null && Config.isSwampColors() && Config.isSmoothBiomes();
	}

	public static int getColorMultiplier(BakedQuad quad, IBlockState blockState, IBlockAccess blockAccess, BlockPos blockPos, RenderEnv renderEnv) {
		Block block = blockState.getBlock();
		IBlockState iblockstate = renderEnv.getBlockState();

		if (blockColormaps != null) {
			if (!quad.hasTintIndex()) {
				if (block == Blocks.GRASS) {
					iblockstate = BLOCK_STATE_DIRT;
				}

				if (block == Blocks.REDSTONE_WIRE) {
					return -1;
				}
			}

			if (block == Blocks.DOUBLE_PLANT && renderEnv.getMetadata() >= 8) {
				blockPos = blockPos.down();
				iblockstate = blockAccess.getBlockState(blockPos);
			}

			CustomColormap customcolormap = getBlockColormap(iblockstate);

			if (customcolormap != null) {
				if (Config.isSmoothBiomes() && !customcolormap.isColorConstant()) {
					return getSmoothColorMultiplier(blockState, blockAccess, blockPos, customcolormap, renderEnv.getColorizerBlockPosM());
				}

				return customcolormap.getColor(blockAccess, blockPos);
			}
		}

		if (!quad.hasTintIndex()) {
			return -1;
		} else if (block == Blocks.WATERLILY) {
			return getLilypadColorMultiplier(blockAccess, blockPos);
		} else if (block == Blocks.REDSTONE_WIRE) {
			return getRedstoneColor(renderEnv.getBlockState());
		} else if (block instanceof BlockStem) {
			return getStemColorMultiplier(block, blockAccess, blockPos, renderEnv);
		} else if (useDefaultGrassFoliageColors) {
			return -1;
		} else {
			int i = renderEnv.getMetadata();
			IColorizer customcolors$icolorizer;

			if (block != Blocks.GRASS && block != Blocks.TALL_GRASS && block != Blocks.DOUBLE_PLANT) {
				if (block == Blocks.DOUBLE_PLANT) {
					customcolors$icolorizer = COLORIZER_GRASS;

					if (i >= 8) {
						blockPos = blockPos.down();
					}
				} else if (block == Blocks.LEAVES) {
					customcolors$icolorizer = switch (i & 3) {
						case 0 -> COLORIZER_FOLIAGE;
						case 1 -> COLORIZER_FOLIAGE_PINE;
						case 2 -> COLORIZER_FOLIAGE_BIRCH;
						default -> COLORIZER_FOLIAGE;
					};
				} else if (block == Blocks.LEAVES_2) {
					customcolors$icolorizer = COLORIZER_FOLIAGE;
				} else {
					if (block != Blocks.VINE) {
						return -1;
					}

					customcolors$icolorizer = COLORIZER_FOLIAGE;
				}
			} else {
				customcolors$icolorizer = COLORIZER_GRASS;
			}

			return Config.isSmoothBiomes() && !customcolors$icolorizer.isColorConstant() ? getSmoothColorMultiplier(blockState, blockAccess, blockPos, customcolors$icolorizer, renderEnv.getColorizerBlockPosM()) : customcolors$icolorizer.getColor(iblockstate, blockAccess, blockPos);
		}
	}

	protected static BiomeGenBase getColorBiome(IBlockAccess blockAccess, BlockPos blockPos) {
		BiomeGenBase biomegenbase = blockAccess.getBiomeGenForCoords(blockPos);

		if (biomegenbase == BiomeGenBase.SWAMPLAND && !Config.isSwampColors()) {
			biomegenbase = BiomeGenBase.PLAINS;
		}

		return biomegenbase;
	}

	private static CustomColormap getBlockColormap(IBlockState blockState) {
		if (blockColormaps == null) {
			return null;
		} else if (!(blockState instanceof BlockStateBase blockstatebase)) {
			return null;
		} else {
			int i = blockstatebase.getBlockId();

			if (i >= 0 && i < blockColormaps.length) {
				CustomColormap[] acustomcolormap = blockColormaps[i];

				if (acustomcolormap != null) {
					for (CustomColormap customcolormap : acustomcolormap) {
						if (customcolormap.matchesBlock(blockstatebase)) {
							return customcolormap;
						}
					}

				}
			}
			return null;
		}
	}

	private static int getSmoothColorMultiplier(IBlockState blockState, IBlockAccess blockAccess, BlockPos blockPos, IColorizer colorizer, BlockPosM blockPosM) {
		int i = 0;
		int j = 0;
		int k = 0;
		int l = blockPos.getX();
		int i1 = blockPos.getY();
		int j1 = blockPos.getZ();

		for (int k1 = l - 1; k1 <= l + 1; ++k1) {
			for (int l1 = j1 - 1; l1 <= j1 + 1; ++l1) {
				blockPosM.setXyz(k1, i1, l1);
				int i2 = colorizer.getColor(blockState, blockAccess, blockPosM);
				i += i2 >> 16 & 255;
				j += i2 >> 8 & 255;
				k += i2 & 255;
			}
		}

		int j2 = i / 9;
		int k2 = j / 9;
		int l2 = k / 9;
		return j2 << 16 | k2 << 8 | l2;
	}

	public static int getFluidColor(IBlockAccess blockAccess, IBlockState blockState, BlockPos blockPos, RenderEnv renderEnv) {
		Block block = blockState.getBlock();
		IColorizer customcolors$icolorizer = getBlockColormap(blockState);

		if (customcolors$icolorizer == null && blockState.getBlock().getMaterial() == Material.WATER) {
			customcolors$icolorizer = COLORIZER_WATER;
		}

		return customcolors$icolorizer == null ? block.colorMultiplier(blockAccess, blockPos, 0) : (Config.isSmoothBiomes() && !customcolors$icolorizer.isColorConstant() ? getSmoothColorMultiplier(blockState, blockAccess, blockPos, customcolors$icolorizer, renderEnv.getColorizerBlockPosM()) : customcolors$icolorizer.getColor(blockState, blockAccess, blockPos));
	}

	public static void updatePortalFX(EntityFX fx) {
		if (particlePortalColor >= 0) {
			int i = particlePortalColor;
			int j = i >> 16 & 255;
			int k = i >> 8 & 255;
			int l = i & 255;
			float f = j / 255.0F;
			float f1 = k / 255.0F;
			float f2 = l / 255.0F;
			fx.setRBGColorF(f, f1, f2);
		}
	}

	public static void updateMyceliumFX(EntityFX fx) {
		if (myceliumParticleColors != null) {
			int i = myceliumParticleColors.getColorRandom();
			int j = i >> 16 & 255;
			int k = i >> 8 & 255;
			int l = i & 255;
			float f = j / 255.0F;
			float f1 = k / 255.0F;
			float f2 = l / 255.0F;
			fx.setRBGColorF(f, f1, f2);
		}
	}

	private static int getRedstoneColor(IBlockState blockState) {
		if (redstoneColors == null) {
			return -1;
		} else {
			int i = getRedstoneLevel(blockState, 15);
			return redstoneColors.getColor(i);
		}
	}

	public static void updateReddustFX(EntityFX fx, IBlockAccess blockAccess, double x, double y, double z) {
		if (redstoneColors != null) {
			IBlockState iblockstate = blockAccess.getBlockState(new BlockPos(x, y, z));
			int i = getRedstoneLevel(iblockstate, 15);
			int j = redstoneColors.getColor(i);
			int k = j >> 16 & 255;
			int l = j >> 8 & 255;
			int i1 = j & 255;
			float f = k / 255.0F;
			float f1 = l / 255.0F;
			float f2 = i1 / 255.0F;
			fx.setRBGColorF(f, f1, f2);
		}
	}

	private static int getRedstoneLevel(IBlockState state, int def) {
		Block block = state.getBlock();

		if (!(block instanceof BlockRedstoneWire)) {
			return def;
		} else {
			Object object = state.getValue(BlockRedstoneWire.POWER);

			if (!(object instanceof Integer integer)) {
				return def;
			} else {
				return integer;
			}
		}
	}

	public static float getXpOrbTimer(float timer) {
		if (xpOrbTime <= 0) {
			return timer;
		} else {
			float f = 628.0F / xpOrbTime;
			return timer * f;
		}
	}

	public static int getXpOrbColor(float timer) {
		if (xpOrbColors == null) {
			return -1;
		} else {
			int i = (int) Math.round(((MathHelper.sin(timer) + 1.0F) * (xpOrbColors.getLength() - 1)) / 2.0D);
			return xpOrbColors.getColor(i);
		}
	}

	public static int getDurabilityColor(int dur255) {
		if (durabilityColors == null) {
			return -1;
		} else {
			int i = dur255 * durabilityColors.getLength() / 255;
			return durabilityColors.getColor(i);
		}
	}

	public static void updateWaterFX(EntityFX fx, IBlockAccess blockAccess, double x, double y, double z, RenderEnv renderEnv) {
		if (waterColors != null || blockColormaps != null || particleWaterColor >= 0) {
			BlockPos blockpos = new BlockPos(x, y, z);
			renderEnv.reset(BLOCK_STATE_WATER, blockpos);
			int i = getFluidColor(blockAccess, BLOCK_STATE_WATER, blockpos, renderEnv);
			int j = i >> 16 & 255;
			int k = i >> 8 & 255;
			int l = i & 255;
			float f = j / 255.0F;
			float f1 = k / 255.0F;
			float f2 = l / 255.0F;

			if (particleWaterColor >= 0) {
				int i1 = particleWaterColor >> 16 & 255;
				int j1 = particleWaterColor >> 8 & 255;
				int k1 = particleWaterColor & 255;
				f *= i1 / 255.0F;
				f1 *= j1 / 255.0F;
				f2 *= k1 / 255.0F;
			}

			fx.setRBGColorF(f, f1, f2);
		}
	}

	private static int getLilypadColorMultiplier(IBlockAccess blockAccess, BlockPos blockPos) {
		return lilyPadColor < 0 ? Blocks.WATERLILY.colorMultiplier(blockAccess, blockPos) : lilyPadColor;
	}

	private static Vec3 getFogColorNether(Vec3 col) {
		return fogColorNether == null ? col : fogColorNether;
	}

	private static Vec3 getFogColorEnd(Vec3 col) {
		return fogColorEnd == null ? col : fogColorEnd;
	}

	private static Vec3 getSkyColorEnd(Vec3 col) {
		return skyColorEnd == null ? col : skyColorEnd;
	}

	public static Vec3 getSkyColor(Vec3 skyColor3d, IBlockAccess blockAccess, double x, double y, double z) {
		if (skyColors == null) {
			return skyColor3d;
		} else {
			int i = skyColors.getColorSmooth(blockAccess, x, y, z, 3);
			int j = i >> 16 & 255;
			int k = i >> 8 & 255;
			int l = i & 255;
			float f = j / 255.0F;
			float f1 = k / 255.0F;
			float f2 = l / 255.0F;
			float f3 = (float) skyColor3d.xCoord / 0.5F;
			float f4 = (float) skyColor3d.yCoord / 0.66275F;
			float f5 = (float) skyColor3d.zCoord;
			f = f * f3;
			f1 = f1 * f4;
			f2 = f2 * f5;
			return SKY_COLOR_FADER.getColor(f, f1, f2);
		}
	}

	private static Vec3 getFogColor(Vec3 fogColor3d, IBlockAccess blockAccess, double x, double y, double z) {
		if (fogColors == null) {
			return fogColor3d;
		} else {
			int i = fogColors.getColorSmooth(blockAccess, x, y, z, 3);
			int j = i >> 16 & 255;
			int k = i >> 8 & 255;
			int l = i & 255;
			float f = j / 255.0F;
			float f1 = k / 255.0F;
			float f2 = l / 255.0F;
			float f3 = (float) fogColor3d.xCoord / 0.753F;
			float f4 = (float) fogColor3d.yCoord / 0.8471F;
			float f5 = (float) fogColor3d.zCoord;
			f = f * f3;
			f1 = f1 * f4;
			f2 = f2 * f5;
			return FOG_COLOR_FADER.getColor(f, f1, f2);
		}
	}

	public static Vec3 getUnderwaterColor(IBlockAccess blockAccess, double x, double y, double z) {
		return getUnderFluidColor(blockAccess, x, y, z, underwaterColors, UNDERWATER_COLOR_FADER);
	}

	public static Vec3 getUnderlavaColor(IBlockAccess blockAccess, double x, double y, double z) {
		return getUnderFluidColor(blockAccess, x, y, z, underlavaColors, UNDERLAVA_COLOR_FADER);
	}

	public static Vec3 getUnderFluidColor(IBlockAccess blockAccess, double x, double y, double z, CustomColormap underFluidColors, CustomColorFader underFluidColorFader) {
		if (underFluidColors == null) {
			return null;
		} else {
			int i = underFluidColors.getColorSmooth(blockAccess, x, y, z, 3);
			int j = i >> 16 & 255;
			int k = i >> 8 & 255;
			int l = i & 255;
			float f = j / 255.0F;
			float f1 = k / 255.0F;
			float f2 = l / 255.0F;
			return underFluidColorFader.getColor(f, f1, f2);
		}
	}

	private static int getStemColorMultiplier(Block blockStem, IBlockAccess blockAccess, BlockPos blockPos, RenderEnv renderEnv) {
		CustomColormap customcolormap = stemColors;

		if (blockStem == Blocks.PUMPKIN_STEM && stemPumpkinColors != null) {
			customcolormap = stemPumpkinColors;
		}

		if (blockStem == Blocks.MELON_STEM && stemMelonColors != null) {
			customcolormap = stemMelonColors;
		}

		if (customcolormap == null) {
			return -1;
		} else {
			int i = renderEnv.getMetadata();
			return customcolormap.getColor(i);
		}
	}

	public static boolean updateLightmap(World world, float torchFlickerX, int[] lmColors, boolean nightvision, float partialTicks) {
		if (world == null) {
			return false;
		} else if (lightMapPacks == null) {
			return false;
		} else {
			int i = world.provider.getDimensionId();
			int j = i - lightmapMinDimensionId;

			if (j >= 0 && j < lightMapPacks.length) {
				LightMapPack lightmappack = lightMapPacks[j];
				return lightmappack != null && lightmappack.updateLightmap(world, torchFlickerX, lmColors, nightvision, partialTicks);
			} else {
				return false;
			}
		}
	}

	public static Vec3 getWorldFogColor(Vec3 fogVec, World world, Entity renderViewEntity, float partialTicks) {
		int i = world.provider.getDimensionId();

		switch (i) {
			case -1:
				fogVec = getFogColorNether(fogVec);
				break;

			case 0:
				Minecraft minecraft = Minecraft.getMinecraft();
				fogVec = getFogColor(fogVec, minecraft.world, renderViewEntity.posX, renderViewEntity.posY + 1.0D, renderViewEntity.posZ);
				break;

			case 1:
				fogVec = getFogColorEnd(fogVec);
		}

		return fogVec;
	}

	public static Vec3 getWorldSkyColor(Vec3 skyVec, World world, Entity renderViewEntity, float partialTicks) {
		int i = world.provider.getDimensionId();

		switch (i) {
			case 0:
				Minecraft minecraft = Minecraft.getMinecraft();
				skyVec = getSkyColor(skyVec, minecraft.world, renderViewEntity.posX, renderViewEntity.posY + 1.0D, renderViewEntity.posZ);
				break;

			case 1:
				skyVec = getSkyColorEnd(skyVec);
		}

		return skyVec;
	}

	private static int[] readSpawnEggColors(Properties props, String fileName, String prefix, String logName) {
		IntList list = new IntArrayList();
		Set set = props.keySet();
		int i = 0;

		for (Object o : set) {
			String s = (String) o;
			String s1 = props.getProperty(s);

			if (s.startsWith(prefix)) {
				String s2 = StrUtils.removePrefix(s, prefix);
				int j = EntityUtils.getEntityIdByName(s2);

				if (j < 0) {
					warn("Invalid spawn egg name: " + s);
				} else {
					int k = parseColor(s1);

					if (k < 0) {
						warn("Invalid spawn egg color: " + s + " = " + s1);
					} else {
						while (list.size() <= j) {
							list.add(-1);
						}

						list.set(j, k);
						++i;
					}
				}
			}
		}

		if (i <= 0) {
			return null;
		} else {
			dbg(logName + " colors: " + i);
			int[] aint = new int[list.size()];

			for (int l = 0; l < aint.length; ++l) {
				aint[l] = list.getInt(l);
			}

			return aint;
		}
	}

	private static int getSpawnEggColor(ItemMonsterPlacer item, ItemStack itemStack, int layer, int color) {
		int i = itemStack.getMetadata();
		int[] aint = layer == 0 ? spawnEggPrimaryColors : spawnEggSecondaryColors;

		if (aint == null) {
			return color;
		} else if (i >= 0 && i < aint.length) {
			int j = aint[i];
			return j < 0 ? color : j;
		} else {
			return color;
		}
	}

	public static int getColorFromItemStack(ItemStack itemStack, int layer, int color) {
		if (itemStack == null) {
			return color;
		} else {
			Item item = itemStack.getItem();
			return item == null ? color : (item instanceof ItemMonsterPlacer itemMonsterPlacer ? getSpawnEggColor(itemMonsterPlacer, itemStack, layer, color) : color);
		}
	}

	private static float[][] readDyeColors(Properties props, String fileName, String prefix, String logName) {
		DyeColor[] aenumdyecolor = DyeColor.values();
		Map<String, DyeColor> map = new HashMap<>();

		for (DyeColor enumdyecolor : aenumdyecolor) {
			map.put(enumdyecolor.getName(), enumdyecolor);
		}

		float[][] afloat1 = new float[aenumdyecolor.length][];
		int k = 0;

		for (Object o : props.keySet()) {
			String s = (String) o;
			String s1 = props.getProperty(s);

			if (s.startsWith(prefix)) {
				String s2 = StrUtils.removePrefix(s, prefix);

				if (s2.equals("lightBlue")) {
					s2 = "light_blue";
				}

				DyeColor enumdyecolor1 = map.get(s2);
				int j = parseColor(s1);

				if (enumdyecolor1 != null && j >= 0) {
					float[] afloat = new float[]{(j >> 16 & 255) / 255.0F, (j >> 8 & 255) / 255.0F, (j & 255) / 255.0F};
					afloat1[enumdyecolor1.ordinal()] = afloat;
					++k;
				} else {
					warn("Invalid color: " + s + " = " + s1);
				}
			}
		}

		if (k <= 0) {
			return null;
		} else {
			dbg(logName + " colors: " + k);
			return afloat1;
		}
	}

	private static float[] getDyeColors(DyeColor dye, float[][] dyeColors, float[] colors) {
		if (dyeColors == null) {
			return colors;
		} else if (dye == null) {
			return colors;
		} else {
			float[] afloat = dyeColors[dye.ordinal()];
			return afloat == null ? colors : afloat;
		}
	}

	public static float[] getWolfCollarColors(DyeColor dye, float[] colors) {
		return getDyeColors(dye, wolfCollarColors, colors);
	}

	public static float[] getSheepColors(DyeColor dye, float[] colors) {
		return getDyeColors(dye, sheepColors, colors);
	}

	private static int[] readTextColors(Properties props, String fileName, String prefix, String logName) {
		int[] aint = new int[32];
		Arrays.fill(aint, -1);
		int i = 0;

		for (Object o : props.keySet()) {
			String s = (String) o;
			String s1 = props.getProperty(s);

			if (s.startsWith(prefix)) {
				String s2 = StrUtils.removePrefix(s, prefix);
				int j = Config.parseInt(s2, -1);
				int k = parseColor(s1);

				if (j >= 0 && j < aint.length && k >= 0) {
					aint[j] = k;
					++i;
				} else {
					warn("Invalid color: " + s + " = " + s1);
				}
			}
		}

		if (i <= 0) {
			return null;
		} else {
			dbg(logName + " colors: " + i);
			return aint;
		}
	}

	public static int getTextColor(int index, int color) {
		if (textColors == null) {
			return color;
		} else if (index >= 0 && index < textColors.length) {
			int i = textColors[index];
			return i < 0 ? color : i;
		} else {
			return color;
		}
	}

	private static int[] readMapColors(Properties props, String fileName, String prefix, String logName) {
		int[] aint = new int[MapColor.MAP_COLOR_ARRAY.length];
		Arrays.fill(aint, -1);
		int i = 0;

		for (Object o : props.keySet()) {
			String s = (String) o;
			String s1 = props.getProperty(s);

			if (s.startsWith(prefix)) {
				String s2 = StrUtils.removePrefix(s, prefix);
				int j = getMapColorIndex(s2);
				int k = parseColor(s1);

				if (j >= 0 && j < aint.length && k >= 0) {
					aint[j] = k;
					++i;
				} else {
					warn("Invalid color: " + s + " = " + s1);
				}
			}
		}

		if (i <= 0) {
			return null;
		} else {
			dbg(logName + " colors: " + i);
			return aint;
		}
	}

	private static int[] readPotionColors(Properties props, String fileName, String prefix, String logName) {
		int[] aint = new int[Potion.POTION_TYPES.length];
		Arrays.fill(aint, -1);
		int i = 0;

		for (Object o : props.keySet()) {
			String s = (String) o;
			String s1 = props.getProperty(s);

			if (s.startsWith(prefix)) {
				int j = getPotionId(s);
				int k = parseColor(s1);

				if (j >= 0 && j < aint.length && k >= 0) {
					aint[j] = k;
					++i;
				} else {
					warn("Invalid color: " + s + " = " + s1);
				}
			}
		}

		if (i <= 0) {
			return null;
		} else {
			dbg(logName + " colors: " + i);
			return aint;
		}
	}

	private static int getPotionId(String name) {
		if (name.equals("potion.water")) {
			return 0;
		} else {
			Potion[] apotion = Potion.POTION_TYPES;

			for (Potion potion : apotion) {
				if (potion != null && potion.getName().equals(name)) {
					return potion.getId();
				}
			}

			return -1;
		}
	}

	public static int getPotionColor(int potionId, int color) {
		if (potionColors == null) {
			return color;
		} else if (potionId >= 0 && potionId < potionColors.length) {
			int i = potionColors[potionId];
			return i < 0 ? color : i;
		} else {
			return color;
		}
	}

	private static int getMapColorIndex(String name) {
		return name == null ? -1 : (name.equals("air") ? MapColor.AIR_COLOR.colorIndex : (name.equals("grass") ? MapColor.GRASS_COLOR.colorIndex : (name.equals("sand") ? MapColor.SAND_COLOR.colorIndex : (name.equals("cloth") ? MapColor.CLOTH_COLOR.colorIndex : (name.equals("tnt") ? MapColor.TNT_COLOR.colorIndex : (name.equals("ice") ? MapColor.ICE_COLOR.colorIndex : (name.equals("iron") ? MapColor.IRON_COLOR.colorIndex : (name.equals("foliage") ? MapColor.FOLIAGE_COLOR.colorIndex : (name.equals("clay") ? MapColor.CLAY_COLOR.colorIndex : (name.equals("dirt") ? MapColor.DIRT_COLOR.colorIndex : (name.equals("stone") ? MapColor.STONE_COLOR.colorIndex : (name.equals("water") ? MapColor.WATER_COLOR.colorIndex : (name.equals("wood") ? MapColor.WOOD_COLOR.colorIndex : (name.equals("quartz") ? MapColor.QUARTZ_COLOR.colorIndex : (name.equals("gold") ? MapColor.GOLD_COLOR.colorIndex : (name.equals("diamond") ? MapColor.DIAMOND_COLOR.colorIndex : (name.equals("lapis") ? MapColor.LAPIS_COLOR.colorIndex : (name.equals("emerald") ? MapColor.EMERALD_COLOR.colorIndex : (name.equals("podzol") ? MapColor.OBSIDIAN_COLOR.colorIndex : (name.equals("netherrack") ? MapColor.NETHERRACK_COLOR.colorIndex : (!name.equals("snow") && !name.equals("white") ? (!name.equals("adobe") && !name.equals("orange") ? (name.equals("magenta") ? MapColor.MAGENTA_COLOR.colorIndex : (!name.equals("light_blue") && !name.equals("lightBlue") ? (name.equals("yellow") ? MapColor.YELLOW_COLOR.colorIndex : (name.equals("lime") ? MapColor.LIME_COLOR.colorIndex : (name.equals("pink") ? MapColor.PINK_COLOR.colorIndex : (name.equals("gray") ? MapColor.GRAY_COLOR.colorIndex : (name.equals("silver") ? MapColor.SILVER_COLOR.colorIndex : (name.equals("cyan") ? MapColor.CYAN_COLOR.colorIndex : (name.equals("purple") ? MapColor.PURPLE_COLOR.colorIndex : (name.equals("blue") ? MapColor.BLUE_COLOR.colorIndex : (name.equals("brown") ? MapColor.BROWN_COLOR.colorIndex : (name.equals("green") ? MapColor.GREEN_COLOR.colorIndex : (name.equals("red") ? MapColor.RED_COLOR.colorIndex : (name.equals("black") ? MapColor.BLACK_COLOR.colorIndex : -1)))))))))))) : MapColor.LIGHT_BLUE_COLOR.colorIndex)) : MapColor.ADOBE_COLOR.colorIndex) : MapColor.SNOW_COLOR.colorIndex)))))))))))))))))))));
	}

	private static int[] getMapColors() {
		MapColor[] amapcolor = MapColor.MAP_COLOR_ARRAY;
		int[] aint = new int[amapcolor.length];
		Arrays.fill(aint, -1);

		for (int i = 0; i < amapcolor.length && i < aint.length; ++i) {
			MapColor mapcolor = amapcolor[i];

			if (mapcolor != null) {
				aint[i] = mapcolor.colorValue;
			}
		}

		return aint;
	}

	private static void setMapColors(int[] colors) {
		if (colors != null) {
			MapColor[] amapcolor = MapColor.MAP_COLOR_ARRAY;
			boolean flag = false;

			for (int i = 0; i < amapcolor.length && i < colors.length; ++i) {
				MapColor mapcolor = amapcolor[i];

				if (mapcolor != null) {
					int j = colors[i];

					if (j >= 0 && mapcolor.colorValue != j) {
						mapcolor.colorValue = j;
						flag = true;
					}
				}
			}

			if (flag) {
				Minecraft.getMinecraft().getTextureManager().reloadBannerTextures();
			}
		}
	}

	private static void dbg(String str) {
		Log.info("CustomColors: " + str);
	}

	private static void warn(String str) {
		Log.error("CustomColors: " + str);
	}

	public static int getExpBarTextColor(int color) {
		return expBarTextColor < 0 ? color : expBarTextColor;
	}

	public static int getBossTextColor(int color) {
		return bossTextColor < 0 ? color : bossTextColor;
	}

	public static int getSignTextColor(int color) {
		return signTextColor < 0 ? color : signTextColor;
	}

	public interface IColorizer {
		int getColor(IBlockState var1, IBlockAccess var2, BlockPos var3);

		boolean isColorConstant();
	}
}
