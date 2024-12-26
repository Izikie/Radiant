package net.optifine.reflect;

import java.io.InputStream;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class ReflectorForge {
    public static Object EVENT_RESULT_ALLOW = Reflector.getFieldValue(Reflector.Event_Result_ALLOW);
    public static Object EVENT_RESULT_DEFAULT = Reflector.getFieldValue(Reflector.Event_Result_DEFAULT);

    public static void FMLClientHandler_trackBrokenTexture(ResourceLocation loc, String message) {
        if (!Reflector.FMLClientHandler_trackBrokenTexture.exists()) {
            Object object = Reflector.call(Reflector.FMLClientHandler_instance);
            Reflector.call(object, Reflector.FMLClientHandler_trackBrokenTexture, loc, message);
        }
    }

    public static void FMLClientHandler_trackMissingTexture(ResourceLocation loc) {
        if (!Reflector.FMLClientHandler_trackMissingTexture.exists()) {
            Object object = Reflector.call(Reflector.FMLClientHandler_instance);
            Reflector.call(object, Reflector.FMLClientHandler_trackMissingTexture, loc);
        }
    }

    public static boolean renderFirstPersonHand(RenderGlobal renderGlobal, float partialTicks, int pass) {
        return !Reflector.ForgeHooksClient_renderFirstPersonHand.exists() ? false : Reflector.callBoolean(Reflector.ForgeHooksClient_renderFirstPersonHand, renderGlobal, Float.valueOf(partialTicks), Integer.valueOf(pass));
    }

    public static InputStream getOptiFineResourceStream(String path) {
        return null;
    }

    public static boolean blockHasTileEntity(IBlockState state) {
        Block block = state.getBlock();
        return !Reflector.ForgeBlock_hasTileEntity.exists() ? block.hasTileEntity() : Reflector.callBoolean(block, Reflector.ForgeBlock_hasTileEntity, state);
    }

    public static boolean isItemDamaged(ItemStack stack) {
        return !Reflector.ForgeItem_showDurabilityBar.exists() ? stack.isItemDamaged() : Reflector.callBoolean(stack.getItem(), Reflector.ForgeItem_showDurabilityBar, stack);
    }

    public static String[] getForgeModIds() {
        return new String[0];
    }

    public static boolean canEntitySpawn(EntityLiving entityliving, World world, float x, float y, float z) {
        Object object = Reflector.call(Reflector.ForgeEventFactory_canEntitySpawn, entityliving, world, Float.valueOf(x), Float.valueOf(y), Float.valueOf(z));
        return object == EVENT_RESULT_ALLOW || object == EVENT_RESULT_DEFAULT && entityliving.getCanSpawnHere() && entityliving.isNotColliding();
    }

    public static boolean doSpecialSpawn(EntityLiving entityliving, World world, float x, int y, float z) {
        return Reflector.ForgeEventFactory_doSpecialSpawn.exists() ? Reflector.callBoolean(Reflector.ForgeEventFactory_doSpecialSpawn, entityliving, world, Float.valueOf(x), Integer.valueOf(y), Float.valueOf(z)) : false;
    }
}
