package net.optifine;

import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiHopper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.DyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.*;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.IWorldNameable;
import net.minecraft.world.biome.BiomeGenBase;
import net.optifine.config.*;
import net.optifine.util.StrUtils;
import net.optifine.util.TextureUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CustomGuiProperties {
	private static final EnumVariant[] VARIANTS_HORSE = new EnumVariant[]{EnumVariant.HORSE, EnumVariant.DONKEY, EnumVariant.MULE, EnumVariant.LLAMA};
	private static final EnumVariant[] VARIANTS_DISPENSER = new EnumVariant[]{EnumVariant.DISPENSER, EnumVariant.DROPPER};
	private static final EnumVariant[] VARIANTS_INVALID = new EnumVariant[0];
	private static final DyeColor[] COLORS_INVALID = new DyeColor[0];
	private static final ResourceLocation ANVIL_GUI_TEXTURE = new ResourceLocation("textures/gui/container/anvil.png");
	private static final ResourceLocation BEACON_GUI_TEXTURE = new ResourceLocation("textures/gui/container/beacon.png");
	private static final ResourceLocation BREWING_STAND_GUI_TEXTURE = new ResourceLocation("textures/gui/container/brewing_stand.png");
	private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
	private static final ResourceLocation CRAFTING_TABLE_GUI_TEXTURE = new ResourceLocation("textures/gui/container/crafting_table.png");
	private static final ResourceLocation HORSE_GUI_TEXTURE = new ResourceLocation("textures/gui/container/horse.png");
	private static final ResourceLocation DISPENSER_GUI_TEXTURE = new ResourceLocation("textures/gui/container/dispenser.png");
	private static final ResourceLocation ENCHANTMENT_TABLE_GUI_TEXTURE = new ResourceLocation("textures/gui/container/enchanting_table.png");
	private static final ResourceLocation FURNACE_GUI_TEXTURE = new ResourceLocation("textures/gui/container/furnace.png");
	private static final ResourceLocation HOPPER_GUI_TEXTURE = new ResourceLocation("textures/gui/container/hopper.png");
	private static final ResourceLocation INVENTORY_GUI_TEXTURE = new ResourceLocation("textures/gui/container/inventory.png");
	private static final ResourceLocation SHULKER_BOX_GUI_TEXTURE = new ResourceLocation("textures/gui/container/shulker_box.png");
	private static final ResourceLocation VILLAGER_GUI_TEXTURE = new ResourceLocation("textures/gui/container/villager.png");
	private final String fileName;
	private final String basePath;
	private final EnumContainer container;
	private final Map<ResourceLocation, ResourceLocation> textureLocations;
	private final NbtTagValue nbtName;
	private final BiomeGenBase[] biomes;
	private final RangeListInt heights;
	private final Boolean large;
	private final Boolean trapped;
	private final Boolean christmas;
	private final Boolean ender;
	private final RangeListInt levels;
	private final VillagerProfession[] professions;
	private final EnumVariant[] variants;
	private final DyeColor[] colors;

	public CustomGuiProperties(Properties props, String path) {
		ConnectedParser connectedparser = new ConnectedParser("CustomGuis");
		this.fileName = connectedparser.parseName(path);
		this.basePath = connectedparser.parseBasePath(path);
		this.container = (EnumContainer) connectedparser.parseEnum(props.getProperty("container"), EnumContainer.values(), "container");
		this.textureLocations = parseTextureLocations(props, "texture", this.container, "textures/gui/", this.basePath);
		this.nbtName = connectedparser.parseNbtTagValue("name", props.getProperty("name"));
		this.biomes = connectedparser.parseBiomes(props.getProperty("biomes"));
		this.heights = connectedparser.parseRangeListInt(props.getProperty("heights"));
		this.large = connectedparser.parseBooleanObject(props.getProperty("large"));
		this.trapped = connectedparser.parseBooleanObject(props.getProperty("trapped"));
		this.christmas = connectedparser.parseBooleanObject(props.getProperty("christmas"));
		this.ender = connectedparser.parseBooleanObject(props.getProperty("ender"));
		this.levels = connectedparser.parseRangeListInt(props.getProperty("levels"));
		this.professions = connectedparser.parseProfessions(props.getProperty("professions"));
		EnumVariant[] acustomguiproperties$enumvariant = getContainerVariants(this.container);
		this.variants = (EnumVariant[]) connectedparser.parseEnums(props.getProperty("variants"), acustomguiproperties$enumvariant, "variants", VARIANTS_INVALID);
		this.colors = parseEnumDyeColors(props.getProperty("colors"));
	}

	private static EnumVariant[] getContainerVariants(EnumContainer cont) {
		return cont == EnumContainer.HORSE ? VARIANTS_HORSE : (cont == EnumContainer.DISPENSER ? VARIANTS_DISPENSER : new EnumVariant[0]);
	}

	private static DyeColor[] parseEnumDyeColors(String str) {
		if (str == null) {
			return null;
		} else {
			str = str.toLowerCase();
			String[] astring = Config.tokenize(str, " ");
			DyeColor[] aenumdyecolor = new DyeColor[astring.length];

			for (int i = 0; i < astring.length; ++i) {
				String s = astring[i];
				DyeColor enumdyecolor = parseEnumDyeColor(s);

				if (enumdyecolor == null) {
					warn("Invalid color: " + s);
					return COLORS_INVALID;
				}

				aenumdyecolor[i] = enumdyecolor;
			}

			return aenumdyecolor;
		}
	}

	private static DyeColor parseEnumDyeColor(String str) {
		if (str != null) {
			DyeColor[] aenumdyecolor = DyeColor.values();

			for (DyeColor enumdyecolor : aenumdyecolor) {
				if (enumdyecolor.getName().equals(str)) {
					return enumdyecolor;
				}

				if (enumdyecolor.getUnlocalizedName().equals(str)) {
					return enumdyecolor;
				}
			}

		}
		return null;
	}

	private static ResourceLocation parseTextureLocation(String str, String basePath) {
		if (str == null) {
			return null;
		} else {
			str = str.trim();
			String s = TextureUtils.fixResourcePath(str, basePath);

			if (!s.endsWith(".png")) {
				s = s + ".png";
			}

			return new ResourceLocation(basePath + "/" + s);
		}
	}

	private static Map<ResourceLocation, ResourceLocation> parseTextureLocations(Properties props, String property, EnumContainer container, String pathPrefix, String basePath) {
		Map<ResourceLocation, ResourceLocation> map = new HashMap<>();
		String s = props.getProperty(property);

		if (s != null) {
			ResourceLocation resourcelocation = getGuiTextureLocation(container);
			ResourceLocation resourcelocation1 = parseTextureLocation(s, basePath);

			if (resourcelocation != null && resourcelocation1 != null) {
				map.put(resourcelocation, resourcelocation1);
			}
		}

		String s5 = property + ".";

		for (Object o : props.keySet()) {
			String s1 = (String) o;
			if (s1.startsWith(s5)) {
				String s2 = s1.substring(s5.length());
				s2 = s2.replace('\\', '/');
				s2 = StrUtils.removePrefixSuffix(s2, "/", ".png");
				String s3 = pathPrefix + s2 + ".png";
				String s4 = props.getProperty(s1);
				ResourceLocation resourcelocation2 = new ResourceLocation(s3);
				ResourceLocation resourcelocation3 = parseTextureLocation(s4, basePath);
				map.put(resourcelocation2, resourcelocation3);
			}
		}

		return map;
	}

	private static ResourceLocation getGuiTextureLocation(EnumContainer container) {
		if (container == null) {
			return null;
		} else {
			return switch (container) {
				case ANVIL -> ANVIL_GUI_TEXTURE;
				case BEACON -> BEACON_GUI_TEXTURE;
				case BREWING_STAND -> BREWING_STAND_GUI_TEXTURE;
				case CHEST -> CHEST_GUI_TEXTURE;
				case CRAFTING -> CRAFTING_TABLE_GUI_TEXTURE;
				case CREATIVE -> null;
				case DISPENSER -> DISPENSER_GUI_TEXTURE;
				case ENCHANTMENT -> ENCHANTMENT_TABLE_GUI_TEXTURE;
				case FURNACE -> FURNACE_GUI_TEXTURE;
				case HOPPER -> HOPPER_GUI_TEXTURE;
				case HORSE -> HORSE_GUI_TEXTURE;
				case INVENTORY -> INVENTORY_GUI_TEXTURE;
				case SHULKER_BOX -> SHULKER_BOX_GUI_TEXTURE;
				case VILLAGER -> VILLAGER_GUI_TEXTURE;
				default -> null;
			};
		}
	}

	private static void warn(String str) {
		Log.error("[CustomGuis] " + str);
	}

	public static String getName(GuiScreen screen) {
		IWorldNameable iworldnameable = getWorldNameable(screen);
		return iworldnameable == null ? null : iworldnameable.getDisplayName().getUnformattedText();
	}

	private static IWorldNameable getWorldNameable(GuiScreen screen) {
		return switch (screen) {
			case GuiBeacon ignored -> getWorldNameable(ignored.getTileBeacon());
			case GuiBrewingStand ignored -> getWorldNameable(ignored.getTileBrewingStand());
			case GuiChest ignored -> getWorldNameable(ignored.getLowerChestInventory());
			case GuiDispenser dispenser -> dispenser.dispenserInventory;
			case GuiEnchantment ignored -> getWorldNameable(ignored.getNameable());
			case GuiFurnace ignored -> getWorldNameable(ignored.getTileFurnace());
			case GuiHopper ignored -> getWorldNameable(ignored.getHopperInventory());
			case null, default -> null;
		};
	}

	private static IWorldNameable getWorldNameable(Object obj) {
		if (obj instanceof IWorldNameable iWorldNameable)
			return iWorldNameable;

		return null;
	}

	public boolean isValid(String path) {
		if (this.fileName != null && !this.fileName.isEmpty()) {
			if (this.basePath == null) {
				warn("No base path found: " + path);
				return false;
			} else if (this.container == null) {
				warn("No container found: " + path);
				return false;
			} else if (this.textureLocations.isEmpty()) {
				warn("No texture found: " + path);
				return false;
			} else if (this.professions == ConnectedParser.PROFESSIONS_INVALID) {
				warn("Invalid professions or careers: " + path);
				return false;
			} else if (this.variants == VARIANTS_INVALID) {
				warn("Invalid variants: " + path);
				return false;
			} else if (this.colors == COLORS_INVALID) {
				warn("Invalid colors: " + path);
				return false;
			} else {
				return true;
			}
		} else {
			warn("No name found: " + path);
			return false;
		}
	}

	private boolean matchesGeneral(EnumContainer ec, BlockPos pos, IBlockAccess blockAccess) {
		if (this.container != ec) {
			return false;
		} else {
			if (this.biomes != null) {
				BiomeGenBase biomegenbase = blockAccess.getBiomeGenForCoords(pos);

				if (!Matches.biome(biomegenbase, this.biomes)) {
					return false;
				}
			}

			return this.heights == null || this.heights.isInRange(pos.getY());
		}
	}

	public boolean matchesPos(EnumContainer ec, BlockPos pos, IBlockAccess blockAccess, GuiScreen screen) {
		if (!this.matchesGeneral(ec, pos, blockAccess)) {
			return false;
		} else {
			if (this.nbtName != null) {
				String s = getName(screen);

				if (!this.nbtName.matchesValue(s)) {
					return false;
				}
			}

			return switch (ec) {
				case BEACON -> this.matchesBeacon(pos, blockAccess);
				case CHEST -> this.matchesChest(pos, blockAccess);
				case DISPENSER -> this.matchesDispenser(pos, blockAccess);
				default -> true;
			};
		}
	}

	private boolean matchesBeacon(BlockPos pos, IBlockAccess blockAccess) {
		TileEntity tileentity = blockAccess.getTileEntity(pos);

		if (!(tileentity instanceof TileEntityBeacon tileentitybeacon)) {
			return false;
		} else {

			if (this.levels != null) {
				NBTTagCompound nbttagcompound = new NBTTagCompound();
				tileentitybeacon.writeToNBT(nbttagcompound);
				int i = nbttagcompound.getInteger("Levels");

				return this.levels.isInRange(i);
			}

			return true;
		}
	}

	private boolean matchesChest(BlockPos pos, IBlockAccess blockAccess) {
		TileEntity tileentity = blockAccess.getTileEntity(pos);

		if (tileentity instanceof TileEntityChest tileentitychest) {
			return this.matchesChest(tileentitychest, pos, blockAccess);
		} else if (tileentity instanceof TileEntityEnderChest tileentityenderchest) {
			return this.matchesEnderChest(tileentityenderchest, pos, blockAccess);
		} else {
			return false;
		}
	}

	private boolean matchesChest(TileEntityChest tec, BlockPos pos, IBlockAccess blockAccess) {
		boolean flag = tec.adjacentChestXNeg != null || tec.adjacentChestXPos != null || tec.adjacentChestZNeg != null || tec.adjacentChestZPos != null;
		boolean flag1 = tec.getChestType() == 1;
		boolean flag2 = CustomGuis.IS_CHRISTMAS;
		boolean flag3 = false;
		return this.matchesChest(flag, flag1, flag2, flag3);
	}

	private boolean matchesEnderChest(TileEntityEnderChest teec, BlockPos pos, IBlockAccess blockAccess) {
		return this.matchesChest(false, false, false, true);
	}

	private boolean matchesChest(boolean isLarge, boolean isTrapped, boolean isChristmas, boolean isEnder) {
		return (this.large == null || this.large == isLarge) && ((this.trapped == null || this.trapped == isTrapped) && ((this.christmas == null || this.christmas == isChristmas) && (this.ender == null || this.ender == isEnder)));
	}

	private boolean matchesDispenser(BlockPos pos, IBlockAccess blockAccess) {
		TileEntity tileentity = blockAccess.getTileEntity(pos);

		if (!(tileentity instanceof TileEntityDispenser tileentitydispenser)) {
			return false;
		} else {

			if (this.variants != null) {
				EnumVariant customguiproperties$enumvariant = this.getDispenserVariant(tileentitydispenser);

				return Config.equalsOne(customguiproperties$enumvariant, this.variants);
			}

			return true;
		}
	}

	private EnumVariant getDispenserVariant(TileEntityDispenser ted) {
		return ted instanceof TileEntityDropper ? EnumVariant.DROPPER : EnumVariant.DISPENSER;
	}

	public boolean matchesEntity(EnumContainer ec, Entity entity, IBlockAccess blockAccess) {
		if (!this.matchesGeneral(ec, entity.getPosition(), blockAccess)) {
			return false;
		} else {
			if (this.nbtName != null) {
				String s = entity.getName();

				if (!this.nbtName.matchesValue(s)) {
					return false;
				}
			}

			return switch (ec) {
				case HORSE -> this.matchesHorse(entity, blockAccess);
				case VILLAGER -> this.matchesVillager(entity, blockAccess);
				default -> true;
			};
		}
	}

	private boolean matchesVillager(Entity entity, IBlockAccess blockAccess) {
		if (!(entity instanceof EntityVillager entityvillager)) {
			return false;
		} else {

			if (this.professions != null) {
				int i = entityvillager.getProfession();
				int j = entityvillager.getCareerId();

				if (j < 0) {
					return false;
				}

				boolean flag = false;

				for (VillagerProfession villagerprofession : this.professions) {
					if (villagerprofession.matches(i, j)) {
						flag = true;
						break;
					}
				}

				return flag;
			}

			return true;
		}
	}

	private boolean matchesHorse(Entity entity, IBlockAccess blockAccess) {
		if (!(entity instanceof EntityHorse entityhorse)) {
			return false;
		} else {

			if (this.variants != null) {
				EnumVariant customguiproperties$enumvariant = this.getHorseVariant(entityhorse);

				return Config.equalsOne(customguiproperties$enumvariant, this.variants);
			}

			return true;
		}
	}

	private EnumVariant getHorseVariant(EntityHorse entity) {
		int i = entity.getHorseType();

		return switch (i) {
			case 0 -> EnumVariant.HORSE;
			case 1 -> EnumVariant.DONKEY;
			case 2 -> EnumVariant.MULE;
			default -> null;
		};
	}

	public EnumContainer getContainer() {
		return this.container;
	}

	public ResourceLocation getTextureLocation(ResourceLocation loc) {
		ResourceLocation resourcelocation = this.textureLocations.get(loc);
		return resourcelocation == null ? loc : resourcelocation;
	}

	public String toString() {
		return "name: " + this.fileName + ", container: " + this.container + ", textures: " + this.textureLocations;
	}

	public enum EnumContainer {
		ANVIL,
		BEACON,
		BREWING_STAND,
		CHEST,
		CRAFTING,
		DISPENSER,
		ENCHANTMENT,
		FURNACE,
		HOPPER,
		HORSE,
		VILLAGER,
		SHULKER_BOX,
		CREATIVE,
		INVENTORY;

		public static final EnumContainer[] VALUES = values();
	}

	private enum EnumVariant {
		HORSE,
		DONKEY,
		MULE,
		LLAMA,
		DISPENSER,
		DROPPER
	}
}
