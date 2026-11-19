package net.radiant.benchmark;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.impl.play.server.S03PacketTimeUpdate;
import net.minecraft.network.packet.impl.play.server.S2BPacketChangeGameState;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.ParticleTypes;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.radiant.lwjgl.opengl.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

final class BenchmarkSuite {
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkSuite.class);
    private static final long PREPARATION_TIMEOUT = TimeUnit.SECONDS.toNanos(90);
    private static final int EXHIBIT_Y = BenchmarkWorld.FLOOR_Y + 1;
    private static final int PARTICLE_WORKLOAD_VERSION = 2;
    private static final Phase[] ROUTE = Phase.values();

    private final IntegratedServer server;
    private final boolean exitAfterSuite;
    private final GameSettings.Perspective originalPerspective;
    private final boolean originalPauseOnLostFocus;
    private final int width;
    private final int height;
    private final int seconds = Math.clamp(Integer.getInteger("radiant.benchmark.seconds", 10), 1, 180);
    private final int warmup = Math.clamp(Integer.getInteger("radiant.benchmark.warmup", 3), 0, 120);
    private final JsonArray reports = new JsonArray();
    private final JsonObject report = new JsonObject();

    private WorldClient world;
    private String playerName;
    private ListenableFuture<?> pending;
    private GuiContainerCreative inventory;
    private int routeIndex;
    private int particleTick;
    private int particleSpawnAttempts;
    private boolean arrived;
    private long arrivalNanos;
    private int unfocusedFrames;
    private int inventoryOpens;
    private Stage stage = Stage.JOINING;
    private long stageStarted = System.nanoTime();

    private BenchmarkSuite(Minecraft mc, String folder, boolean exitAfterSuite) {
        server = mc.getIntegratedServer();
        this.exitAfterSuite = exitAfterSuite;
        originalPerspective = mc.gameSettings.thirdPersonView;
        originalPauseOnLostFocus = mc.gameSettings.pauseOnLostFocus;
        if (exitAfterSuite) {
            mc.gameSettings.pauseOnLostFocus = false;
        }
        width = mc.displayWidth;
        height = mc.displayHeight;
        report.addProperty("schemaVersion", 1);
        report.addProperty("suite", System.getProperty("radiant.benchmark.scenario", "rendering"));
        report.addProperty("worldFolder", folder);
        report.addProperty("worldSeed", BenchmarkWorld.SEED);
        report.addProperty("layoutVersion", BenchmarkWorld.LAYOUT_VERSION);
        report.add("phases", reports);
    }

    static BenchmarkSuite launch(Minecraft mc, boolean exitAfterSuite) {
        String folder = "benchmark-" + UUID.randomUUID();
        WorldSettings settings = new WorldSettings(
                BenchmarkWorld.SEED, WorldSettings.GameType.CREATIVE, false, false, WorldType.DEFAULT);
        mc.launchIntegratedServer(folder, "Radiant Benchmark", settings);
        LOGGER.info("Preparing benchmark world {}", folder);
        return new BenchmarkSuite(mc, folder, exitAfterSuite);
    }

    void endFrame(Minecraft mc, long frameStart) {
        long now = System.nanoTime();
        String interruption = interruption(mc);
        if (interruption != null) {
            ClientBenchmark.stopSuite(mc, interruption);
            return;
        }
        if (pending != null && pending.isDone()) {
            try {
                Futures.getDone(pending);
                pending = null;
            } catch (ExecutionException | CancellationException e) {
                LOGGER.warn("Benchmark server task failed", e);
                ClientBenchmark.stopSuite(mc, "server task failed");
                return;
            }
        }
        if ((stage == Stage.JOINING || stage == Stage.BUILDING || stage == Stage.TRAVELLING)
                && now - stageStarted >= PREPARATION_TIMEOUT) {
            ClientBenchmark.stopSuite(mc, "timed out during " + stage);
            return;
        }

        Phase phase = ROUTE[routeIndex];
        Phase warmupLocation = phase == Phase.TERRAIN_STREAMING ? Phase.BLOCK_GALLERY : phase;
        if ((stage == Stage.WARMING_UP && !atDestination(mc, warmupLocation))
                || (stage == Stage.CAPTURING && phase != Phase.TERRAIN_STREAMING && !atDestination(mc, phase))) {
            LOGGER.warn("Benchmark player moved to ({}, {}, {}) during {}",
                    mc.player.posX, mc.player.posY, mc.player.posZ, phase.scenario);
            ClientBenchmark.stopSuite(mc, "player moved away from " + phase.scenario);
            return;
        }
        switch (stage) {
            case JOINING -> {
                if (mc.world != null && mc.player != null && mc.currentScreen == null) {
                    world = mc.world;
                    playerName = mc.player.getName();
                    mc.gameSettings.thirdPersonView = GameSettings.Perspective.FIRST_PERSON;
                    pending = server.addScheduledTask(() -> BenchmarkWorld.build(server.worldServerForDimension(0)));
                    enter(Stage.BUILDING, now);
                }
            }
            case BUILDING -> {
                if (pending == null) {
                    preparePhase(phase, now);
                }
            }
            case TRAVELLING -> {
                BlockPos floor = new BlockPos(warmupLocation.x, BenchmarkWorld.FLOOR_Y, warmupLocation.z);
                if (pending == null && atDestination(mc, warmupLocation) && world.getWorldTime() == warmupLocation.worldTime
                        && world.getBlockState(floor).getBlock() == Blocks.STONEBRICK
                        && world.getRainStrength(1.0F) == warmupLocation.rainStrength()
                        && (warmupLocation.precipitationBiome == null || world.getBiomeGenForCoords(floor) == warmupLocation.precipitationBiome)
                        && hasExpectedHeldItem(mc, warmupLocation)) {
                    enter(Stage.WARMING_UP, now);
                }
            }
            case WARMING_UP -> {
                if (now - stageStarted >= TimeUnit.SECONDS.toNanos(warmup)) {
                    beginCapture(mc, phase);
                    if (phase == Phase.TERRAIN_STREAMING) {
                        // Start sampling from the settled staging area before visiting cold terrain.
                        teleport(phase);
                    }
                }
            }
            case CAPTURING -> {
                if (!arrived && atDestination(mc, phase)) {
                    arrived = true;
                    arrivalNanos = now - stageStarted;
                }
                if (!Display.isActive()) {
                    unfocusedFrames++;
                }
                JsonObject result = ClientBenchmark.sample(frameStart, now);
                if (result != null) {
                    result.addProperty("destinationReached", arrived);
                    result.addProperty("unfocusedFrames", unfocusedFrames);
                    if (phase == Phase.TERRAIN_STREAMING && arrived) {
                        result.addProperty("teleportArrivalMs", arrivalNanos / 1_000_000.0);
                    }
                    if (phase == Phase.CREATIVE_INVENTORY) {
                        result.addProperty("inventoryOpens", inventoryOpens);
                    }
                    if (phase == Phase.PARTICLES) {
                        result.addProperty("particleWorkloadVersion", PARTICLE_WORKLOAD_VERSION);
                        result.addProperty("particleSpawnAttempts", particleSpawnAttempts);
                    }
                    if (phase.precipitationBiome != null) {
                        result.addProperty("precipitationBiomeId", world.getBiomeGenForCoords(mc.player.getPosition()).biomeID);
                        result.addProperty("precipitationStrength", world.getRainStrength(1.0F));
                    }
                    reports.add(result);
                    closeInventory(mc);
                    routeIndex++;
                    if (routeIndex == ROUTE.length) {
                        ClientBenchmark.stopSuite(mc, "complete");
                    } else {
                        preparePhase(ROUTE[routeIndex], now);
                    }
                }
            }
        }
    }

    private String interruption(Minecraft mc) {
        if (mc.getIntegratedServer() != server || (world != null && (mc.world != world || mc.player == null))) {
            return "world changed";
        }
        if (mc.displayWidth != width || mc.displayHeight != height) {
            return "window size changed";
        }
        if (world != null && mc.currentScreen != inventory) {
            return "unexpected screen opened or closed";
        }
        if (!exitAfterSuite && world != null && !Display.isActive()) {
            return "window lost focus";
        }
        return null;
    }

    private void preparePhase(Phase phase, long now) {
        arrived = false;
        particleTick = 0;
        particleSpawnAttempts = 0;
        unfocusedFrames = 0;
        inventoryOpens = 0;
        LOGGER.info("Benchmark preparing {}", phase.scenario);
        enter(Stage.TRAVELLING, now);
        teleport(phase == Phase.TERRAIN_STREAMING ? Phase.BLOCK_GALLERY : phase);
    }

    private void teleport(Phase phase) {
        pending = server.addScheduledTask(() -> {
            EntityPlayerMP player = server.getConfigurationManager().getPlayerByUsername(playerName);
            if (player == null) {
                throw new IllegalStateException("Benchmark player disconnected");
            }
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
            var serverWorld = player.getServerForPlayer();
            if (serverWorld.getWorldTime() != phase.worldTime) {
                serverWorld.setWorldTime(phase.worldTime);
                player.playerNetServerHandler.sendPacket(new S03PacketTimeUpdate(serverWorld.getTotalWorldTime(), phase.worldTime, false));
            }
            setWeather(serverWorld, player, phase.rainStrength());
            if (phase == Phase.FIRST_PERSON_HAND || phase == Phase.FIRST_PERSON_ITEM || phase == Phase.CREATIVE_INVENTORY) {
                ItemStack stack = switch (phase) {
                    case FIRST_PERSON_ITEM -> new ItemStack(Items.DIAMOND_SWORD);
                    case CREATIVE_INVENTORY -> new ItemStack(Items.APPLE, 16);
                    default -> null;
                };
                player.inventory.setInventorySlotContents(player.inventory.currentItem, stack);
                player.inventoryContainer.detectAndSendChanges();
            }
            player.playerNetServerHandler.setPlayerLocation(phase.x + 0.5, phase.y, phase.z + 0.5, phase.yaw, phase.pitch);
        });
    }

    private static void setWeather(WorldServer world, EntityPlayerMP player, float strength) {
        boolean wet = strength > 0.0F;
        var info = world.getWorldInfo();
        if (info.isRaining() == wet && world.getRainStrength(1.0F) == strength) {
            return;
        }
        info.setCleanWeatherTime(wet ? 0 : Integer.MAX_VALUE);
        info.setRainTime(wet ? Integer.MAX_VALUE : 0);
        info.setThunderTime(Integer.MAX_VALUE);
        info.setThundering(false);
        info.setRaining(wet);
        world.setRainStrength(strength);
        player.playerNetServerHandler.sendPacket(new S2BPacketChangeGameState(wet ? 1 : 2, 0.0F));
        player.playerNetServerHandler.sendPacket(new S2BPacketChangeGameState(7, strength));
    }

    private void beginCapture(Minecraft mc, Phase phase) {
        LOGGER.info("Benchmark capturing {} for {} seconds", phase.scenario, seconds);
        ClientBenchmark.beginPhase(mc, phase.scenario, seconds, warmup);
        enter(Stage.CAPTURING, System.nanoTime());
    }

    private void enter(Stage next, long now) {
        stage = next;
        stageStarted = now;
    }

    private static boolean atDestination(Minecraft mc, Phase phase) {
        return mc.player.getDistanceSq(phase.x + 0.5, phase.y, phase.z + 0.5) < 1;
    }

    private static boolean hasExpectedHeldItem(Minecraft mc, Phase phase) {
        ItemStack stack = mc.player.getHeldItem();
        return switch (phase) {
            case FIRST_PERSON_HAND -> stack == null;
            case FIRST_PERSON_ITEM -> stack != null && stack.getItem() == Items.DIAMOND_SWORD;
            case CREATIVE_INVENTORY -> stack != null && stack.getItem() == Items.APPLE;
            default -> true;
        };
    }

    void beforeRender(Minecraft mc) {
        if (stage != Stage.CAPTURING || mc.world != world || mc.player == null || mc.currentScreen != inventory) {
            return;
        }
        Phase phase = ROUTE[routeIndex];
        if (!atDestination(mc, phase)) {
            return;
        }
        long elapsed = ClientBenchmark.phaseElapsedNanos();
        double secondsElapsed = elapsed / 1_000_000_000.0;
        float yaw = phase.yaw + (float) Math.sin(secondsElapsed * Math.PI / 3.0) * phase.sweep;
        mc.player.rotationYaw = yaw;
        mc.player.prevRotationYaw = yaw;
        mc.player.rotationPitch = phase.pitch;
        mc.player.prevRotationPitch = phase.pitch;

        if (phase == Phase.CREATIVE_INVENTORY) {
            boolean open = (TimeUnit.NANOSECONDS.toMillis(elapsed) / 1_500) % 2 == 0;
            if (open && inventory == null) {
                inventory = new GuiContainerCreative(mc.player);
                mc.displayGuiScreen(inventory);
                inventoryOpens++;
            } else if (!open) {
                closeInventory(mc);
            }
        }
    }

    void tick(Minecraft mc) {
        if ((stage != Stage.WARMING_UP && stage != Stage.CAPTURING) || mc.world != world || mc.currentScreen != null) {
            return;
        }
        Phase phase = ROUTE[routeIndex];
        if (phase == Phase.FIRST_PERSON_HAND || phase == Phase.FIRST_PERSON_ITEM) {
            if (stage == Stage.CAPTURING && mc.player.ticksExisted % 30 == 0) {
                mc.player.swingItem();
            }
            return;
        }
        if (phase != Phase.PARTICLES) {
            return;
        }
        int attempts = 36;
        int blockState = Block.getStateId(Blocks.REDSTONE_BLOCK.getDefaultState());
        for (int i = 0; i < attempts; i++) {
            double x = BenchmarkWorld.PARTICLE_X - 8 + i % 12 * 1.5;
            double y = EXHIBIT_Y + 0.4;
            double z = 5 + i / 12 * 5 + particleTick % 5 * 0.2;
            switch (i % 6) {
                case 1 -> world.spawnParticle(ParticleTypes.SMOKE_NORMAL, x, y, z, 0, 0.04, 0);
                case 3 -> world.spawnParticle(ParticleTypes.BLOCK_CRACK, x, y, z, 0, 0.04, 0, blockState);
                case 5 -> world.spawnParticle(ParticleTypes.SPELL_MOB_AMBIENT, x, y, z, 0.15, 0.6, 0.95);
                default -> world.spawnParticle(ParticleTypes.FLAME, x, y, z, 0, 0.04, 0);
            }
        }
        if (particleTick % 16 == 0) {
            world.spawnParticle(ParticleTypes.EXPLOSION_LARGE, BenchmarkWorld.PARTICLE_X + 0.5, EXHIBIT_Y + 1.2, 12, 1, 0, 0);
            attempts++;
        }
        if (stage == Stage.CAPTURING) {
            particleSpawnAttempts += attempts;
        }
        particleTick++;
    }

    private void closeInventory(Minecraft mc) {
        if (inventory != null && mc.currentScreen == inventory) {
            if (mc.player != null) {
                mc.player.closeScreen();
            } else {
                mc.displayGuiScreen(null);
            }
        }
        inventory = null;
    }

    void finish(Minecraft mc, String reason) {
        if (pending != null) {
            pending.cancel(false);
        }
        closeInventory(mc);
        if (world != null) {
            mc.gameSettings.thirdPersonView = originalPerspective;
        }
        if (exitAfterSuite) {
            mc.gameSettings.pauseOnLostFocus = originalPauseOnLostFocus;
        }
        report.addProperty("complete", reports.size() == ROUTE.length);
        report.addProperty("endReason", reason);
        ClientBenchmark.writeReport(report, "suite-");
        if (exitAfterSuite) {
            mc.shutdown();
        }
    }

    private enum Stage {
        JOINING, BUILDING, TRAVELLING, WARMING_UP, CAPTURING
    }

    private enum Phase {
        TERRAIN_STREAMING("terrain-streaming", 4096, EXHIBIT_Y, 4096, 50, 55),
        BLOCK_GALLERY("block-gallery", BenchmarkWorld.GALLERY_X, EXHIBIT_Y, 0, 4, 42),
        TRANSLUCENCY("translucency", BenchmarkWorld.TRANSLUCENT_X, EXHIBIT_Y + 3, 0, 20, 24),
        ANIMATED_TEXTURES("animated-textures", BenchmarkWorld.TRANSLUCENT_X, EXHIBIT_Y + 3, 0, 20, 0),
        DAY_SKY("day-sky", BenchmarkWorld.SKY_X, 108, 0, -30, 0, -90, 500),
        NIGHT_SKY("night-sky", BenchmarkWorld.SKY_X, 108, 0, -30, 0, -90, 15_000),
        ENTITIES("entities-and-block-entities", BenchmarkWorld.ENTITY_X, EXHIBIT_Y, 0, 3, 36),
        PARTICLES("particles", BenchmarkWorld.PARTICLE_X, EXHIBIT_Y, 0, 3, 22),
        RAIN("rain", BenchmarkWorld.RAIN_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_CAMERA_Z, 4, 25, 0, 6000, BiomeGenBase.PLAINS),
        SNOW("snow", BenchmarkWorld.SNOW_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_CAMERA_Z, 4, 25, 0, 6000, BiomeGenBase.ICE_PLAINS),
        FIRST_PERSON_HAND("first-person-hand", BenchmarkWorld.RAIN_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_SHELTER_BACK_Z - 1, 3, 0),
        FIRST_PERSON_ITEM("first-person-held-item", BenchmarkWorld.RAIN_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_SHELTER_BACK_Z - 1, 3, 0),
        CREATIVE_INVENTORY("creative-inventory", BenchmarkWorld.RAIN_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_CAMERA_Z, 4, 0);

        private final String scenario;
        private final int x;
        private final int y;
        private final int z;
        private final float pitch;
        private final float sweep;
        private final float yaw;
        private final long worldTime;
        private final BiomeGenBase precipitationBiome;

        Phase(String scenario, int x, int y, int z, float pitch, float sweep) {
            this(scenario, x, y, z, pitch, sweep, 0, 6000);
        }

        Phase(String scenario, int x, int y, int z, float pitch, float sweep, float yaw, long worldTime) {
            this(scenario, x, y, z, pitch, sweep, yaw, worldTime, null);
        }

        Phase(String scenario, int x, int y, int z, float pitch, float sweep, float yaw, long worldTime,
              BiomeGenBase precipitationBiome) {
            this.scenario = scenario;
            this.x = x;
            this.y = y;
            this.z = z;
            this.pitch = pitch;
            this.sweep = sweep;
            this.yaw = yaw;
            this.worldTime = worldTime;
            this.precipitationBiome = precipitationBiome;
        }

        float rainStrength() {
            return precipitationBiome == null ? 0.0F : 1.0F;
        }
    }
}
