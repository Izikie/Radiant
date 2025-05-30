package net.minecraft.client.renderer.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.RenderEnderCrystal;
import net.minecraft.client.renderer.tileentity.RenderItemFrame;
import net.minecraft.client.renderer.tileentity.RenderWitherSkull;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLeashKnot;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityMinecartMobSpawner;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.projectile.*;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.player.PlayerItemsLayer;
import net.optifine.shaders.Shaders;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RenderManager {
    private Map<Class, Render> entityRenderMap = new HashMap<>();
    private final Map<String, RenderPlayer> skinMap = new HashMap<>();
    private final RenderPlayer playerRenderer;
    private FontRenderer textRenderer;
    private double renderPosX;
    private double renderPosY;
    private double renderPosZ;
    public final TextureManager renderEngine;
    public World worldObj;
    public Entity livingPlayer;
    public Entity pointedEntity;
    public float playerViewY;
    public float playerViewX;
    public GameSettings options;
    public double viewerPosX;
    public double viewerPosY;
    public double viewerPosZ;
    private boolean renderOutlines = false;
    private boolean renderShadow = true;
    private boolean debugBoundingBox = false;
    public Render renderRender = null;

    public RenderManager(TextureManager renderEngineIn, RenderItem itemRendererIn) {
        this.renderEngine = renderEngineIn;
        this.entityRenderMap.put(EntityCaveSpider.class, new RenderCaveSpider(this));
        this.entityRenderMap.put(EntitySpider.class, new RenderSpider(this));
        this.entityRenderMap.put(EntityPig.class, new RenderPig(this, new ModelPig(), 0.7F));
        this.entityRenderMap.put(EntitySheep.class, new RenderSheep(this, new ModelSheep2(), 0.7F));
        this.entityRenderMap.put(EntityCow.class, new RenderCow(this, new ModelCow(), 0.7F));
        this.entityRenderMap.put(EntityMooshroom.class, new RenderMooshroom(this, new ModelCow(), 0.7F));
        this.entityRenderMap.put(EntityWolf.class, new RenderWolf(this, new ModelWolf(), 0.5F));
        this.entityRenderMap.put(EntityChicken.class, new RenderChicken(this, new ModelChicken(), 0.3F));
        this.entityRenderMap.put(EntityOcelot.class, new RenderOcelot(this, new ModelOcelot(), 0.4F));
        this.entityRenderMap.put(EntityRabbit.class, new RenderRabbit(this, new ModelRabbit(), 0.3F));
        this.entityRenderMap.put(EntitySilverfish.class, new RenderSilverfish(this));
        this.entityRenderMap.put(EntityEndermite.class, new RenderEndermite(this));
        this.entityRenderMap.put(EntityCreeper.class, new RenderCreeper(this));
        this.entityRenderMap.put(EntityEnderman.class, new RenderEnderman(this));
        this.entityRenderMap.put(EntitySnowman.class, new RenderSnowMan(this));
        this.entityRenderMap.put(EntitySkeleton.class, new RenderSkeleton(this));
        this.entityRenderMap.put(EntityWitch.class, new RenderWitch(this));
        this.entityRenderMap.put(EntityBlaze.class, new RenderBlaze(this));
        this.entityRenderMap.put(EntityPigZombie.class, new RenderPigZombie(this));
        this.entityRenderMap.put(EntityZombie.class, new RenderZombie(this));
        this.entityRenderMap.put(EntitySlime.class, new RenderSlime(this, new ModelSlime(16), 0.25F));
        this.entityRenderMap.put(EntityMagmaCube.class, new RenderMagmaCube(this));
        this.entityRenderMap.put(EntityGiantZombie.class, new RenderGiantZombie(this, new ModelZombie(), 0.5F, 6.0F));
        this.entityRenderMap.put(EntityGhast.class, new RenderGhast(this));
        this.entityRenderMap.put(EntitySquid.class, new RenderSquid(this, new ModelSquid(), 0.7F));
        this.entityRenderMap.put(EntityVillager.class, new RenderVillager(this));
        this.entityRenderMap.put(EntityIronGolem.class, new RenderIronGolem(this));
        this.entityRenderMap.put(EntityBat.class, new RenderBat(this));
        this.entityRenderMap.put(EntityGuardian.class, new RenderGuardian(this));
        this.entityRenderMap.put(EntityDragon.class, new RenderDragon(this));
        this.entityRenderMap.put(EntityEnderCrystal.class, new RenderEnderCrystal(this));
        this.entityRenderMap.put(EntityWither.class, new RenderWither(this));
        this.entityRenderMap.put(Entity.class, new RenderEntity(this));
        this.entityRenderMap.put(EntityPainting.class, new RenderPainting(this));
        this.entityRenderMap.put(EntityItemFrame.class, new RenderItemFrame(this, itemRendererIn));
        this.entityRenderMap.put(EntityLeashKnot.class, new RenderLeashKnot(this));
        this.entityRenderMap.put(EntityArrow.class, new RenderArrow(this));
        this.entityRenderMap.put(EntitySnowball.class, new RenderSnowball(this, Items.SNOWBALL, itemRendererIn));
        this.entityRenderMap.put(EntityEnderPearl.class, new RenderSnowball(this, Items.ENDER_PEARL, itemRendererIn));
        this.entityRenderMap.put(EntityEnderEye.class, new RenderSnowball(this, Items.ENDER_EYE, itemRendererIn));
        this.entityRenderMap.put(EntityEgg.class, new RenderSnowball(this, Items.EGG, itemRendererIn));
        this.entityRenderMap.put(EntityPotion.class, new RenderPotion(this, itemRendererIn));
        this.entityRenderMap.put(EntityExpBottle.class, new RenderSnowball(this, Items.EXPERIENCE_BOTTLE, itemRendererIn));
        this.entityRenderMap.put(EntityFireworkRocket.class, new RenderSnowball(this, Items.FIREWORKS, itemRendererIn));
        this.entityRenderMap.put(EntityLargeFireball.class, new RenderFireball(this, 2.0F));
        this.entityRenderMap.put(EntitySmallFireball.class, new RenderFireball(this, 0.5F));
        this.entityRenderMap.put(EntityWitherSkull.class, new RenderWitherSkull(this));
        this.entityRenderMap.put(EntityItem.class, new RenderEntityItem(this, itemRendererIn));
        this.entityRenderMap.put(EntityXPOrb.class, new RenderXPOrb(this));
        this.entityRenderMap.put(EntityTNTPrimed.class, new RenderTNTPrimed(this));
        this.entityRenderMap.put(EntityFallingBlock.class, new RenderFallingBlock(this));
        this.entityRenderMap.put(EntityArmorStand.class, new ArmorStandRenderer(this));
        this.entityRenderMap.put(EntityMinecartTNT.class, new RenderTntMinecart(this));
        this.entityRenderMap.put(EntityMinecartMobSpawner.class, new RenderMinecartMobSpawner(this));
        this.entityRenderMap.put(EntityMinecart.class, new RenderMinecart(this));
        this.entityRenderMap.put(EntityBoat.class, new RenderBoat(this));
        this.entityRenderMap.put(EntityFishHook.class, new RenderFish(this));
        this.entityRenderMap.put(EntityHorse.class, new RenderHorse(this, new ModelHorse(), 0.75F));
        this.entityRenderMap.put(EntityLightningBolt.class, new RenderLightningBolt(this));
        this.playerRenderer = new RenderPlayer(this);
        this.skinMap.put("default", this.playerRenderer);
        this.skinMap.put("slim", new RenderPlayer(this, true));
        PlayerItemsLayer.register(this.skinMap);
    }

    public void setRenderPosition(double renderPosXIn, double renderPosYIn, double renderPosZIn) {
        this.renderPosX = renderPosXIn;
        this.renderPosY = renderPosYIn;
        this.renderPosZ = renderPosZIn;
    }

    public <T extends Entity> Render<T> getEntityClassRenderObject(Class<? extends Entity> entityClass) {
        Render<? extends Entity> render = (Render) this.entityRenderMap.get(entityClass);

        if (render == null && entityClass != Entity.class) {
            render = this.getEntityClassRenderObject((Class<? extends Entity>) entityClass.getSuperclass());
            this.entityRenderMap.put(entityClass, render);
        }

        return (Render<T>) render;
    }

    public <T extends Entity> Render<T> getEntityRenderObject(Entity entityIn) {
        if (entityIn instanceof AbstractClientPlayer abstractClientPlayer) {
            String s = abstractClientPlayer.getSkinType();
            RenderPlayer renderplayer = this.skinMap.get(s);
            return (Render<T>) (renderplayer != null ? renderplayer : this.playerRenderer);
        } else {
            return this.getEntityClassRenderObject(entityIn.getClass());
        }
    }

    public void cacheActiveRenderInfo(World worldIn, FontRenderer textRendererIn, Entity livingPlayerIn, Entity pointedEntityIn, GameSettings optionsIn, float partialTicks) {
        this.worldObj = worldIn;
        this.options = optionsIn;
        this.livingPlayer = livingPlayerIn;
        this.pointedEntity = pointedEntityIn;
        this.textRenderer = textRendererIn;

        if (livingPlayerIn instanceof EntityLivingBase entityLivingBase && entityLivingBase.isPlayerSleeping()) {
            IBlockState iblockstate = worldIn.getBlockState(new BlockPos(livingPlayerIn));
            Block block = iblockstate.getBlock();

            if (block == Blocks.BED) {
                int j = iblockstate.getValue(BlockBed.FACING).getHorizontalIndex();
                this.playerViewY = (j * 90 + 180);
                this.playerViewX = 0.0F;
            }
        } else {
            this.playerViewY = livingPlayerIn.prevRotationYaw + (livingPlayerIn.rotationYaw - livingPlayerIn.prevRotationYaw) * partialTicks;
            this.playerViewX = livingPlayerIn.prevRotationPitch + (livingPlayerIn.rotationPitch - livingPlayerIn.prevRotationPitch) * partialTicks;
        }

        if (optionsIn.thirdPersonView == 2) {
            this.playerViewY += 180.0F;
        }

        this.viewerPosX = livingPlayerIn.lastTickPosX + (livingPlayerIn.posX - livingPlayerIn.lastTickPosX) * partialTicks;
        this.viewerPosY = livingPlayerIn.lastTickPosY + (livingPlayerIn.posY - livingPlayerIn.lastTickPosY) * partialTicks;
        this.viewerPosZ = livingPlayerIn.lastTickPosZ + (livingPlayerIn.posZ - livingPlayerIn.lastTickPosZ) * partialTicks;
    }

    public void setPlayerViewY(float playerViewYIn) {
        this.playerViewY = playerViewYIn;
    }

    public boolean isRenderShadow() {
        return this.renderShadow;
    }

    public void setRenderShadow(boolean renderShadowIn) {
        this.renderShadow = renderShadowIn;
    }

    public void setDebugBoundingBox(boolean debugBoundingBoxIn) {
        this.debugBoundingBox = debugBoundingBoxIn;
    }

    public boolean isDebugBoundingBox() {
        return this.debugBoundingBox;
    }

    public boolean renderEntitySimple(Entity entityIn, float partialTicks) {
        return this.renderEntityStatic(entityIn, partialTicks, false);
    }

    public boolean shouldRender(Entity entityIn, ICamera camera, double camX, double camY, double camZ) {
        Render<Entity> render = this.getEntityRenderObject(entityIn);
        return render != null && render.shouldRender(entityIn, camera, camX, camY, camZ);
    }

    public boolean renderEntityStatic(Entity entity, float partialTicks, boolean hideDebugBox) {
        if (entity.ticksExisted == 0) {
            entity.lastTickPosX = entity.posX;
            entity.lastTickPosY = entity.posY;
            entity.lastTickPosZ = entity.posZ;
        }

        double xPos = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double zPos = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double yPos = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        float f = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        int i = entity.getBrightnessForRender(partialTicks);

        if (entity.isBurning()) {
            i = 15728880;
        }

        int j = i % 65536;
        int k = i / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, j, k);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        return this.doRenderEntity(entity, xPos - this.renderPosX, zPos - this.renderPosY, yPos - this.renderPosZ, f, partialTicks, hideDebugBox);
    }

    public void renderWitherSkull(Entity entityIn, float partialTicks) {
        double d0 = entityIn.lastTickPosX + (entityIn.posX - entityIn.lastTickPosX) * partialTicks;
        double d1 = entityIn.lastTickPosY + (entityIn.posY - entityIn.lastTickPosY) * partialTicks;
        double d2 = entityIn.lastTickPosZ + (entityIn.posZ - entityIn.lastTickPosZ) * partialTicks;
        Render<Entity> render = this.getEntityRenderObject(entityIn);

        if (render != null && this.renderEngine != null) {
            int i = entityIn.getBrightnessForRender(partialTicks);
            int j = i % 65536;
            int k = i / 65536;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, j, k);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            render.renderName(entityIn, d0 - this.renderPosX, d1 - this.renderPosY, d2 - this.renderPosZ);
        }
    }

    public boolean renderEntityWithPosYaw(Entity entityIn, double x, double y, double z, float entityYaw, float partialTicks) {
        return this.doRenderEntity(entityIn, x, y, z, entityYaw, partialTicks, false);
    }

    public boolean doRenderEntity(Entity entity, double x, double y, double z, float entityYaw, float partialTicks, boolean hideDebugBox) {
        Render<Entity> render = null;

        try {
            render = this.getEntityRenderObject(entity);

            if (render != null && this.renderEngine != null) {
                try {
                    if (render instanceof RendererLivingEntity rendererLivingEntity) {
                        rendererLivingEntity.setRenderOutlines(this.renderOutlines);
                    }

                    if (CustomEntityModels.isActive()) {
                        this.renderRender = render;
                    }

                    render.doRender(entity, x, y, z, entityYaw, partialTicks);
                } catch (Throwable throwable) {
                    throw new ReportedException(CrashReport.makeCrashReport(throwable, "Rendering entity in world"));
                }

                try {
                    if (!this.renderOutlines) {
                        render.doRenderShadowAndFire(entity, x, y, z, entityYaw, partialTicks);
                    }
                } catch (Throwable throwable) {
                    throw new ReportedException(CrashReport.makeCrashReport(throwable, "Post-rendering entity in world"));
                }

                if (this.debugBoundingBox && !entity.isInvisible() && !hideDebugBox) {
                    try {
                        this.renderDebugBoundingBox(entity, x, y, z, entityYaw, partialTicks);
                    } catch (Throwable throwable) {
                        throw new ReportedException(CrashReport.makeCrashReport(throwable, "Rendering entity hitbox in world"));
                    }
                }
            } else return this.renderEngine == null;

            return true;
        } catch (Throwable throwable) {
            CrashReport report = CrashReport.makeCrashReport(throwable, "Rendering entity in world");
            CrashReportCategory category = report.makeCategory("Entity being rendered");
            entity.addEntityCrashInfo(category);

            category = report.makeCategory("Renderer details");
            category.addCrashSection("Assigned Renderer", render);
            category.addCrashSection("Location", CrashReportCategory.getCoordinateInfo(x, y, z));
            category.addCrashSection("Rotation", entityYaw);
            category.addCrashSection("Delta", partialTicks);
            throw new ReportedException(report);
        }
    }

    private void renderDebugBoundingBox(Entity entityIn, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!Shaders.isShadowPass) {
            GlStateManager.depthMask(false);
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
            GlStateManager.disableBlend();
            float f = entityIn.width / 2.0F;
            AxisAlignedBB axisalignedbb = entityIn.getEntityBoundingBox();
            AxisAlignedBB axisalignedbb1 = new AxisAlignedBB(axisalignedbb.minX - entityIn.posX + x, axisalignedbb.minY - entityIn.posY + y, axisalignedbb.minZ - entityIn.posZ + z, axisalignedbb.maxX - entityIn.posX + x, axisalignedbb.maxY - entityIn.posY + y, axisalignedbb.maxZ - entityIn.posZ + z);
            RenderGlobal.drawOutlinedBoundingBox(axisalignedbb1, 255, 255, 255, 255);

            if (entityIn instanceof EntityLivingBase) {
                float f1 = 0.01F;
                RenderGlobal.drawOutlinedBoundingBox(new AxisAlignedBB(x - f, y + entityIn.getEyeHeight() - 0.009999999776482582D, z - f, x + f, y + entityIn.getEyeHeight() + 0.009999999776482582D, z + f), 255, 0, 0, 255);
            }

            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            Vec3 vec3 = entityIn.getLook(partialTicks);
            worldrenderer.begin(3, DefaultVertexFormats.POSITION_COLOR);
            worldrenderer.pos(x, y + entityIn.getEyeHeight(), z).color(0, 0, 255, 255).endVertex();
            worldrenderer.pos(x + vec3.xCoord * 2.0D, y + entityIn.getEyeHeight() + vec3.yCoord * 2.0D, z + vec3.zCoord * 2.0D).color(0, 0, 255, 255).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
            GlStateManager.enableLighting();
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
        }
    }

    public void set(World worldIn) {
        this.worldObj = worldIn;
    }

    public double getDistanceToCamera(double x, double y, double z) {
        double d0 = x - this.viewerPosX;
        double d1 = y - this.viewerPosY;
        double d2 = z - this.viewerPosZ;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public FontRenderer getFontRenderer() {
        return this.textRenderer;
    }

    public void setRenderOutlines(boolean renderOutlinesIn) {
        this.renderOutlines = renderOutlinesIn;
    }

    public Map<Class, Render> getEntityRenderMap() {
        return this.entityRenderMap;
    }

    public void setEntityRenderMap(Map p_setEntityRenderMap_1_) {
        this.entityRenderMap = p_setEntityRenderMap_1_;
    }

    public Map<String, RenderPlayer> getSkinMap() {
        return Collections.unmodifiableMap(this.skinMap);
    }
}
