package net.radiant.benchmark;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLeashKnot;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.chat.ChatComponentText;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.MapData;

import java.util.Arrays;

final class BenchmarkWorld {
    static final long SEED = 681889L;
    static final int LAYOUT_VERSION = 14;
    private static final int DYNAMIC_MAP_ID = 910;
    static final String DYNAMIC_MAP_NAME = "map_" + DYNAMIC_MAP_ID;
    static final int FLOOR_Y = 192;
    static final int GALLERY_X = 0;
    static final int ENTITY_X = 64;
    static final int PARTICLE_X = 128;
    static final int TRANSLUCENT_X = 192;
    static final int SKY_X = -64;
    static final int RAIN_X = 256;
    static final int SNOW_X = 320;
    static final int WEATHER_CAMERA_Z = 8;
    static final int WEATHER_SHELTER_BACK_Z = 22;
    static final int WATER_OVERLAY_X = RAIN_X - 10;
    static final int BLOCK_OVERLAY_X = RAIN_X + 10;
    static final int OVERLAY_FIXTURE_Z = 27;
    static final int DIMENSION_SKY_X = 0;
    static final int BLOCK_INTERACTION_X = RAIN_X + 10;
    static final BlockPos BREAKING_SLAB = new BlockPos(BLOCK_INTERACTION_X, FLOOR_Y + 2, WEATHER_CAMERA_Z + 3);
    static final BlockPos BREAKING_CHEST = new BlockPos(BLOCK_INTERACTION_X + 2, FLOOR_Y + 1, WEATHER_CAMERA_Z + 3);

    private BenchmarkWorld() {
    }

    // Called on the integrated server thread for the freshly generated benchmark save.
    static void build(WorldServer world) {
        GameRules rules = world.getGameRules();
        rules.setOrCreateGameRule("doDaylightCycle", "false");
        rules.setOrCreateGameRule("doMobSpawning", "false");
        rules.setOrCreateGameRule("randomTickSpeed", "0");
        world.setWorldTime(6000);
        world.getWorldInfo().setRaining(false);
        world.getWorldInfo().setThundering(false);
        world.getWorldInfo().setCleanWeatherTime(Integer.MAX_VALUE);

        buildFloor(world, GALLERY_X);
        buildFloor(world, ENTITY_X);
        buildFloor(world, PARTICLE_X);
        buildFloor(world, TRANSLUCENT_X);
        world.setBlockState(new BlockPos(SKY_X, FLOOR_Y, 0), Blocks.STONEBRICK.getDefaultState(), 2);
        buildGallery(world);
        buildEntityScene(world);
        buildTranslucencyScene(world);
        buildPrecipitationScene(world, RAIN_X, BiomeGenBase.PLAINS);
        buildPrecipitationScene(world, SNOW_X, BiomeGenBase.ICE_PLAINS);
        buildCameraOverlayFixtures(world);
        world.setBlockState(BREAKING_SLAB.down(), Blocks.STONEBRICK.getDefaultState(), 2);
        world.setBlockState(BREAKING_SLAB, Blocks.STONE_SLAB.getStateFromMeta(8), 2);
        world.setBlockState(BREAKING_CHEST, Blocks.CHEST.getDefaultState(), 2);
    }

