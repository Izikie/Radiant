package net.optifine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.*;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.optifine.override.PlayerControllerOF;
import net.optifine.util.collection.PropertiesOrdered;
import net.optifine.util.ResUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

public class CustomGuis {
	public static final boolean IS_CHRISTMAS = isChristmas();
	private static final Minecraft MINECRAFT = Config.getMinecraft();
	private static final PlayerControllerOF PLAYER_CONTROLLER_OF = null;
	private static CustomGuiProperties[][] guiProperties = null;

	public static ResourceLocation getTextureLocation(ResourceLocation loc) {
		if (guiProperties == null) {
			return loc;
		} else {
			GuiScreen guiscreen = MINECRAFT.currentScreen;

			if (!(guiscreen instanceof GuiContainer)) {
				return loc;
			} else if (loc.getResourceDomain().equals("minecraft") && loc.getResourcePath().startsWith("textures/gui/")) {
				if (PLAYER_CONTROLLER_OF == null) {
					return loc;
				} else {
					IBlockAccess iblockaccess = MINECRAFT.world;

					if (iblockaccess == null) {
						return loc;
					} else if (guiscreen instanceof GuiContainerCreative) {
						return getTexturePos(CustomGuiProperties.EnumContainer.CREATIVE, MINECRAFT.player.getPosition(), iblockaccess, loc, guiscreen);
					} else if (guiscreen instanceof GuiInventory) {
						return getTexturePos(CustomGuiProperties.EnumContainer.INVENTORY, MINECRAFT.player.getPosition(), iblockaccess, loc, guiscreen);
					} else {
						BlockPos blockpos = PLAYER_CONTROLLER_OF.getLastClickBlockPos();

						if (blockpos != null) {
							if (guiscreen instanceof GuiRepair) {
								return getTexturePos(CustomGuiProperties.EnumContainer.ANVIL, blockpos, iblockaccess, loc, guiscreen);
							}

							if (guiscreen instanceof GuiBeacon) {
								return getTexturePos(CustomGuiProperties.EnumContainer.BEACON, blockpos, iblockaccess, loc, guiscreen);
							}

							if (guiscreen instanceof GuiBrewingStand) {
								return getTexturePos(CustomGuiProperties.EnumContainer.BREWING_STAND, blockpos, iblockaccess, loc, guiscreen);
							}

							if (guiscreen instanceof GuiChest) {
								return getTexturePos(CustomGuiProperties.EnumContainer.CHEST, blockpos, iblockaccess, loc, guiscreen);
							}

							if (guiscreen instanceof GuiCrafting) {
								return getTexturePos(CustomGuiProperties.EnumContainer.CRAFTING, blockpos, iblockaccess, loc, guiscreen);
							}

							if (guiscreen instanceof GuiDispenser) {
								return getTexturePos(CustomGuiProperties.EnumContainer.DISPENSER, blockpos, iblockaccess, loc, guiscreen);
							}

							if (guiscreen instanceof GuiEnchantment) {
								return getTexturePos(CustomGuiProperties.EnumContainer.ENCHANTMENT, blockpos, iblockaccess, loc, guiscreen);
							}

							if (guiscreen instanceof GuiFurnace) {
								return getTexturePos(CustomGuiProperties.EnumContainer.FURNACE, blockpos, iblockaccess, loc, guiscreen);
							}

							if (guiscreen instanceof GuiHopper) {
								return getTexturePos(CustomGuiProperties.EnumContainer.HOPPER, blockpos, iblockaccess, loc, guiscreen);
							}
						}

						Entity entity = PLAYER_CONTROLLER_OF.getLastClickEntity();

						if (entity != null) {
							if (guiscreen instanceof GuiScreenHorseInventory) {
								return getTextureEntity(CustomGuiProperties.EnumContainer.HORSE, entity, iblockaccess, loc);
							}

							if (guiscreen instanceof GuiMerchant) {
								return getTextureEntity(CustomGuiProperties.EnumContainer.VILLAGER, entity, iblockaccess, loc);
							}
						}

						return loc;
					}
				}
			} else {
				return loc;
			}
		}
	}

