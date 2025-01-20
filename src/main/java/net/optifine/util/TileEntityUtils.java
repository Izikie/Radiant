package net.optifine.util;

import net.minecraft.src.Config;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityEnchantmentTable;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.IWorldNameable;
import net.optifine.reflect.Reflector;

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
            case TileEntityBeacon tileEntityBeacon -> {
                return (String) Reflector.getFieldValue(te, Reflector.TileEntityBeacon_customName);
            }
            case TileEntityBrewingStand tileEntityBrewingStand -> {
                return (String) Reflector.getFieldValue(te, Reflector.TileEntityBrewingStand_customName);
            }
            case TileEntityEnchantmentTable tileEntityEnchantmentTable -> {
                return (String) Reflector.getFieldValue(te, Reflector.TileEntityEnchantmentTable_customName);
            }
            case TileEntityFurnace tileEntityFurnace -> {
                return (String) Reflector.getFieldValue(te, Reflector.TileEntityFurnace_customName);
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

    public static boolean setTileEntityRawName(TileEntity te, String name) {
        switch (te) {
            case TileEntityBeacon tileEntityBeacon -> {
                return Reflector.setFieldValue(te, Reflector.TileEntityBeacon_customName, name);
            }
            case TileEntityBrewingStand tileEntityBrewingStand -> {
                return Reflector.setFieldValue(te, Reflector.TileEntityBrewingStand_customName, name);
            }
            case TileEntityEnchantmentTable tileEntityEnchantmentTable -> {
                return Reflector.setFieldValue(te, Reflector.TileEntityEnchantmentTable_customName, name);
            }
            case TileEntityFurnace tileEntityFurnace -> {
                return Reflector.setFieldValue(te, Reflector.TileEntityFurnace_customName, name);
            }
            case TileEntityChest tileEntityChest -> {
                tileEntityChest.setCustomName(name);
                return true;
            }
            case TileEntityDispenser tileEntityDispenser -> {
                tileEntityDispenser.setCustomName(name);
                return true;
            }
            case TileEntityHopper tileEntityHopper -> {
                tileEntityHopper.setCustomName(name);
                return true;
            }
            case null, default -> {
                return false;
            }
        }
    }
}