    private static void buildCameraOverlayFixtures(WorldServer world) {
        var bricks = Blocks.STONEBRICK.getDefaultState();
        var water = Blocks.WATER.getDefaultState();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -2; dz <= 6; dz++) {
                boolean edge = Math.abs(dx) == 3 || dz == -2 || dz == 6;
                for (int dy = 1; dy <= 3; dy++) {
                    world.setBlockState(new BlockPos(WATER_OVERLAY_X + dx, FLOOR_Y + dy, OVERLAY_FIXTURE_Z + dz),
                            edge ? bricks : water, 2);
                }
            }
        }
        // Cover the adjacent columns at eye level so push-out physics keeps the test camera in place.
        var brickBlock = Blocks.BRICK_BLOCK.getDefaultState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(new BlockPos(BLOCK_OVERLAY_X + dx, FLOOR_Y + 2, OVERLAY_FIXTURE_Z + dz), brickBlock, 2);
            }
        }
    }

    static void buildDimensionSky(WorldServer world) {
        buildFloor(world, DIMENSION_SKY_X);
        var pillar = (world.provider.getDimensionId() == -1 ? Blocks.NETHERRACK : Blocks.END_STONE).getDefaultState();
        for (int z = 7; z <= 31; z += 8) {
            for (int x = -5; x <= 5; x += 10) {
                for (int y = 1; y <= 5; y++) {
                    world.setBlockState(new BlockPos(DIMENSION_SKY_X + x, FLOOR_Y + y, z), pillar, 2);
                }
            }
        }
    }

    private static void buildFloor(WorldServer world, int center) {
        for (int x = center - 16; x <= center + 16; x++) {
            for (int z = -3; z <= 34; z++) {
                world.setBlockState(new BlockPos(x, FLOOR_Y, z), Blocks.STONEBRICK.getDefaultState(), 2);
            }
        }
    }

    private static void buildPrecipitationScene(WorldServer world, int center, BiomeGenBase biome) {
        buildFloor(world, center);
        for (int chunkX = (center - 16) >> 4; chunkX <= (center + 16) >> 4; chunkX++) {
            for (int chunkZ = -1; chunkZ <= 2; chunkZ++) {
                Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
                Arrays.fill(chunk.getBiomeArray(), (byte) biome.biomeID);
                chunk.setChunkModified();
            }
        }

        // A roof and back wall make the precipitation height and depth tests visible in the camera sweep.
        var bricks = Blocks.STONEBRICK.getDefaultState();
        for (int x = center - 4; x <= center + 4; x++) {
            for (int z = 16; z <= WEATHER_SHELTER_BACK_Z; z++) {
                world.setBlockState(new BlockPos(x, FLOOR_Y + 4, z), bricks, 2);
            }
            for (int y = 1; y < 4; y++) {
                world.setBlockState(new BlockPos(x, FLOOR_Y + y, WEATHER_SHELTER_BACK_Z), bricks, 2);
            }
        }
        for (int y = 1; y < 4; y++) {
            world.setBlockState(new BlockPos(center - 4, FLOOR_Y + y, 16), bricks, 2);
            world.setBlockState(new BlockPos(center + 4, FLOOR_Y + y, 16), bricks, 2);
        }
    }

    private static void buildGallery(WorldServer world) {
        Block[] materials = {
                Blocks.STAINED_HARDENED_CLAY, Blocks.OAK_STAIRS, Blocks.OAK_FENCE, Blocks.GLOWSTONE,
                Blocks.GLASS, Blocks.STAINED_GLASS, Blocks.GLASS_PANE, Blocks.LEAVES,
                Blocks.WOOL, Blocks.STONE_SLAB, Blocks.LOG, Blocks.SEA_LANTERN,
                Blocks.ICE, Blocks.STAINED_GLASS_PANE, Blocks.IRON_BARS, Blocks.LEAVES_2
        };
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                Block material = materials[(row % 2) * 8 + column];
                int x = GALLERY_X - 14 + column * 4;
                int z = 5 + row * 4;
                int metadata = material == Blocks.STAINED_GLASS || material == Blocks.STAINED_HARDENED_CLAY
                        || material == Blocks.STAINED_GLASS_PANE || material == Blocks.WOOL
                        ? row + 4 : 0;
                for (int y = 1; y <= 2 + row % 3; y++) {
                    world.setBlockState(new BlockPos(x, FLOOR_Y + y, z), material.getStateFromMeta(metadata), 2);
                    world.setBlockState(new BlockPos(x + 1, FLOOR_Y + y, z), material.getStateFromMeta(metadata), 2);
                }
            }
        }
    }

    private static void buildTranslucencyScene(WorldServer world) {
        int center = TRANSLUCENT_X;
        buildFluidPool(world, center - 6, center + 2, 4, 12, Blocks.WATER, Blocks.SEA_LANTERN, Blocks.PRISMARINE);
        buildFluidPool(world, center + 3, center + 9, 6, 12, Blocks.LAVA, Blocks.NETHER_BRICK, Blocks.NETHERRACK);

        for (int x = center + 4; x <= center + 7; x++) {
            world.setBlockState(new BlockPos(x, FLOOR_Y + 1, 14), Blocks.ICE.getDefaultState(), 2);
            world.setBlockState(new BlockPos(x, FLOOR_Y + 2, 14), Blocks.ICE.getDefaultState(), 2);
            world.setBlockState(new BlockPos(x, FLOOR_Y + 1, 15), Blocks.GLOWSTONE.getDefaultState(), 2);
        }

        // Put the overlapping panels in adjacent sections so section order and alpha blending are both visible.
        for (int x = center - 4; x <= center + 4; x++) {
            for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 4; y++) {
                world.setBlockState(new BlockPos(x, y, 15), Blocks.STAINED_GLASS.getStateFromMeta(DyeColor.CYAN.getMetadata()), 2);
                world.setBlockState(new BlockPos(x + 2, y, 17), Blocks.STAINED_GLASS.getStateFromMeta(DyeColor.MAGENTA.getMetadata()), 2);
                Block backdrop = ((x + y) & 1) == 0 ? Blocks.QUARTZ_BLOCK : Blocks.STONEBRICK;
                world.setBlockState(new BlockPos(x + 1, y, 20), backdrop.getDefaultState(), 2);
            }
        }
    }

    private static void buildFluidPool(WorldServer world, int minX, int maxX, int minZ, int maxZ,
                                       Block fluid, Block lightTile, Block darkTile) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                Block base = ((x + z) & 1) == 0 ? lightTile : darkTile;
                world.setBlockState(new BlockPos(x, FLOOR_Y, z), base.getDefaultState(), 2);
                world.setBlockState(new BlockPos(x, FLOOR_Y + 1, z),
                        (edge ? Blocks.STONEBRICK : fluid).getDefaultState(), 2);
            }
        }
    }

    private static void buildEntityScene(WorldServer world) {
        placeDynamicMap(world);
        placeArmoredStand(world, ENTITY_X + 0.5, true);
        placeArmoredStand(world, ENTITY_X + 2, false);
        EntitySpider spider = new EntitySpider(world);
        spider.setNoAI(true);
        spider.enablePersistence();
        spider.setLocationAndAngles(ENTITY_X - 2.5, FLOOR_Y + 1, 4.0, 180, 0);
        spider.setRotationYawHead(180);
        spider.setRenderYawOffset(180);
        world.spawnEntityInWorld(spider);
        EntityArmorStand hiddenStand = new EntityArmorStand(world);
        hiddenStand.setLocationAndAngles(ENTITY_X + 6.5, FLOOR_Y + 1, 18.5, 180, 0);
        hiddenStand.setCustomNameTag("Behind the wall");
        hiddenStand.setAlwaysRenderNameTag(true);
        world.spawnEntityInWorld(hiddenStand);
        BlockPos frontFence = new BlockPos(ENTITY_X + 6, FLOOR_Y + 1, 5);
        BlockPos rearFence = new BlockPos(ENTITY_X - 5, FLOOR_Y + 1, 9);
        world.setBlockState(frontFence, Blocks.OAK_FENCE.getDefaultState(), 2);
        world.setBlockState(rearFence, Blocks.OAK_FENCE.getDefaultState(), 2);
        EntityLeashKnot frontKnot = EntityLeashKnot.createKnot(world, frontFence);
        EntityLeashKnot rearKnot = EntityLeashKnot.createKnot(world, rearFence);
        world.setBlockState(new BlockPos(ENTITY_X - 3, FLOOR_Y + 1, 8), Blocks.STONEBRICK.getDefaultState(), 2);
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 10; column++) {
                int x = ENTITY_X - 13 + column * 3;
                int z = 5 + row * 3;
                EntityLiving animal = (row + column) % 2 == 0 ? new EntityCow(world) : new EntitySheep(world);
                if (animal instanceof EntitySheep sheep) {
                    sheep.setFleeceColor(DyeColor.byMetadata((row * 10 + column) % 16));
                }
                animal.setNoAI(true);
                animal.enablePersistence();
                animal.setCustomNameTag("Exhibit " + (row * 10 + column + 1));
                animal.setAlwaysRenderNameTag(true);
                animal.setLocationAndAngles(x + 0.5, FLOOR_Y + 1, z + 0.5, 180, 0);
                if (row == 0 && (column == 3 || column == 6)) {
                    int burnSeconds = 1200;
                    animal.addPotionEffect(new PotionEffect(Potion.FIRE_RESISTANCE.id, burnSeconds * 20, 0, false, false));
                    animal.setFire(burnSeconds);
                }
                world.spawnEntityInWorld(animal);
                if (row == 0 && column == 5) {
                    animal.setLeashedToEntity(frontKnot, true);
                } else if (row == 1 && column == 4) {
                    animal.setLeashedToEntity(rearKnot, true);
                }

                BlockPos prop = new BlockPos(x, FLOOR_Y + 1, z + 1);
                if (row % 2 == 0) {
                    world.setBlockState(prop, Blocks.CHEST.getDefaultState(), 2);
                } else {
                    world.setBlockState(prop, Blocks.STANDING_SIGN.getStateFromMeta(8), 2);
                    if (world.getTileEntity(prop) instanceof TileEntitySign sign) {
                        sign.signText[1] = new ChatComponentText("Exhibit " + (column + 1));
                        sign.markDirty();
                    }
                }
            }
        }

        // Leave a gap between two walls so a camera sweep sees both exposed and occluded exhibits.
        for (int offset = -15; offset <= 15; offset++) {
            if (Math.abs(offset) < 4) {
                continue;
            }
            int x = ENTITY_X + offset;
            for (int y = 1; y <= 4; y++) {
                world.setBlockState(new BlockPos(x, FLOOR_Y + y, 16), Blocks.STONEBRICK.getDefaultState(), 2);
            }
            if (offset % 2 == 0) {
                BlockPos pos = new BlockPos(x, FLOOR_Y + 3, 15);
                EntityPainting painting = new EntityPainting(world, pos, Direction.NORTH, "Kebab");
                if (painting.onValidSurface()) {
                    world.spawnEntityInWorld(painting);
                }
            }
        }
    }

    private static void placeDynamicMap(WorldServer world) {
        BlockPos position = new BlockPos(ENTITY_X - 5, FLOOR_Y + 2, 8);
        world.setBlockState(position.south(), Blocks.STONEBRICK.getDefaultState(), 2);
        MapData data = new MapData(DYNAMIC_MAP_NAME);
        paintDynamicMap(data, false);
        data.markDirty();
        world.setItemData(data.mapName, data);
        EntityItemFrame frame = new EntityItemFrame(world, position, Direction.NORTH);
        frame.setDisplayedItem(new ItemStack(Items.FILLED_MAP, 1, DYNAMIC_MAP_ID));
        world.spawnEntityInWorld(frame);
    }

    static void paintDynamicMap(MapData data, boolean alternate) {
        MapColor color = alternate ? MapColor.WATER_COLOR : MapColor.GRASS_COLOR;
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                data.colors[y * 128 + x] = (byte) (color.colorIndex * 4 + (((x / 16) ^ (y / 16)) & 1) + 1);
            }
        }
    }

    private static void placeArmoredStand(WorldServer world, double x, boolean enchanted) {
        EntityArmorStand stand = new EntityArmorStand(world);
        stand.setLocationAndAngles(x, FLOOR_Y + 1, 4, 180, 0);
        if (enchanted) {
            stand.setCustomNameTag("Enchanted armor");
            stand.setAlwaysRenderNameTag(true);
        }
        ItemArmor[] pieces = {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS};
        for (ItemArmor piece : pieces) {
            ItemStack stack = new ItemStack(piece);
            if (enchanted) {
                stack.addEnchantment(Enchantment.PROTECTION, 1);
            }
            stand.setCurrentItemOrArmor(4 - piece.armorType, stack);
        }
        world.spawnEntityInWorld(stand);
    }
}
