package net.optifine;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.item.DyeColor;
import net.minecraft.src.Config;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.optifine.config.*;
import net.optifine.reflect.Reflector;
import net.optifine.util.ArrayUtils;
import net.minecraft.util.MathHelper;

import java.util.Properties;

public class RandomEntityRule {
	private final String pathProps;
	private final ResourceLocation baseResLoc;
	private final int index;
	private final int[] textures;
	private final BiomeGenBase[] biomes;
	public int[] sumWeights = null;
	public int sumAllWeights = 1;
	private ResourceLocation[] resourceLocations = null;
	private int[] weights;
	private RangeListInt heights;
	private RangeListInt healthRange = null;
	private boolean healthPercent = false;
	private NbtTagValue nbtName = null;
	private VillagerProfession[] professions = null;
	private DyeColor[] collarColors = null;
	private Boolean baby = null;
	private RangeListInt moonPhases = null;
	private RangeListInt dayTimes = null;
	private Weather[] weatherList = null;

	public RandomEntityRule(Properties props, String pathProps, ResourceLocation baseResLoc, int index, String valTextures, ConnectedParser cp) {
		this.pathProps = pathProps;
		this.baseResLoc = baseResLoc;
		this.index = index;
		this.textures = cp.parseIntList(valTextures);
		this.weights = cp.parseIntList(props.getProperty("weights." + index));
		this.biomes = cp.parseBiomes(props.getProperty("biomes." + index));
		this.heights = cp.parseRangeListInt(props.getProperty("heights." + index));

		if (this.heights == null) {
			this.heights = this.parseMinMaxHeight(props, index);
		}

		String s = props.getProperty("health." + index);

		if (s != null) {
			this.healthPercent = s.contains("%");
			s = s.replace("%", "");
			this.healthRange = cp.parseRangeListInt(s);
		}

		this.nbtName = cp.parseNbtTagValue("name", props.getProperty("name." + index));
		this.professions = cp.parseProfessions(props.getProperty("professions." + index));
		this.collarColors = cp.parseDyeColors(props.getProperty("collarColors." + index), "collar color", ConnectedParser.DYE_COLORS_INVALID);
		this.baby = cp.parseBooleanObject(props.getProperty("baby." + index));
		this.moonPhases = cp.parseRangeListInt(props.getProperty("moonPhase." + index));
		this.dayTimes = cp.parseRangeListInt(props.getProperty("dayTime." + index));
		this.weatherList = cp.parseWeather(props.getProperty("weather." + index), "weather." + index, null);
	}

	private RangeListInt parseMinMaxHeight(Properties props, int index) {
		String s = props.getProperty("minHeight." + index);
		String s1 = props.getProperty("maxHeight." + index);

		if (s == null && s1 == null) {
			return null;
		} else {
			int i = 0;

			if (s != null) {
				i = Config.parseInt(s, -1);

				if (i < 0) {
					Log.error("Invalid minHeight: " + s);
					return null;
				}
			}

			int j = 256;

			if (s1 != null) {
				j = Config.parseInt(s1, -1);

				if (j < 0) {
					Log.error("Invalid maxHeight: " + s1);
					return null;
				}
			}

			if (j < 0) {
				Log.error("Invalid minHeight, maxHeight: " + s + ", " + s1);
				return null;
			} else {
				RangeListInt rangelistint = new RangeListInt();
				rangelistint.addRange(new RangeInt(i, j));
				return rangelistint;
			}
		}
	}

	public boolean isValid(String path) {
		if (this.textures != null && this.textures.length != 0) {
			if (this.resourceLocations != null) {
				return true;
			} else {
				this.resourceLocations = new ResourceLocation[this.textures.length];
				boolean flag = this.pathProps.startsWith("mcpatcher/mob/");
				ResourceLocation resourcelocation = RandomEntities.getLocationRandom(this.baseResLoc, flag);

				if (resourcelocation == null) {
					Log.error("Invalid path: " + this.baseResLoc.getResourcePath());
					return false;
				} else {
					for (int i = 0; i < this.resourceLocations.length; ++i) {
						int j = this.textures[i];

						if (j <= 1) {
							this.resourceLocations[i] = this.baseResLoc;
						} else {
							ResourceLocation resourcelocation1 = RandomEntities.getLocationIndexed(resourcelocation, j);

							if (resourcelocation1 == null) {
								Log.error("Invalid path: " + this.baseResLoc.getResourcePath());
								return false;
							}

							if (!Config.hasResource(resourcelocation1)) {
								Log.error("Texture not found: " + resourcelocation1.getResourcePath());
								return false;
							}

							this.resourceLocations[i] = resourcelocation1;
						}
					}

					if (this.weights != null) {
						if (this.weights.length > this.resourceLocations.length) {
							Log.error("More weights defined than skins, trimming weights: " + path);
							int[] aint = new int[this.resourceLocations.length];
							System.arraycopy(this.weights, 0, aint, 0, aint.length);
							this.weights = aint;
						}

						if (this.weights.length < this.resourceLocations.length) {
							Log.error("Less weights defined than skins, expanding weights: " + path);
							int[] aint1 = new int[this.resourceLocations.length];
							System.arraycopy(this.weights, 0, aint1, 0, this.weights.length);
							int l = MathHelper.getAverage(this.weights);

							for (int j1 = this.weights.length; j1 < aint1.length; ++j1) {
								aint1[j1] = l;
							}

							this.weights = aint1;
						}

						this.sumWeights = new int[this.weights.length];
						int k = 0;

						for (int i1 = 0; i1 < this.weights.length; ++i1) {
							if (this.weights[i1] < 0) {
								Log.error("Invalid weight: " + this.weights[i1]);
								return false;
							}

							k += this.weights[i1];
							this.sumWeights[i1] = k;
						}

						this.sumAllWeights = k;

						if (this.sumAllWeights <= 0) {
							Log.error("Invalid sum of all weights: " + k);
							this.sumAllWeights = 1;
						}
					}

					if (this.professions == ConnectedParser.PROFESSIONS_INVALID) {
						Log.error("Invalid professions or careers: " + path);
						return false;
					} else if (this.collarColors == ConnectedParser.DYE_COLORS_INVALID) {
						Log.error("Invalid collar colors: " + path);
						return false;
					} else {
						return true;
					}
				}
			}
		} else {
			Log.error("Invalid skins for rule: " + this.index);
			return false;
		}
	}

