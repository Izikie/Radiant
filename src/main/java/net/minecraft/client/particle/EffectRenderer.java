package net.minecraft.client.particle;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.Entity;
import net.minecraft.src.Config;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class EffectRenderer {
    private static final ResourceLocation PARTICLE_TEXTURES = new ResourceLocation("textures/particle/particles.png");
    protected World worldObj;
    private final List<EntityFX>[][] fxLayers = new List[4][];
    private final List<EntityParticleEmitter> particleEmitters = new ArrayList<>();
    private final TextureManager renderer;
    private final Random rand = new Random();
    private final Int2ObjectOpenHashMap<IParticleFactory> particleTypes = new Int2ObjectOpenHashMap<>();

    public EffectRenderer(World worldIn, TextureManager rendererIn) {
        this.worldObj = worldIn;
        this.renderer = rendererIn;

        for (int i = 0; i < 4; ++i) {
            this.fxLayers[i] = new List[2];

            for (int j = 0; j < 2; ++j) {
                this.fxLayers[i][j] = new ArrayList<>();
            }
        }

        this.registerVanillaParticles();
    }

    private void registerVanillaParticles() {
        this.registerParticle(ParticleTypes.EXPLOSION_NORMAL.getParticleID(), new EntityExplodeFX.Factory());
        this.registerParticle(ParticleTypes.WATER_BUBBLE.getParticleID(), new EntityBubbleFX.Factory());
        this.registerParticle(ParticleTypes.WATER_SPLASH.getParticleID(), new EntitySplashFX.Factory());
        this.registerParticle(ParticleTypes.WATER_WAKE.getParticleID(), new EntityFishWakeFX.Factory());
        this.registerParticle(ParticleTypes.WATER_DROP.getParticleID(), new EntityRainFX.Factory());
        this.registerParticle(ParticleTypes.SUSPENDED.getParticleID(), new EntitySuspendFX.Factory());
        this.registerParticle(ParticleTypes.SUSPENDED_DEPTH.getParticleID(), new EntityAuraFX.Factory());
        this.registerParticle(ParticleTypes.CRIT.getParticleID(), new EntityCrit2FX.Factory());
        this.registerParticle(ParticleTypes.CRIT_MAGIC.getParticleID(), new EntityCrit2FX.MagicFactory());
        this.registerParticle(ParticleTypes.SMOKE_NORMAL.getParticleID(), new EntitySmokeFX.Factory());
        this.registerParticle(ParticleTypes.SMOKE_LARGE.getParticleID(), new EntityCritFX.Factory());
        this.registerParticle(ParticleTypes.SPELL.getParticleID(), new EntitySpellParticleFX.Factory());
        this.registerParticle(ParticleTypes.SPELL_INSTANT.getParticleID(), new EntitySpellParticleFX.InstantFactory());
        this.registerParticle(ParticleTypes.SPELL_MOB.getParticleID(), new EntitySpellParticleFX.MobFactory());
        this.registerParticle(ParticleTypes.SPELL_MOB_AMBIENT.getParticleID(), new EntitySpellParticleFX.AmbientMobFactory());
        this.registerParticle(ParticleTypes.SPELL_WITCH.getParticleID(), new EntitySpellParticleFX.WitchFactory());
        this.registerParticle(ParticleTypes.DRIP_WATER.getParticleID(), new EntityDropParticleFX.WaterFactory());
        this.registerParticle(ParticleTypes.DRIP_LAVA.getParticleID(), new EntityDropParticleFX.LavaFactory());
        this.registerParticle(ParticleTypes.VILLAGER_ANGRY.getParticleID(), new EntityHeartFX.AngryVillagerFactory());
        this.registerParticle(ParticleTypes.VILLAGER_HAPPY.getParticleID(), new EntityAuraFX.HappyVillagerFactory());
        this.registerParticle(ParticleTypes.TOWN_AURA.getParticleID(), new EntityAuraFX.Factory());
        this.registerParticle(ParticleTypes.NOTE.getParticleID(), new EntityNoteFX.Factory());
        this.registerParticle(ParticleTypes.PORTAL.getParticleID(), new EntityPortalFX.Factory());
        this.registerParticle(ParticleTypes.ENCHANTMENT_TABLE.getParticleID(), new EntityEnchantmentTableParticleFX.EnchantmentTable());
        this.registerParticle(ParticleTypes.FLAME.getParticleID(), new EntityFlameFX.Factory());
        this.registerParticle(ParticleTypes.LAVA.getParticleID(), new EntityLavaFX.Factory());
        this.registerParticle(ParticleTypes.FOOTSTEP.getParticleID(), new EntityFootStepFX.Factory());
        this.registerParticle(ParticleTypes.CLOUD.getParticleID(), new EntityCloudFX.Factory());
        this.registerParticle(ParticleTypes.REDSTONE.getParticleID(), new EntityReddustFX.Factory());
        this.registerParticle(ParticleTypes.SNOWBALL.getParticleID(), new EntityBreakingFX.SnowballFactory());
        this.registerParticle(ParticleTypes.SNOW_SHOVEL.getParticleID(), new EntitySnowShovelFX.Factory());
        this.registerParticle(ParticleTypes.SLIME.getParticleID(), new EntityBreakingFX.SlimeFactory());
        this.registerParticle(ParticleTypes.HEART.getParticleID(), new EntityHeartFX.Factory());
        this.registerParticle(ParticleTypes.BARRIER.getParticleID(), new Barrier.Factory());
        this.registerParticle(ParticleTypes.ITEM_CRACK.getParticleID(), new EntityBreakingFX.Factory());
        this.registerParticle(ParticleTypes.BLOCK_CRACK.getParticleID(), new EntityDiggingFX.Factory());
        this.registerParticle(ParticleTypes.BLOCK_DUST.getParticleID(), new EntityBlockDustFX.Factory());
        this.registerParticle(ParticleTypes.EXPLOSION_HUGE.getParticleID(), new EntityHugeExplodeFX.Factory());
        this.registerParticle(ParticleTypes.EXPLOSION_LARGE.getParticleID(), new EntityLargeExplodeFX.Factory());
        this.registerParticle(ParticleTypes.FIREWORKS_SPARK.getParticleID(), new EntityFirework.Factory());
        this.registerParticle(ParticleTypes.MOB_APPEARANCE.getParticleID(), new MobAppearance.Factory());
    }

    public void registerParticle(int id, IParticleFactory particleFactory) {
        this.particleTypes.put(id, particleFactory);
    }

    public void emitParticleAtEntity(Entity entityIn, ParticleTypes particleTypes) {
        this.particleEmitters.add(new EntityParticleEmitter(this.worldObj, entityIn, particleTypes));
    }

    public EntityFX spawnEffectParticle(int particleId, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        IParticleFactory iparticlefactory = this.particleTypes.get(particleId);

        if (iparticlefactory != null) {
            EntityFX entityfx = iparticlefactory.getEntityFX(particleId, this.worldObj, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, parameters);

            if (entityfx != null) {
                this.addEffect(entityfx);
                return entityfx;
            }
        }

        return null;
    }

    public void addEffect(EntityFX effect) {
        if (effect != null) {
            if (!(effect instanceof EntityFirework.SparkFX) || Config.isFireworkParticles()) {
                int i = effect.getFXLayer();
                int j = effect.getAlpha() != 1.0F ? 0 : 1;

                if (this.fxLayers[i][j].size() >= 4000) {
                    this.fxLayers[i][j].removeFirst();
                }

                this.fxLayers[i][j].add(effect);
            }
        }
    }

    public void updateEffects() {
        for (int i = 0; i < 4; ++i) {
            this.updateEffectLayer(i);
        }

        List<EntityParticleEmitter> list = new ArrayList<>();

        for (EntityParticleEmitter entityparticleemitter : this.particleEmitters) {
            entityparticleemitter.onUpdate();

            if (entityparticleemitter.isDead) {
                list.add(entityparticleemitter);
            }
        }

        this.particleEmitters.removeAll(list);
    }

    private void updateEffectLayer(int layer) {
        for (int i = 0; i < 2; ++i) {
            this.updateEffectAlphaLayer(this.fxLayers[layer][i]);
        }
    }

    private void updateEffectAlphaLayer(List<EntityFX> entitiesFX) {
        List<EntityFX> list = new ArrayList<>();
        long i = System.currentTimeMillis();
        int j = entitiesFX.size();

        for (int k = 0; k < entitiesFX.size(); k++) {
            EntityFX entityfx = entitiesFX.get(k);
            this.tickParticle(entityfx);

            if (entityfx.isDead) {
                list.add(entityfx);
            }

            --j;

            if (System.currentTimeMillis() > i + 20L) {
                break;
            }
        }

        if (j > 0) {
            int l = j;

            for (Iterator iterator = entitiesFX.iterator(); iterator.hasNext() && l > 0; --l) {
                EntityFX entityfx1 = (EntityFX) iterator.next();
                entityfx1.setDead();
                iterator.remove();
            }
        }

        entitiesFX.removeAll(list);
    }

    private void tickParticle(final EntityFX particle) {
        try {
            particle.onUpdate();
        } catch (Throwable throwable) {
            CrashReport report = CrashReport.makeCrashReport(throwable, "Ticking Particle");
            CrashReportCategory category = report.makeCategory("Particle being ticked");
            category.addCrashSectionCallable("Particle", particle::toString);
            category.addCrashSectionCallable("Particle Type", () -> switch (particle.getFXLayer()) {
                case 0 -> "MISC_TEXTURE";
                case 1 -> "TERRAIN_TEXTURE";
                case 3 -> "ENTITY_PARTICLE_TEXTURE";
                default -> "Unknown - " + particle.getFXLayer();
            });
            throw new ReportedException(report);
        }
    }

    public void renderParticles(Entity entityIn, float partialTicks) {
        float f = ActiveRenderInfo.getRotationX();
        float f1 = ActiveRenderInfo.getRotationZ();
        float f2 = ActiveRenderInfo.getRotationYZ();
        float f3 = ActiveRenderInfo.getRotationXY();
        float f4 = ActiveRenderInfo.getRotationXZ();
        EntityFX.interpPosX = entityIn.lastTickPosX + (entityIn.posX - entityIn.lastTickPosX) * partialTicks;
        EntityFX.interpPosY = entityIn.lastTickPosY + (entityIn.posY - entityIn.lastTickPosY) * partialTicks;
        EntityFX.interpPosZ = entityIn.lastTickPosZ + (entityIn.posZ - entityIn.lastTickPosZ) * partialTicks;
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.alphaFunc(516, 0.003921569F);
        Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(this.worldObj, entityIn, partialTicks);
        boolean flag = block.getMaterial() == Material.WATER;

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 2; ++j) {
                final int i_f = i;

                if (!this.fxLayers[i][j].isEmpty()) {
                    switch (j) {
                        case 0:
                            GlStateManager.depthMask(false);
                            break;

                        case 1:
                            GlStateManager.depthMask(true);
                    }

                    switch (i) {
                        case 1:
                            this.renderer.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                            break;

                        case 0:
                        default:
                            this.renderer.bindTexture(PARTICLE_TEXTURES);
                            break;
                    }

                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    Tessellator tessellator = Tessellator.get();
                    WorldRenderer worldrenderer = tessellator.getWorldRenderer();
                    worldrenderer.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);

                    for (int k = 0; k < this.fxLayers[i][j].size(); ++k) {
                        final EntityFX entityfx = this.fxLayers[i][j].get(k);

                        try {
                            if (flag || !(entityfx instanceof EntitySuspendFX)) {
                                entityfx.renderParticle(worldrenderer, entityIn, partialTicks, f, f4, f1, f2, f3);
                            }
                        } catch (Throwable throwable) {
                            CrashReport report = CrashReport.makeCrashReport(throwable, "Rendering Particle");
                            CrashReportCategory category = report.makeCategory("Particle being rendered");
                            category.addCrashSectionCallable("Particle", () -> entityfx.toString());
                            category.addCrashSectionCallable("Particle Type", () -> switch (i_f) {
                                case 0 -> "MISC_TEXTURE";
                                case 1 -> "TERRAIN_TEXTURE";
                                case 3 -> "ENTITY_PARTICLE_TEXTURE";
                                default -> "Unknown - " + i_f;
                            });
                            throw new ReportedException(report);
                        }
                    }

                    tessellator.draw();
                }
            }
        }

        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.alphaFunc(516, 0.1F);
    }

    public void renderLitParticles(Entity entityIn, float partialTick) {
        float f = 0.017453292F;
        float f1 = MathHelper.cos(entityIn.rotationYaw * 0.017453292F);
        float f2 = MathHelper.sin(entityIn.rotationYaw * 0.017453292F);
        float f3 = -f2 * MathHelper.sin(entityIn.rotationPitch * 0.017453292F);
        float f4 = f1 * MathHelper.sin(entityIn.rotationPitch * 0.017453292F);
        float f5 = MathHelper.cos(entityIn.rotationPitch * 0.017453292F);

        for (int i = 0; i < 2; ++i) {
            List<EntityFX> list = this.fxLayers[3][i];

            if (!list.isEmpty()) {
                Tessellator tessellator = Tessellator.get();
                WorldRenderer worldrenderer = tessellator.getWorldRenderer();

                for (EntityFX entityFX : list) {
                    entityFX.renderParticle(worldrenderer, entityIn, partialTick, f1, f5, f2, f3, f4);
                }
            }
        }
    }

    public void clearEffects(World worldIn) {
        this.worldObj = worldIn;

        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 2; ++j) {
                this.fxLayers[i][j].clear();
            }
        }

        this.particleEmitters.clear();
    }

    public void addBlockDestroyEffects(BlockPos pos, IBlockState state) {
        if (state.getBlock().getMaterial() != Material.AIR) {
            state = state.getBlock().getActualState(state, this.worldObj, pos);
            int l = 4;

            for (int i = 0; i < l; ++i) {
                for (int j = 0; j < l; ++j) {
                    for (int k = 0; k < l; ++k) {
                        double d0 = pos.getX() + (i + 0.5D) / l;
                        double d1 = pos.getY() + (j + 0.5D) / l;
                        double d2 = pos.getZ() + (k + 0.5D) / l;
                        this.addEffect((new EntityDiggingFX(this.worldObj, d0, d1, d2, d0 - pos.getX() - 0.5D, d1 - pos.getY() - 0.5D, d2 - pos.getZ() - 0.5D, state)).setBlockPos(pos));
                    }
                }
            }
        }
    }

    public void addBlockHitEffects(BlockPos pos, Direction side) {
        IBlockState iblockstate = this.worldObj.getBlockState(pos);
        Block block = iblockstate.getBlock();

        if (block.getRenderType() != -1) {
            int i = pos.getX();
            int j = pos.getY();
            int k = pos.getZ();
            float f = 0.1F;
            double d0 = i + this.rand.nextDouble() * (block.getBlockBoundsMaxX() - block.getBlockBoundsMinX() - (f * 2.0F)) + f + block.getBlockBoundsMinX();
            double d1 = j + this.rand.nextDouble() * (block.getBlockBoundsMaxY() - block.getBlockBoundsMinY() - (f * 2.0F)) + f + block.getBlockBoundsMinY();
            double d2 = k + this.rand.nextDouble() * (block.getBlockBoundsMaxZ() - block.getBlockBoundsMinZ() - (f * 2.0F)) + f + block.getBlockBoundsMinZ();

            if (side == Direction.DOWN) {
                d1 = j + block.getBlockBoundsMinY() - f;
            }

            if (side == Direction.UP) {
                d1 = j + block.getBlockBoundsMaxY() + f;
            }

            if (side == Direction.NORTH) {
                d2 = k + block.getBlockBoundsMinZ() - f;
            }

            if (side == Direction.SOUTH) {
                d2 = k + block.getBlockBoundsMaxZ() + f;
            }

            if (side == Direction.WEST) {
                d0 = i + block.getBlockBoundsMinX() - f;
            }

            if (side == Direction.EAST) {
                d0 = i + block.getBlockBoundsMaxX() + f;
            }

            this.addEffect((new EntityDiggingFX(this.worldObj, d0, d1, d2, 0.0D, 0.0D, 0.0D, iblockstate)).setBlockPos(pos).multiplyVelocity(0.2F).multipleParticleScaleBy(0.6F));
        }
    }

    public void moveToAlphaLayer(EntityFX effect) {
        this.moveToLayer(effect, 1, 0);
    }

    public void moveToNoAlphaLayer(EntityFX effect) {
        this.moveToLayer(effect, 0, 1);
    }

    private void moveToLayer(EntityFX effect, int layerFrom, int layerTo) {
        for (int i = 0; i < 4; ++i) {
            if (this.fxLayers[i][layerFrom].contains(effect)) {
                this.fxLayers[i][layerFrom].remove(effect);
                this.fxLayers[i][layerTo].add(effect);
            }
        }
    }

    public String getStatistics() {
        int i = 0;

        for (int j = 0; j < 4; ++j) {
            for (int k = 0; k < 2; ++k) {
                i += this.fxLayers[j][k].size();
            }
        }

        return "" + i;
    }
}