	private static ResourceLocation getTexturePos(CustomGuiProperties.EnumContainer container, BlockPos pos, IBlockAccess blockAccess, ResourceLocation loc, GuiScreen screen) {
		CustomGuiProperties[] acustomguiproperties = guiProperties[container.ordinal()];

		if (acustomguiproperties != null) {
			for (CustomGuiProperties customguiproperties : acustomguiproperties) {
				if (customguiproperties.matchesPos(container, pos, blockAccess, screen)) {
					return customguiproperties.getTextureLocation(loc);
				}
			}

		}
		return loc;
	}

	private static ResourceLocation getTextureEntity(CustomGuiProperties.EnumContainer container, Entity entity, IBlockAccess blockAccess, ResourceLocation loc) {
		CustomGuiProperties[] acustomguiproperties = guiProperties[container.ordinal()];

		if (acustomguiproperties != null) {
			for (CustomGuiProperties customguiproperties : acustomguiproperties) {
				if (customguiproperties.matchesEntity(container, entity, blockAccess)) {
					return customguiproperties.getTextureLocation(loc);
				}
			}

		}
		return loc;
	}

	public static void update() {
		guiProperties = null;

		if (Config.isCustomGuis()) {
			List<List<CustomGuiProperties>> list = new ArrayList<>();
			IResourcePack[] airesourcepack = Config.getResourcePacks();

			for (int i = airesourcepack.length - 1; i >= 0; --i) {
				IResourcePack iresourcepack = airesourcepack[i];
				update(iresourcepack, list);
			}

			guiProperties = propertyListToArray(list);
		}
	}

	private static CustomGuiProperties[][] propertyListToArray(List<List<CustomGuiProperties>> listProps) {
		if (listProps.isEmpty()) {
			return null;
		} else {
			CustomGuiProperties[][] acustomguiproperties = new CustomGuiProperties[CustomGuiProperties.EnumContainer.VALUES.length][];

			for (int i = 0; i < acustomguiproperties.length; ++i) {
				if (listProps.size() > i) {
					List<CustomGuiProperties> list = listProps.get(i);

					if (list != null) {
						CustomGuiProperties[] acustomguiproperties1 = list.toArray(new CustomGuiProperties[0]);
						acustomguiproperties[i] = acustomguiproperties1;
					}
				}
			}

			return acustomguiproperties;
		}
	}

	private static void update(IResourcePack rp, List<List<CustomGuiProperties>> listProps) {
		String[] astring = ResUtils.collectFiles(rp, "optifine/gui/container/", ".properties", null);
		Arrays.sort(astring);

		for (String s : astring) {
			Log.info("CustomGuis: " + s);

			try {
				ResourceLocation resourcelocation = new ResourceLocation(s);
				InputStream inputstream = rp.getInputStream(resourcelocation);

				if (inputstream == null) {
					Log.error("CustomGuis file not found: " + s);
				} else {
					Properties properties = new PropertiesOrdered();
					properties.load(inputstream);
					inputstream.close();
					CustomGuiProperties customguiproperties = new CustomGuiProperties(properties, s);

					if (customguiproperties.isValid(s)) {
						addToList(customguiproperties, listProps);
					}
				}
			} catch (FileNotFoundException exception) {
				Log.error("CustomGuis file not found: " + s);
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}
	}

	private static void addToList(CustomGuiProperties cgp, List<List<CustomGuiProperties>> listProps) {
		if (cgp.getContainer() == null) {
			warn("Invalid container: " + cgp.getContainer());
		} else {
			int i = cgp.getContainer().ordinal();

			while (listProps.size() <= i) {
				listProps.add(null);
			}

			List<CustomGuiProperties> list = listProps.get(i);

			if (list == null) {
				list = new ArrayList<>();
				listProps.set(i, list);
			}

			list.add(cgp);
		}
	}

	public static PlayerControllerOF getPlayerControllerOF() {
		return PLAYER_CONTROLLER_OF;
	}

	public static void setPlayerControllerOF(PlayerControllerOF playerControllerOF) {
	}

	private static boolean isChristmas() {
		Calendar calendar = Calendar.getInstance();
		return calendar.get(Calendar.MONTH) + 1 == 12 && calendar.get(Calendar.DATE) >= 24 && calendar.get(Calendar.DATE) <= 26;
	}

	private static void warn(String str) {
		Log.error("[CustomGuis] " + str);
	}
}
