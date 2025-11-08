package net.optifine.util;

import net.optifine.Config;
import net.minecraft.tileentity.*;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.IWorldNameable;

public class TileEntityUtils {
	public static String getTileEntityName(IBlockAccess blockAccess, BlockPos blockPos) {
		TileEntity tileentity = blockAccess.getTileEntity(blockPos);
		return getTileEntityName(tileentity);
	}

	public static String getTileEntityName(TileEntity te) {
		if (!(te instanceof IWorldNameable iworldnameable)) {
			return null;
		} else {
			updateTileEntityName(te);
			return !iworldnameable.hasCustomName() ? null : iworldnameable.getName();
		}
	}

	public static void updateTileEntityName(TileEntity te) {
		BlockPos blockpos = te.getPos();
		String s = getTileEntityRawName(te);

		if (s == null) {
			String s1 = getServerTileEntityRawName(blockpos);
			s1 = Config.normalize(s1);
			setTileEntityRawName(te, s1);
		}
	}

	public static String getServerTileEntityRawName(BlockPos blockPos) {
		TileEntity tileentity = IntegratedServerUtils.getTileEntity(blockPos);
		return tileentity == null ? null : getTileEntityRawName(tileentity);
	}

	public static String getTileEntityRawName(TileEntity te) {
		switch (te) {
			case TileEntityBeacon ignored -> {
				return ignored.getCustomName();
			}
			case TileEntityBrewingStand ignored -> {
				return ignored.getCustomName();
			}
			case TileEntityEnchantmentTable ignored -> {
				return ignored.getCustomName();
			}
			case TileEntityFurnace ignored -> {
				return ignored.getFurnaceCustomName();
			}
			case null, default -> {
				if (te instanceof IWorldNameable iworldnameable) {

					if (iworldnameable.hasCustomName()) {
						return iworldnameable.getName();
					}
				}

				return null;
			}
		}
	}

	public static void setTileEntityRawName(TileEntity te, String name) {
		switch (te) {
			case TileEntityBeacon ignored -> ignored.setName(name);
			case TileEntityBrewingStand ignored -> ignored.setName(name);
			case TileEntityEnchantmentTable ignored -> ignored.setCustomName(name);
			case TileEntityFurnace ignored -> ignored.setFurnaceCustomName(name);
			case TileEntityChest tileEntityChest -> tileEntityChest.setCustomName(name);
			case TileEntityDispenser tileEntityDispenser -> tileEntityDispenser.setCustomName(name);
			case TileEntityHopper tileEntityHopper -> tileEntityHopper.setCustomName(name);
			case null, default -> {
			}
		}
	}
}
