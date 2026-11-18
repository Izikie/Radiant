package net.radiant.benchmark;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Blocks;
import net.minecraft.item.DyeColor;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.chat.ChatComponentText;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldServer;

final class BenchmarkWorld {
    static final long SEED = 681889L;
    static final int LAYOUT_VERSION = 4;
    static final int FLOOR_Y = 192;
    static final int GALLERY_X = 0;
    static final int ENTITY_X = 64;
    static final int PARTICLE_X = 128;
    static final int TRANSLUCENT_X = 192;
    static final int SKY_X = -64;

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
    }

    private static void buildFloor(WorldServer world, int center) {
        for (int x = center - 16; x <= center + 16; x++) {
            for (int z = -3; z <= 34; z++) {
                world.setBlockState(new BlockPos(x, FLOOR_Y, z), Blocks.STONEBRICK.getDefaultState(), 2);
            }
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
                world.spawnEntityInWorld(animal);

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
}