	public boolean matches(IRandomEntity randomEntity) {
		if (this.biomes != null && !Matches.biome(randomEntity.getSpawnBiome(), this.biomes)) {
			return false;
		} else {
			if (this.heights != null) {
				BlockPos blockpos = randomEntity.getSpawnPosition();

				if (blockpos != null && !this.heights.isInRange(blockpos.getY())) {
					return false;
				}
			}

			if (this.healthRange != null) {
				int i1 = randomEntity.getHealth();

				if (this.healthPercent) {
					int i = randomEntity.getMaxHealth();

					if (i > 0) {
						i1 = (int) ((double) (i1 * 100) / i);
					}
				}

				if (!this.healthRange.isInRange(i1)) {
					return false;
				}
			}

			if (this.nbtName != null) {
				String s = randomEntity.getName();

				if (!this.nbtName.matchesValue(s)) {
					return false;
				}
			}

			if (this.professions != null && randomEntity instanceof RandomEntity randomentity) {
				Entity entity = randomentity.getEntity();

				if (entity instanceof EntityVillager entityvillager) {
					int j = entityvillager.getProfession();
					int k = Reflector.getFieldValueInt(entityvillager, Reflector.EntityVillager_careerId, -1);

					if (j < 0 || k < 0) {
						return false;
					}

					boolean flag = false;

					for (VillagerProfession villagerprofession : this.professions) {
						if (villagerprofession.matches(j, k)) {
							flag = true;
							break;
						}
					}

					if (!flag) {
						return false;
					}
				}
			}

			if (this.collarColors != null && randomEntity instanceof RandomEntity randomentity1) {
				Entity entity1 = randomentity1.getEntity();

				if (entity1 instanceof EntityWolf entitywolf) {

					if (!entitywolf.isTamed()) {
						return false;
					}

					DyeColor enumdyecolor = entitywolf.getCollarColor();

					if (!Config.equalsOne(enumdyecolor, this.collarColors)) {
						return false;
					}
				}
			}

			if (this.baby != null && randomEntity instanceof RandomEntity randomentity2) {
				Entity entity2 = randomentity2.getEntity();

				if (entity2 instanceof EntityLiving entityliving) {

					if (entityliving.isChild() != this.baby) {
						return false;
					}
				}
			}

			if (this.moonPhases != null) {
				World world = Config.getMinecraft().world;

				if (world != null) {
					int j1 = world.getMoonPhase();

					if (!this.moonPhases.isInRange(j1)) {
						return false;
					}
				}
			}

			if (this.dayTimes != null) {
				World world1 = Config.getMinecraft().world;

				if (world1 != null) {
					int k1 = (int) world1.getWorldInfo().getWorldTime();

					if (!this.dayTimes.isInRange(k1)) {
						return false;
					}
				}
			}

			if (this.weatherList != null) {
				World world2 = Config.getMinecraft().world;

				if (world2 != null) {
					Weather weather = Weather.getWeather(world2, 0.0F);

					return ArrayUtils.contains(this.weatherList, weather);
				}
			}

			return true;
		}
	}

	public ResourceLocation getTextureLocation(ResourceLocation loc, int randomId) {
		if (this.resourceLocations != null && this.resourceLocations.length != 0) {
			int i = 0;

			if (this.weights == null) {
				i = randomId % this.resourceLocations.length;
			} else {
				int j = randomId % this.sumAllWeights;

				for (int k = 0; k < this.sumWeights.length; ++k) {
					if (this.sumWeights[k] > j) {
						i = k;
						break;
					}
				}
			}

			return this.resourceLocations[i];
		} else {
			return loc;
		}
	}
}
