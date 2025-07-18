package net.optifine;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.src.Config;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorRaw;
import net.optifine.util.IntegratedServerUtils;
import net.optifine.util.PropertiesOrdered;
import net.optifine.util.ResUtils;
import net.optifine.util.StrUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class RandomEntities {
	public static final String SUFFIX_PNG = ".png";
	public static final String SUFFIX_PROPERTIES = ".properties";
	public static final String PREFIX_TEXTURES_ENTITY = "textures/entity/";
	public static final String PREFIX_TEXTURES_PAINTING = "textures/painting/";
	public static final String PREFIX_TEXTURES = "textures/";
	public static final String PREFIX_OPTIFINE_RANDOM = "optifine/random/";
	public static final String PREFIX_MCPATCHER_MOB = "mcpatcher/mob/";
	private static final Map<String, RandomEntityProperties> MAP_PROPERTIES = new HashMap<>();
	private static final RandomEntity RANDOM_ENTITY = new RandomEntity();
	private static final RandomTileEntity RANDOM_TILE_ENTITY = new RandomTileEntity();
	private static final String[] DEPENDANT_SUFFIXES = new String[]{"_armor", "_eyes", "_exploding", "_shooting", "_fur", "_eyes", "_invulnerable", "_angry", "_tame", "_collar"};
	private static final String PREFIX_DYNAMIC_TEXTURE_HORSE = "horse/";
	private static final String[] HORSE_TEXTURES = (String[]) ReflectorRaw.getFieldValue(null, EntityHorse.class, String[].class, 2);
	private static final String[] HORSE_TEXTURES_ABBR = (String[]) ReflectorRaw.getFieldValue(null, EntityHorse.class, String[].class, 3);
	private static boolean active = false;
	private static RenderGlobal renderGlobal;
	private static TileEntityRendererDispatcher tileEntityRendererDispatcher;
	private static boolean working = false;

	public static void entityLoaded(Entity entity, World world) {
		if (world != null) {
			DataWatcher datawatcher = entity.getDataWatcher();
			datawatcher.spawnPosition = entity.getPosition();
			datawatcher.spawnBiome = world.getBiomeGenForCoords(datawatcher.spawnPosition);
			UUID uuid = entity.getUniqueID();

			if (entity instanceof EntityVillager entityVillager) {
				updateEntityVillager(uuid, entityVillager);
			}
		}
	}

	public static void entityUnloaded(Entity entity, World world) {
	}

	private static void updateEntityVillager(UUID uuid, EntityVillager ev) {
		Entity entity = IntegratedServerUtils.getEntity(uuid);

		if (entity instanceof EntityVillager entityvillager) {
			int i = entityvillager.getProfession();
			ev.setProfession(i);
			int j = Reflector.getFieldValueInt(entityvillager, Reflector.EntityVillager_careerId, 0);
			System.out.println(j);
			Reflector.setFieldValueInt(ev, Reflector.EntityVillager_careerId, j);
			int k = Reflector.getFieldValueInt(entityvillager, Reflector.EntityVillager_careerLevel, 0);
			Reflector.setFieldValueInt(ev, Reflector.EntityVillager_careerLevel, k);
			System.out.println(k);
		}
	}

	public static void worldChanged(World oldWorld, World newWorld) {
		if (newWorld != null) {
			List list = newWorld.getLoadedEntityList();

			for (Object o : list) {
				Entity entity = (Entity) o;
				entityLoaded(entity, newWorld);
			}
		}

		RANDOM_ENTITY.setEntity(null);
		RANDOM_TILE_ENTITY.setTileEntity(null);
	}

	public static ResourceLocation getTextureLocation(ResourceLocation loc) {
		if (!active) {
			return loc;
		} else if (working) {
			return loc;
		} else {
			ResourceLocation name;

			try {
				working = true;
				IRandomEntity irandomentity = getRandomEntityRendered();

				if (irandomentity != null) {
					String s = loc.getResourcePath();

					if (s.startsWith("horse/")) {
						s = getHorseTexturePath(s, "horse/".length());
					}

					if (!s.startsWith("textures/entity/") && !s.startsWith("textures/painting/")) {
						return loc;
					}

					RandomEntityProperties randomentityproperties = MAP_PROPERTIES.get(s);

					if (randomentityproperties == null) {
						return loc;
					}

					return randomentityproperties.getTextureLocation(loc, irandomentity);
				}

				name = loc;
			} finally {
				working = false;
			}

			return name;
		}
	}

	private static String getHorseTexturePath(String path, int pos) {
		if (HORSE_TEXTURES != null && HORSE_TEXTURES_ABBR != null) {
			for (int i = 0; i < HORSE_TEXTURES_ABBR.length; ++i) {
				String s = HORSE_TEXTURES_ABBR[i];

				if (path.startsWith(s, pos)) {
					return HORSE_TEXTURES[i];
				}
			}

		}
		return path;
	}

	private static IRandomEntity getRandomEntityRendered() {
		if (renderGlobal.renderedEntity != null) {
			RANDOM_ENTITY.setEntity(renderGlobal.renderedEntity);
			return RANDOM_ENTITY;
		} else {
			if (tileEntityRendererDispatcher.tileEntityRendered != null) {
				TileEntity tileentity = tileEntityRendererDispatcher.tileEntityRendered;

				if (tileentity.getWorld() != null) {
					RANDOM_TILE_ENTITY.setTileEntity(tileentity);
					return RANDOM_TILE_ENTITY;
				}
			}

			return null;
		}
	}

	private static RandomEntityProperties makeProperties(ResourceLocation loc, boolean mcpatcher) {
		String s = loc.getResourcePath();
		ResourceLocation resourcelocation = getLocationProperties(loc, mcpatcher);

		if (resourcelocation != null) {
			RandomEntityProperties randomentityproperties = parseProperties(resourcelocation, loc);

			if (randomentityproperties != null) {
				return randomentityproperties;
			}
		}

		ResourceLocation[] aresourcelocation = getLocationsVariants(loc, mcpatcher);
		return aresourcelocation == null ? null : new RandomEntityProperties(s, aresourcelocation);
	}

	private static RandomEntityProperties parseProperties(ResourceLocation propLoc, ResourceLocation resLoc) {
		try {
			String s = propLoc.getResourcePath();
			dbg(resLoc.getResourcePath() + ", properties: " + s);
			InputStream inputstream = Config.getResourceStream(propLoc);

			if (inputstream == null) {
				warn("Properties not found: " + s);
				return null;
			} else {
				Properties properties = new PropertiesOrdered();
				properties.load(inputstream);
				inputstream.close();
				RandomEntityProperties randomentityproperties = new RandomEntityProperties(properties, s, resLoc);
				return !randomentityproperties.isValid(s) ? null : randomentityproperties;
			}
		} catch (FileNotFoundException exception) {
			warn("File not found: " + resLoc.getResourcePath());
			return null;
		} catch (IOException exception) {
			exception.printStackTrace();
			return null;
		}
	}

	private static ResourceLocation getLocationProperties(ResourceLocation loc, boolean mcpatcher) {
		ResourceLocation resourcelocation = getLocationRandom(loc, mcpatcher);

		if (resourcelocation == null) {
			return null;
		} else {
			String s = resourcelocation.getResourceDomain();
			String s1 = resourcelocation.getResourcePath();
			String s2 = StrUtils.removeSuffix(s1, ".png");
			String s3 = s2 + ".properties";
			ResourceLocation resourcelocation1 = new ResourceLocation(s, s3);

			if (Config.hasResource(resourcelocation1)) {
				return resourcelocation1;
			} else {
				String s4 = getParentTexturePath(s2);

				if (s4 == null) {
					return null;
				} else {
					ResourceLocation resourcelocation2 = new ResourceLocation(s, s4 + ".properties");
					return Config.hasResource(resourcelocation2) ? resourcelocation2 : null;
				}
			}
		}
	}

	protected static ResourceLocation getLocationRandom(ResourceLocation loc, boolean mcpatcher) {
		String s = loc.getResourceDomain();
		String s1 = loc.getResourcePath();
		String s2 = "textures/";
		String s3 = "optifine/random/";

		if (mcpatcher) {
			s2 = "textures/entity/";
			s3 = "mcpatcher/mob/";
		}

		if (!s1.startsWith(s2)) {
			return null;
		} else {
			String s4 = StrUtils.replacePrefix(s1, s2, s3);
			return new ResourceLocation(s, s4);
		}
	}

	private static String getPathBase(String pathRandom) {
		return pathRandom.startsWith("optifine/random/") ? StrUtils.replacePrefix(pathRandom, "optifine/random/", "textures/") : (pathRandom.startsWith("mcpatcher/mob/") ? StrUtils.replacePrefix(pathRandom, "mcpatcher/mob/", "textures/entity/") : null);
	}

	protected static ResourceLocation getLocationIndexed(ResourceLocation loc, int index) {
		if (loc == null) {
			return null;
		} else {
			String s = loc.getResourcePath();
			int i = s.lastIndexOf(46);

			if (i < 0) {
				return null;
			} else {
				String s1 = s.substring(0, i);
				String s2 = s.substring(i);
				String s3 = s1 + index + s2;
				return new ResourceLocation(loc.getResourceDomain(), s3);
			}
		}
	}

	private static String getParentTexturePath(String path) {
		for (String s : DEPENDANT_SUFFIXES) {
			if (path.endsWith(s)) {
				return StrUtils.removeSuffix(path, s);
			}
		}

		return null;
	}

	private static ResourceLocation[] getLocationsVariants(ResourceLocation loc, boolean mcpatcher) {
		List list = new ArrayList<>();
		list.add(loc);
		ResourceLocation resourcelocation = getLocationRandom(loc, mcpatcher);

		if (resourcelocation == null) {
			return null;
		} else {
			for (int i = 1; i < list.size() + 10; ++i) {
				int j = i + 1;
				ResourceLocation resourcelocation1 = getLocationIndexed(resourcelocation, j);

				if (Config.hasResource(resourcelocation1)) {
					list.add(resourcelocation1);
				}
			}

			if (list.size() <= 1) {
				return null;
			} else {
				ResourceLocation[] aresourcelocation = (ResourceLocation[]) list.toArray(new ResourceLocation[0]);
				dbg(loc.getResourcePath() + ", variants: " + aresourcelocation.length);
				return aresourcelocation;
			}
		}
	}

	public static void update() {
		MAP_PROPERTIES.clear();
		active = false;

		if (Config.isRandomEntities()) {
			initialize();
		}
	}

	private static void initialize() {
		renderGlobal = Config.getRenderGlobal();
		tileEntityRendererDispatcher = TileEntityRendererDispatcher.INSTANCE;
		String[] astring = new String[]{"optifine/random/", "mcpatcher/mob/"};
		String[] astring1 = new String[]{".png", ".properties"};
		String[] astring2 = ResUtils.collectFiles(astring, astring1);
		Set set = new HashSet();

		for (String string : astring2) {
			String s = string;
			s = StrUtils.removeSuffix(s, astring1);
			s = StrUtils.trimTrailing(s, "0123456789");
			s = s + ".png";
			String s1 = getPathBase(s);

			if (!set.contains(s1)) {
				set.add(s1);
				ResourceLocation resourcelocation = new ResourceLocation(s1);

				if (Config.hasResource(resourcelocation)) {
					RandomEntityProperties randomentityproperties = MAP_PROPERTIES.get(s1);

					if (randomentityproperties == null) {
						randomentityproperties = makeProperties(resourcelocation, false);

						if (randomentityproperties == null) {
							randomentityproperties = makeProperties(resourcelocation, true);
						}

						if (randomentityproperties != null) {
							MAP_PROPERTIES.put(s1, randomentityproperties);
						}
					}
				}
			}
		}

		active = !MAP_PROPERTIES.isEmpty();
	}

	public static void dbg(String str) {
		Log.info("RandomEntities: " + str);
	}

	public static void warn(String str) {
		Log.error("RandomEntities: " + str);
	}
}
