package net.optifine.reflect;


import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiHopper;
import net.minecraft.client.gui.inventory.GuiBeacon;
import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.entity.RenderBoat;
import net.minecraft.client.renderer.entity.RenderLeashKnot;
import net.minecraft.client.renderer.entity.RenderMinecart;
import net.minecraft.client.renderer.tileentity.*;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityEnchantmentTable;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IWorldNameable;
import net.optifine.Log;
import net.optifine.util.ArrayUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class Reflector {
    // Reflector Vanilla
    public static final ReflectorField EntityVillager_careerId = new ReflectorField(new FieldLocatorTypes(EntityVillager.class, new Class[0], Integer.TYPE, new Class[]{Integer.TYPE, Boolean.TYPE, Boolean.TYPE, InventoryBasic.class}, "EntityVillager.careerId"));
    public static final ReflectorField EntityVillager_careerLevel = new ReflectorField(new FieldLocatorTypes(EntityVillager.class, new Class[]{Integer.TYPE}, Integer.TYPE, new Class[]{Boolean.TYPE, Boolean.TYPE, InventoryBasic.class}, "EntityVillager.careerLevel"));

    public static final ReflectorField GuiBeacon_tileBeacon = new ReflectorField(new ReflectorClass(GuiBeacon.class), IInventory.class);
    public static final ReflectorField GuiBrewingStand_tileBrewingStand = new ReflectorField(new ReflectorClass(GuiBrewingStand.class), IInventory.class);
    public static final ReflectorField GuiChest_lowerChestInventory = new ReflectorField(new ReflectorClass(GuiChest.class), IInventory.class, 1);
    public static final ReflectorField GuiEnchantment_nameable = new ReflectorField(new ReflectorClass(GuiEnchantment.class), IWorldNameable.class);
    public static final ReflectorField GuiFurnace_tileFurnace = new ReflectorField(new ReflectorClass(GuiFurnace.class), IInventory.class);
    public static final ReflectorField GuiHopper_hopperInventory = new ReflectorField(new ReflectorClass(GuiHopper.class), IInventory.class, 1);

    public static final ReflectorField ModelHumanoidHead_head = new ReflectorField(new ReflectorClass(ModelHumanoidHead.class), ModelRenderer.class);
    public static final ReflectorFields ModelBat_ModelRenderers = new ReflectorFields(new ReflectorClass(ModelBat.class), ModelRenderer.class, 6);
    public static final ReflectorClass ModelBlaze = new ReflectorClass(ModelBlaze.class);
    public static final ReflectorField ModelBlaze_blazeHead = new ReflectorField(ModelBlaze, ModelRenderer.class);
    public static final ReflectorField ModelBlaze_blazeSticks = new ReflectorField(ModelBlaze, ModelRenderer[].class);
    public static final ReflectorClass ModelBlock = new ReflectorClass(ModelBlock.class);
    public static final ReflectorField ModelBlock_parentLocation = new ReflectorField(ModelBlock, ResourceLocation.class);
    public static final ReflectorField ModelBlock_textures = new ReflectorField(ModelBlock, Map.class);
    public static final ReflectorFields ModelDragon_ModelRenderers = new ReflectorFields(new ReflectorClass(ModelDragon.class), ModelRenderer.class, 12);
    public static final ReflectorFields ModelEnderCrystal_ModelRenderers = new ReflectorFields(new ReflectorClass(ModelEnderCrystal.class), ModelRenderer.class, 3);
    public static final ReflectorField RenderEnderCrystal_modelEnderCrystal = new ReflectorField(new ReflectorClass(RenderEnderCrystal.class), ModelBase.class, 0);
    public static final ReflectorField ModelEnderMite_bodyParts = new ReflectorField(new ReflectorClass(ModelEnderMite.class), ModelRenderer[].class);
    public static final ReflectorClass ModelGhast = new ReflectorClass(ModelGhast.class);
    public static final ReflectorField ModelGhast_body = new ReflectorField(ModelGhast, ModelRenderer.class);
    public static final ReflectorField ModelGhast_tentacles = new ReflectorField(ModelGhast, ModelRenderer[].class);
    public static final ReflectorClass ModelGuardian = new ReflectorClass(ModelGuardian.class);
    public static final ReflectorField ModelGuardian_body = new ReflectorField(ModelGuardian, ModelRenderer.class, 0);
    public static final ReflectorField ModelGuardian_eye = new ReflectorField(ModelGuardian, ModelRenderer.class, 1);
    public static final ReflectorField ModelGuardian_spines = new ReflectorField(ModelGuardian, ModelRenderer[].class, 0);
    public static final ReflectorField ModelGuardian_tail = new ReflectorField(ModelGuardian, ModelRenderer[].class, 1);
    public static final ReflectorFields ModelHorse_ModelRenderers = new ReflectorFields(new ReflectorClass(ModelHorse.class), ModelRenderer.class, 39);
    public static final ReflectorField RenderLeashKnot_leashKnotModel = new ReflectorField(new ReflectorClass(RenderLeashKnot.class), ModelLeashKnot.class);
    public static final ReflectorClass ModelMagmaCube = new ReflectorClass(ModelMagmaCube.class);
    public static final ReflectorField ModelMagmaCube_core = new ReflectorField(ModelMagmaCube, ModelRenderer.class);
    public static final ReflectorField ModelMagmaCube_segments = new ReflectorField(ModelMagmaCube, ModelRenderer[].class);
    public static final ReflectorFields ModelOcelot_ModelRenderers = new ReflectorFields(new ReflectorClass(ModelOcelot.class), ModelRenderer.class, 8);
    public static final ReflectorFields ModelRabbit_renderers = new ReflectorFields(new ReflectorClass(ModelRabbit.class), ModelRenderer.class, 12);
    public static final ReflectorClass ModelSilverfish = new ReflectorClass(ModelSilverfish.class);
    public static final ReflectorField ModelSilverfish_bodyParts = new ReflectorField(ModelSilverfish, ModelRenderer[].class, 0);
    public static final ReflectorField ModelSilverfish_wingParts = new ReflectorField(ModelSilverfish, ModelRenderer[].class, 1);
    public static final ReflectorFields ModelSlime_ModelRenderers = new ReflectorFields(new ReflectorClass(ModelSlime.class), ModelRenderer.class, 4);
    public static final ReflectorClass ModelSquid = new ReflectorClass(ModelSquid.class);
    public static final ReflectorField ModelSquid_body = new ReflectorField(ModelSquid, ModelRenderer.class);
    public static final ReflectorField ModelSquid_tentacles = new ReflectorField(ModelSquid, ModelRenderer[].class);
    public static final ReflectorClass ModelWitch = new ReflectorClass(ModelWitch.class);
    public static final ReflectorField ModelWitch_mole = new ReflectorField(ModelWitch, ModelRenderer.class, 0);
    public static final ReflectorField ModelWitch_hat = new ReflectorField(ModelWitch, ModelRenderer.class, 1);
    public static final ReflectorClass ModelWither = new ReflectorClass(ModelWither.class);
    public static final ReflectorField ModelWither_bodyParts = new ReflectorField(ModelWither, ModelRenderer[].class, 0);
    public static final ReflectorField ModelWither_heads = new ReflectorField(ModelWither, ModelRenderer[].class, 1);
    public static final ReflectorClass ModelWolf = new ReflectorClass(ModelWolf.class);
    public static final ReflectorField ModelWolf_tail = new ReflectorField(ModelWolf, ModelRenderer.class, 6);
    public static final ReflectorField ModelWolf_mane = new ReflectorField(ModelWolf, ModelRenderer.class, 7);
    public static final ReflectorField RenderBoat_modelBoat = new ReflectorField(new ReflectorClass(RenderBoat.class), ModelBase.class);
    public static final ReflectorField RenderMinecart_modelMinecart = new ReflectorField(new ReflectorClass(RenderMinecart.class), ModelBase.class);
    public static final ReflectorField RenderWitherSkull_model = new ReflectorField(new ReflectorClass(RenderWitherSkull.class), ModelSkeletonHead.class);
    public static final ReflectorField TileEntityBannerRenderer_bannerModel = new ReflectorField(new ReflectorClass(TileEntityBannerRenderer.class), ModelBanner.class);
    public static final ReflectorField TileEntityBeacon_customName = new ReflectorField(new ReflectorClass(TileEntityBeacon.class), String.class);
    public static final ReflectorField TileEntityBrewingStand_customName = new ReflectorField(new ReflectorClass(TileEntityBrewingStand.class), String.class);
    public static final ReflectorClass TileEntityChestRenderer = new ReflectorClass(TileEntityChestRenderer.class);
    public static final ReflectorField TileEntityChestRenderer_simpleChest = new ReflectorField(TileEntityChestRenderer, ModelChest.class, 0);
    public static final ReflectorField TileEntityChestRenderer_largeChest = new ReflectorField(TileEntityChestRenderer, ModelChest.class, 1);
    public static final ReflectorField TileEntityEnchantmentTable_customName = new ReflectorField(new ReflectorClass(TileEntityEnchantmentTable.class), String.class);
    public static final ReflectorField TileEntityEnchantmentTableRenderer_modelBook = new ReflectorField(new ReflectorClass(TileEntityEnchantmentTableRenderer.class), ModelBook.class);
    public static final ReflectorField TileEntityEnderChestRenderer_modelChest = new ReflectorField(new ReflectorClass(TileEntityEnderChestRenderer.class), ModelChest.class);
    public static final ReflectorField TileEntityFurnace_customName = new ReflectorField(new ReflectorClass(TileEntityFurnace.class), String.class);
    public static final ReflectorField TileEntitySignRenderer_model = new ReflectorField(new ReflectorClass(TileEntitySignRenderer.class), ModelSign.class);
    public static final ReflectorField TileEntitySkullRenderer_humanoidHead = new ReflectorField(new ReflectorClass(TileEntitySkullRenderer.class), ModelSkeletonHead.class, 1);

    public static Object call(ReflectorMethod refMethod, Object... params) {
        try {
            Method method = refMethod.getTargetMethod();

            if (method == null) {
                return null;
            } else {
                return method.invoke(null, params);
            }
        } catch (Throwable throwable) {
            handleException(throwable, null, refMethod, params);
            return null;
        }
    }

    public static Object getFieldValue(ReflectorField refField) {
        return getFieldValue(null, refField);
    }

    public static Object getFieldValue(Object obj, ReflectorField refField) {
        try {
            Field field = refField.getTargetField();

            if (field == null) {
                return null;
            } else {
                return field.get(obj);
            }
        } catch (Throwable throwable) {
            Log.error("", throwable);
            return null;
        }
    }

    public static Object getFieldValue(ReflectorFields refFields, int index) {
        ReflectorField reflectorfield = refFields.getReflectorField(index);
        return reflectorfield == null ? null : getFieldValue(reflectorfield);
    }

    public static Object getFieldValue(Object obj, ReflectorFields refFields, int index) {
        ReflectorField reflectorfield = refFields.getReflectorField(index);
        return reflectorfield == null ? null : getFieldValue(obj, reflectorfield);
    }

    public static int getFieldValueInt(Object obj, ReflectorField refField, int def) {
        try {
            Field field = refField.getTargetField();

            if (field == null) {
                return def;
            } else {
                return field.getInt(obj);
            }
        } catch (Throwable throwable) {
            Log.error("", throwable);
            return def;
        }
    }

    public static boolean setFieldValue(ReflectorField refField, Object value) {
        return setFieldValue(null, refField, value);
    }

    public static boolean setFieldValue(Object obj, ReflectorField refField, Object value) {
        try {
            Field field = refField.getTargetField();

            if (field == null) {
                return false;
            } else {
                field.set(obj, value);
                return true;
            }
        } catch (Throwable throwable) {
            Log.error("", throwable);
            return false;
        }
    }

    public static void setFieldValueInt(Object obj, ReflectorField refField, int value) {
        try {
            Field field = refField.getTargetField();

            if (field != null) {
                field.setInt(obj, value);
            }
        } catch (Throwable throwable) {
            Log.error("", throwable);
        }
    }

    public static Object newInstance(ReflectorConstructor constr, Object... params) {
        Constructor constructor = constr.getTargetConstructor();

        if (constructor == null) {
            return null;
        } else {
            try {
                return constructor.newInstance(params);
            } catch (Throwable throwable) {
                handleException(throwable, constr, params);
                return null;
            }
        }
    }

    public static boolean matchesTypes(Class[] pTypes, Class[] cTypes) {
        if (pTypes.length != cTypes.length) {
            return false;
        } else {
            for (int i = 0; i < cTypes.length; ++i) {
                Class oclass = pTypes[i];
                Class oclass1 = cTypes[i];

                if (oclass != oclass1) {
                    return false;
                }
            }

            return true;
        }
    }

    private static void handleException(Throwable e, Object obj, ReflectorMethod refMethod, Object[] params) {
        if (e instanceof InvocationTargetException) {
            Throwable throwable = e.getCause();

            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else {
                Log.error("", e);
            }
        } else {
            Log.warn("*** Exception outside of method ***");
            Log.warn("Method deactivated: " + refMethod.getTargetMethod());
            refMethod.deactivate();

            if (e instanceof IllegalArgumentException) {
                Log.warn("*** IllegalArgumentException ***");
                Log.warn("Method: " + refMethod.getTargetMethod());
                Log.warn("Object: " + obj);
                Log.warn("Parameter classes: " + ArrayUtils.arrayToString(getClasses(params)));
                Log.warn("Parameters: " + ArrayUtils.arrayToString(params));
            }

            Log.warn("", e);
        }
    }

    private static void handleException(Throwable e, ReflectorConstructor refConstr, Object[] params) {
        if (e instanceof InvocationTargetException) {
            Log.error("", e);
        } else {
            Log.warn("*** Exception outside of constructor ***");
            Log.warn("Constructor deactivated: " + refConstr.getTargetConstructor());
            refConstr.deactivate();

            if (e instanceof IllegalArgumentException) {
                Log.warn("*** IllegalArgumentException ***");
                Log.warn("Constructor: " + refConstr.getTargetConstructor());
                Log.warn("Parameter classes: " + ArrayUtils.arrayToString(getClasses(params)));
                Log.warn("Parameters: " + ArrayUtils.arrayToString(params));
            }

            Log.warn("", e);
        }
    }

    private static Object[] getClasses(Object[] objs) {
        if (objs == null) {
            return new Class[0];
        } else {
            Class[] aclass = new Class[objs.length];

            for (int i = 0; i < aclass.length; ++i) {
                Object object = objs[i];

                if (object != null) {
                    aclass[i] = object.getClass();
                }
            }

            return aclass;
        }
    }
}
