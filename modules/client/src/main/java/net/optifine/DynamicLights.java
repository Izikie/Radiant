package net.optifine;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class DynamicLights {
    private static final DynamicLightsMap DYNAMIC_LIGHTS_MAP = new DynamicLightsMap();
    private static final Object2IntMap<Entity> ENTITY_LIGHT_LEVELS = new Object2IntOpenHashMap<>();
    private static final Object2IntMap<Item> ITEM_LIGHT_LEVELS = new Object2IntOpenHashMap<>();
    private static final double MAX_DIST = 7.5D;
    private static final double MAX_DIST_SQ = 56.25D;
    private static final int LIGHT_LEVEL_MAX = 15;
    private static final int LIGHT_LEVEL_FIRE = 15;
    private static final int LIGHT_LEVEL_BLAZE = 10;
    private static final int LIGHT_LEVEL_MAGMA_CUBE = 8;
    private static final int LIGHT_LEVEL_MAGMA_CUBE_CORE = 13;
    private static final int LIGHT_LEVEL_GLOWSTONE_DUST = 8;
    private static final int LIGHT_LEVEL_PRISMARINE_CRYSTALS = 8;
    private static long timeUpdateMs = 0L;
    private static boolean initialized;

    public static void entityAdded(Entity entityIn, RenderGlobal renderGlobal) {
    }

    public static void entityRemoved(Entity entityIn, RenderGlobal renderGlobal) {
        synchronized (DYNAMIC_LIGHTS_MAP) {
            DynamicLight dynamiclight = DYNAMIC_LIGHTS_MAP.remove(entityIn.getEntityId());

            if (dynamiclight != null) {
                dynamiclight.updateLitChunks(renderGlobal);
            }
        }
    }

    public static void update(RenderGlobal renderGlobal) {
        long i = System.currentTimeMillis();

        if (i >= timeUpdateMs + 50L) {
            timeUpdateMs = i;

            if (!initialized) {
                initialize();
            }

            synchronized (DYNAMIC_LIGHTS_MAP) {
                updateMapDynamicLights(renderGlobal);

                if (DYNAMIC_LIGHTS_MAP.size() > 0) {
                    List<DynamicLight> list = DYNAMIC_LIGHTS_MAP.valueList();

                    for (DynamicLight dynamicLight : list) {
                        dynamicLight.update(renderGlobal);
                    }
                }
            }
        }
    }

    private static void initialize() {
        initialized = true;
        ENTITY_LIGHT_LEVELS.clear();
        ITEM_LIGHT_LEVELS.clear();

        if (!ENTITY_LIGHT_LEVELS.isEmpty()) {
            Log.info("DynamicLights entities: " + ENTITY_LIGHT_LEVELS.size());
        }

        if (!ITEM_LIGHT_LEVELS.isEmpty()) {
            Log.info("DynamicLights items: " + ITEM_LIGHT_LEVELS.size());
        }
    }

    private static void updateMapDynamicLights(RenderGlobal renderGlobal) {
        World world = renderGlobal.getWorld();

        if (world != null) {
            for (Entity entity : world.getLoadedEntityList()) {
                int i = getLightLevel(entity);

                if (i > 0) {
                    int j = entity.getEntityId();
                    DynamicLight dynamiclight = DYNAMIC_LIGHTS_MAP.get(j);

                    if (dynamiclight == null) {
                        dynamiclight = new DynamicLight(entity);
                        DYNAMIC_LIGHTS_MAP.put(j, dynamiclight);
                    }
                } else {
                    int k = entity.getEntityId();
                    DynamicLight dynamiclight1 = DYNAMIC_LIGHTS_MAP.remove(k);

                    if (dynamiclight1 != null) {
                        dynamiclight1.updateLitChunks(renderGlobal);
                    }
                }
            }
        }
    }

    public static int getCombinedLight(BlockPos pos, int combinedLight) {
        double d0 = getLightLevel(pos);
        combinedLight = getCombinedLight(d0, combinedLight);
        return combinedLight;
    }

    public static int getCombinedLight(Entity entity, int combinedLight) {
        double d0 = getLightLevel(entity);
        combinedLight = getCombinedLight(d0, combinedLight);
        return combinedLight;
    }

    public static int getCombinedLight(double lightPlayer, int combinedLight) {
        if (lightPlayer > 0.0D) {
            int i = (int) (lightPlayer * 16.0D);
            int j = combinedLight & 255;

            if (i > j) {
                combinedLight = combinedLight & -256;
                combinedLight = combinedLight | i;
            }
        }

        return combinedLight;
    }

    public static double getLightLevel(BlockPos pos) {
        double d0 = 0.0D;

        synchronized (DYNAMIC_LIGHTS_MAP) {
            List<DynamicLight> list = DYNAMIC_LIGHTS_MAP.valueList();
            int i = list.size();

            for (DynamicLight dynamicLight : list) {
                int k = dynamicLight.getLastLightLevel();

                if (k > 0) {
                    double d1 = dynamicLight.getLastPosX();
                    double d2 = dynamicLight.getLastPosY();
                    double d3 = dynamicLight.getLastPosZ();
                    double d4 = pos.getX() - d1;
                    double d5 = pos.getY() - d2;
                    double d6 = pos.getZ() - d3;
                    double d7 = d4 * d4 + d5 * d5 + d6 * d6;

                    if (dynamicLight.isUnderwater() && !Config.isClearWater()) {
                        k = Math.clamp(k - 2, 0, 15);
                        d7 *= 2.0D;
                    }

                    if (d7 <= 56.25D) {
                        double d8 = Math.sqrt(d7);
                        double d9 = 1.0D - d8 / 7.5D;
                        double d10 = d9 * k;

                        if (d10 > d0) {
                            d0 = d10;
                        }
                    }
                }
            }
        }

        return Math.clamp(d0, 0.0D, 15.0D);
    }

    public static int getLightLevel(ItemStack itemStack) {
        if (itemStack == null) {
            return 0;
        } else {
            Item item = itemStack.getItem();

            if (item instanceof ItemBlock itemblock) {
                Block block = itemblock.getBlock();

                if (block != null) {
                    return block.getLightValue();
                }
            }

            if (item == Items.LAVA_BUCKET) {
                return Blocks.LAVA.getLightValue();
            } else if (item != Items.BLAZE_ROD && item != Items.BLAZE_POWDER) {
                if (item == Items.GLOWSTONE_DUST) {
                    return 8;
                } else if (item == Items.PRISMARINE_CRYSTALS) {
                    return 8;
                } else if (item == Items.MAGMA_CREAM) {
                    return 8;
                } else if (item == Items.NETHER_STAR) {
                    return Blocks.BEACON.getLightValue() / 2;
                } else {
                    if (!ITEM_LIGHT_LEVELS.isEmpty()) {
                        Integer integer = ITEM_LIGHT_LEVELS.get(item);

                        if (integer != null) {
                            return integer;
                        }
                    }

                    return 0;
                }
            } else {
                return 10;
            }
        }
    }

    public static int getLightLevel(Entity entity) {
        if (entity == Config.getMinecraft().getRenderViewEntity() && !Config.isDynamicHandLight()) {
            return 0;
        } else {
            if (entity instanceof EntityPlayer entityplayer) {

                if (entityplayer.isSpectator()) {
                    return 0;
                }
            }

            if (entity.isBurning()) {
                return 15;
            } else {
                if (!ENTITY_LIGHT_LEVELS.isEmpty()) {
                    Integer integer = ENTITY_LIGHT_LEVELS.get(entity.getClass());

                    if (integer != null) {
                        return integer;
                    }
                }

                switch (entity) {
                    case EntityFireball entityFireball -> {
                        return 15;
                    }
                    case EntityTNTPrimed entityTNTPrimed -> {
                        return 15;
                    }
                    case EntityBlaze entityblaze -> {
                        return entityblaze.func_70845_n() ? 15 : 10;
                    }
                    case EntityMagmaCube entitymagmacube -> {
                        return entitymagmacube.squishFactor > 0.6D ? 13 : 8;
                    }
                    default -> {
                        if (entity instanceof EntityCreeper entitycreeper) {

                            if (entitycreeper.getCreeperFlashIntensity(0.0F) > 0.001D) {
                                return 15;
                            }
                        }

                        if (entity instanceof EntityLivingBase entitylivingbase) {
                            ItemStack itemstack2 = entitylivingbase.getHeldItem();
                            int i = getLightLevel(itemstack2);
                            ItemStack itemstack1 = entitylivingbase.getEquipmentInSlot(4);
                            int j = getLightLevel(itemstack1);
                            return Math.max(i, j);
                        } else if (entity instanceof EntityItem entityitem) {
                            ItemStack itemstack = getItemStack(entityitem);
                            return getLightLevel(itemstack);
                        } else {
                            return 0;
                        }
                    }
                }
            }
        }
    }

    public static void removeLights(RenderGlobal renderGlobal) {
        synchronized (DYNAMIC_LIGHTS_MAP) {
            List<DynamicLight> list = DYNAMIC_LIGHTS_MAP.valueList();

            for (DynamicLight dynamicLight : list) {
                dynamicLight.updateLitChunks(renderGlobal);
            }

            DYNAMIC_LIGHTS_MAP.clear();
        }
    }

    public static void clear() {
        synchronized (DYNAMIC_LIGHTS_MAP) {
            DYNAMIC_LIGHTS_MAP.clear();
        }
    }

    public static int getCount() {
        synchronized (DYNAMIC_LIGHTS_MAP) {
            return DYNAMIC_LIGHTS_MAP.size();
        }
    }

    public static ItemStack getItemStack(EntityItem entityItem) {
        return entityItem.getDataWatcher().getWatchableObjectItemStack(10);
    }
}
