package net.minecraft.client.renderer.tileentity;

import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnchantmentTable;
import net.minecraft.tileentity.TileEntityEndPortal;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.BlockPos;
import net.minecraft.crash.ReportedException;
import net.minecraft.world.World;
import net.optifine.EmissiveTextures;

public class TileEntityRendererDispatcher {
    public final Map<Class, TileEntitySpecialRenderer> mapSpecialRenderers = new HashMap<>();
    public static final TileEntityRendererDispatcher INSTANCE = new TileEntityRendererDispatcher();
    public FontRenderer fontRenderer;
    public static double staticPlayerX;
    public static double staticPlayerY;
    public static double staticPlayerZ;
    public TextureManager renderEngine;
    public World worldObj;
    public Entity entity;
    public float entityYaw;
    public float entityPitch;
    public double entityX;
    public double entityY;
    public double entityZ;
    public TileEntity tileEntityRendered;
    private final Tessellator batchBuffer = new Tessellator(2097152);

    private TileEntityRendererDispatcher() {
        this.mapSpecialRenderers.put(TileEntitySign.class, new TileEntitySignRenderer());
        this.mapSpecialRenderers.put(TileEntityMobSpawner.class, new TileEntityMobSpawnerRenderer());
        this.mapSpecialRenderers.put(TileEntityPiston.class, new TileEntityPistonRenderer());
        this.mapSpecialRenderers.put(TileEntityChest.class, new TileEntityChestRenderer());
        this.mapSpecialRenderers.put(TileEntityEnderChest.class, new TileEntityEnderChestRenderer());
        this.mapSpecialRenderers.put(TileEntityEnchantmentTable.class, new TileEntityEnchantmentTableRenderer());
        this.mapSpecialRenderers.put(TileEntityEndPortal.class, new TileEntityEndPortalRenderer());
        this.mapSpecialRenderers.put(TileEntityBeacon.class, new TileEntityBeaconRenderer());
        this.mapSpecialRenderers.put(TileEntitySkull.class, new TileEntitySkullRenderer());
        this.mapSpecialRenderers.put(TileEntityBanner.class, new TileEntityBannerRenderer());

        for (TileEntitySpecialRenderer<?> tileentityspecialrenderer : this.mapSpecialRenderers.values()) {
            tileentityspecialrenderer.setRendererDispatcher(this);
        }
    }

    public <T extends TileEntity> TileEntitySpecialRenderer<T> getSpecialRendererByClass(Class<? extends TileEntity> teClass) {
        TileEntitySpecialRenderer<? extends TileEntity> tileentityspecialrenderer = (TileEntitySpecialRenderer) this.mapSpecialRenderers.get(teClass);

        if (tileentityspecialrenderer == null && teClass != TileEntity.class) {
            tileentityspecialrenderer = this.getSpecialRendererByClass((Class<? extends TileEntity>) teClass.getSuperclass());
            this.mapSpecialRenderers.put(teClass, tileentityspecialrenderer);
        }

        return (TileEntitySpecialRenderer<T>) tileentityspecialrenderer;
    }

    public <T extends TileEntity> TileEntitySpecialRenderer<T> getSpecialRenderer(TileEntity tileEntityIn) {
        return tileEntityIn != null && !tileEntityIn.isInvalid() ? this.getSpecialRendererByClass(tileEntityIn.getClass()) : null;
    }

    public void cacheActiveRenderInfo(World worldIn, TextureManager textureManagerIn, FontRenderer fontrendererIn, Entity entityIn, float partialTicks) {
        if (this.worldObj != worldIn) {
            this.setWorld(worldIn);
        }

        this.renderEngine = textureManagerIn;
        this.entity = entityIn;
        this.fontRenderer = fontrendererIn;
        this.entityYaw = entityIn.prevRotationYaw + (entityIn.rotationYaw - entityIn.prevRotationYaw) * partialTicks;
        this.entityPitch = entityIn.prevRotationPitch + (entityIn.rotationPitch - entityIn.prevRotationPitch) * partialTicks;
        this.entityX = entityIn.lastTickPosX + (entityIn.posX - entityIn.lastTickPosX) * partialTicks;
        this.entityY = entityIn.lastTickPosY + (entityIn.posY - entityIn.lastTickPosY) * partialTicks;
        this.entityZ = entityIn.lastTickPosZ + (entityIn.posZ - entityIn.lastTickPosZ) * partialTicks;
    }

    public void renderTileEntity(TileEntity tileentityIn, float partialTicks, int destroyStage) {
        if (tileentityIn.getDistanceSq(this.entityX, this.entityY, this.entityZ) < tileentityIn.getMaxRenderDistanceSquared()) {
            RenderHelper.enableStandardItemLighting();
            int i = this.worldObj.getCombinedLight(tileentityIn.getPos(), 0);
            int j = i % 65536;
            int k = i / 65536;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, j, k);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            BlockPos blockpos = tileentityIn.getPos();

            if (!this.worldObj.isBlockLoaded(blockpos, false)) {
                return;
            }

            if (EmissiveTextures.isActive()) {
                EmissiveTextures.beginRender();
            }

            this.renderTileEntityAt(tileentityIn, blockpos.getX() - staticPlayerX, blockpos.getY() - staticPlayerY, blockpos.getZ() - staticPlayerZ, partialTicks, destroyStage);

            if (EmissiveTextures.isActive()) {
                if (EmissiveTextures.hasEmissive()) {
                    EmissiveTextures.beginRenderEmissive();
                    this.renderTileEntityAt(tileentityIn, blockpos.getX() - staticPlayerX, blockpos.getY() - staticPlayerY, blockpos.getZ() - staticPlayerZ, partialTicks, destroyStage);
                    EmissiveTextures.endRenderEmissive();
                }

                EmissiveTextures.endRender();
            }
        }
    }

    public void renderTileEntityAt(TileEntity tileEntityIn, double x, double y, double z, float partialTicks) {
        this.renderTileEntityAt(tileEntityIn, x, y, z, partialTicks, -1);
    }

    public void renderTileEntityAt(TileEntity tileEntityIn, double x, double y, double z, float partialTicks, int destroyStage) {
        TileEntitySpecialRenderer<TileEntity> tileentityspecialrenderer = this.getSpecialRenderer(tileEntityIn);

        if (tileentityspecialrenderer != null) {
            try {
                this.tileEntityRendered = tileEntityIn;
                tileentityspecialrenderer.renderTileEntityAt(tileEntityIn, x, y, z, partialTicks, destroyStage);

                this.tileEntityRendered = null;
            } catch (Throwable throwable) {
                CrashReport report = CrashReport.makeCrashReport(throwable, "Rendering Block Entity");
                CrashReportCategory category = report.makeCategory("Block Entity Details");
                tileEntityIn.addInfoToCrashReport(category);
                throw new ReportedException(report);
            }
        }
    }

    public void setWorld(World worldIn) {
        this.worldObj = worldIn;
    }

    public FontRenderer getFontRenderer() {
        return this.fontRenderer;
    }
}
