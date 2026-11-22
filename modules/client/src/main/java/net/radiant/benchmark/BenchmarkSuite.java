package net.radiant.benchmark;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.impl.play.server.S03PacketTimeUpdate;
import net.minecraft.network.packet.impl.play.server.S2BPacketChangeGameState;
import net.minecraft.network.packet.impl.play.server.S44PacketWorldBorder;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.ParticleTypes;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.border.WorldBorderStage;
import net.radiant.lwjgl.opengl.Display;
import net.radiant.util.NativeImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
    private final Path comparisonDir;
    private JsonObject comparison;

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
    private int breakStage = -1;
    private int breakStageMask;
    private S44PacketWorldBorder originalBorder;
    private WorldBorderStage benchmarkBorderStage;
    private final List<EntityLightningBolt> stormBolts = new ArrayList<>();
    private EntityCow damageTarget;
    private int mapPattern = -1;
    private int stormTick;
    private boolean stormActive;
    private Stage stage = Stage.JOINING;
    private long stageStarted = System.nanoTime();

    private BenchmarkSuite(Minecraft mc, String folder, boolean exitAfterSuite) {
        server = mc.getIntegratedServer();
        comparisonDir = ClientBenchmark.COMPARE_FRAMES ? Path.of(folder) : null;
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
        if (phase == Phase.FIRE_OVERLAY && mc.player != null) {
            mc.player.extinguish();
        }
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
                if (changingDimension()) {
                    if (mc.world == null || mc.player == null || mc.currentScreen != null
                            || mc.world.provider.getDimensionId() != warmupLocation.dimension) {
                        return;
                    }
                    world = mc.world;
                }
                BlockPos floor = new BlockPos(warmupLocation.x, BenchmarkWorld.FLOOR_Y, warmupLocation.z);
                if (pending == null && atDestination(mc, warmupLocation) && world.getWorldTime() == warmupLocation.worldTime
                        && world.getBlockState(floor).getBlock() == Blocks.STONEBRICK
                        && world.getRainStrength(1.0F) == warmupLocation.rainStrength()
                        && (warmupLocation.precipitationBiome == null || world.getBiomeGenForCoords(floor) == warmupLocation.precipitationBiome)
                        && hasExpectedCameraState(mc, warmupLocation) && hasExpectedHeldItem(mc, warmupLocation)) {
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
                    if (comparisonDir != null) {
                        if (comparison == null) {
                            comparison = new JsonObject();
                            comparison.addProperty("status", "unavailable");
                            comparison.addProperty("reason", "No matching OpenGL and Vulkan frame was available");
                        }
                        result.add("comparison", comparison);
                    }
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
                    if (phase == Phase.BLOCK_INTERACTION) {
                        result.addProperty("blockBreakStages", Integer.bitCount(breakStageMask));
                        setBreakStage(mc, -1);
                    }
                    if (phase == Phase.WORLD_BORDER) {
                        restoreBorder();
                    }
                    if (phase == Phase.THUNDERSTORM) {
                        clearStorm();
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
        boolean transfer = changingDimension();
        if (mc.getIntegratedServer() != server || (world != null && !transfer && (mc.world != world || mc.player == null))) {
            return "world changed";
        }
        if (mc.displayWidth != width || mc.displayHeight != height) {
            return "window size changed";
        }
        if (world != null && mc.currentScreen != inventory && !(transfer && mc.currentScreen instanceof GuiDownloadTerrain)) {
            return "unexpected screen opened or closed";
        }
        if (!exitAfterSuite && world != null && !Display.isActive()) {
            return "window lost focus";
        }
        return null;
    }

    private boolean changingDimension() {
        return stage == Stage.TRAVELLING && world != null && world.provider.getDimensionId() != ROUTE[routeIndex].dimension;
    }

    private void preparePhase(Phase phase, long now) {
        comparison = null;
        arrived = false;
        particleTick = 0;
        particleSpawnAttempts = 0;
        unfocusedFrames = 0;
        inventoryOpens = 0;
        breakStageMask = 0;
        stormTick = 0;
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
            if (player.dimension != phase.dimension) {
                BenchmarkWorld.buildDimensionSky(server.worldServerForDimension(phase.dimension));
                player = server.getConfigurationManager().recreatePlayerEntity(player, phase.dimension, true);
                player.playerNetServerHandler.playerEntity = player;
            }
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
            var serverWorld = player.getServerForPlayer();
            if (serverWorld.getWorldTime() != phase.worldTime) {
                serverWorld.setWorldTime(phase.worldTime);
                player.playerNetServerHandler.sendPacket(new S03PacketTimeUpdate(serverWorld.getTotalWorldTime(), phase.worldTime, false));
            }
            setWeather(serverWorld, player, phase.rainStrength());
            if (phase == Phase.FIRST_PERSON_HAND || phase == Phase.FIRST_PERSON_ITEM || phase == Phase.WATER_OVERLAY
                    || phase == Phase.FIRST_PERSON_GLASS || phase == Phase.FIRST_PERSON_GLINT || phase == Phase.CREATIVE_INVENTORY) {
                ItemStack stack = switch (phase) {
                    case FIRST_PERSON_ITEM -> new ItemStack(Items.DIAMOND_SWORD);
                    case FIRST_PERSON_GLASS -> new ItemStack(Blocks.STAINED_GLASS);
                    case FIRST_PERSON_GLINT -> enchantedSword();
                    case CREATIVE_INVENTORY -> new ItemStack(Items.APPLE, 16);
                    default -> null;
                };
                int selected = player.inventory.currentItem;
                player.inventory.setInventorySlotContents(selected, stack);
                if (phase == Phase.CREATIVE_INVENTORY) {
                    player.inventory.setInventorySlotContents((selected + 1) % 9, new ItemStack(Items.DIAMOND_SWORD));
                    player.inventory.setInventorySlotContents((selected + 2) % 9, enchantedSword());
                }
                player.inventoryContainer.detectAndSendChanges();
            }
            player.playerNetServerHandler.setPlayerLocation(phase.x + 0.5, phase.y, phase.z + 0.5, phase.yaw, phase.pitch);
        });
    }

    private static ItemStack enchantedSword() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.SHARPNESS, 1);
        return sword;
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

    boolean wantsFrameComparison(Minecraft mc) {
        if (stage != Stage.CAPTURING || comparison != null || world != mc.world || mc.player == null
                || mc.displayWidth != width || mc.displayHeight != height
                || Display.getFramebufferWidth() < width || Display.getFramebufferHeight() < height
                || mc.currentScreen != inventory) {
            return false;
        }
        Phase phase = ROUTE[routeIndex];
        return atDestination(mc, phase) && (phase != Phase.CREATIVE_INVENTORY || inventory != null)
                && ClientBenchmark.phaseElapsedNanos() >= TimeUnit.MILLISECONDS.toNanos(Math.min(seconds * 500L, 1_000L));
    }

    void saveFrameComparison(Minecraft mc, float partialTicks, NativeImage vulkan) {
        String scenario = ROUTE[routeIndex].scenario;
        comparison = new JsonObject();
        try {
            BenchmarkFrameComparison.save(comparisonDir.resolve(scenario), vulkan, comparison);
            comparison.addProperty("status", "captured");
            comparison.addProperty("worldTick", world.getTotalWorldTime());
            comparison.addProperty("partialTicks", partialTicks);
            comparison.addProperty("yaw", mc.player.rotationYaw);
            comparison.addProperty("pitch", mc.player.rotationPitch);
        } catch (IOException e) {
            comparison.addProperty("status", "failed");
            comparison.addProperty("reason", e.getMessage());
            LOGGER.warn("Could not write frame comparison for {}", scenario, e);
        }
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
            case FIRST_PERSON_HAND, WATER_OVERLAY, BLOCK_OVERLAY, FIRE_OVERLAY -> stack == null;
            case FIRST_PERSON_ITEM -> stack != null && stack.getItem() == Items.DIAMOND_SWORD;
            case FIRST_PERSON_GLASS -> stack != null && Block.getBlockFromItem(stack.getItem()) == Blocks.STAINED_GLASS;
            case FIRST_PERSON_GLINT -> stack != null && stack.getItem() == Items.DIAMOND_SWORD && stack.hasEffect();
            case CREATIVE_INVENTORY -> stack != null && stack.getItem() == Items.APPLE;
            default -> true;
        };
    }

    private static boolean hasExpectedCameraState(Minecraft mc, Phase phase) {
        return switch (phase) {
            case WATER_OVERLAY -> mc.player.isInsideOfMaterial(Material.WATER);
            case BLOCK_OVERLAY -> mc.player.isEntityInsideOpaqueBlock();
            case BLOCK_INTERACTION -> mc.world.getBlockState(BenchmarkWorld.BREAKING_SLAB).getBlock() == Blocks.STONE_SLAB
                    && mc.world.getBlockState(BenchmarkWorld.BREAKING_CHEST).getBlock() == Blocks.CHEST;
            default -> true;
        };
    }

    void beforeRender(Minecraft mc) {
        if ((stage != Stage.WARMING_UP && stage != Stage.CAPTURING)
                || mc.world != world || mc.player == null || mc.currentScreen != inventory) {
            return;
        }
        Phase phase = ROUTE[routeIndex];
        if (!atDestination(mc, phase)) {
            return;
        }
        // Exercise the local fire overlay without changing game mode or applying server damage.
        if (phase == Phase.FIRE_OVERLAY) {
            mc.player.setFire(1);
        }
        if (phase == Phase.ENTITIES) {
            int pattern = mc.player.ticksExisted / 6 % 2;
            if (pattern != mapPattern && world.loadItemData(MapData.class, BenchmarkWorld.DYNAMIC_MAP_NAME) instanceof MapData map) {
                BenchmarkWorld.paintDynamicMap(map, pattern != 0);
                mc.entityRenderer.getMapItemRenderer().updateMapTexture(map);
                mapPattern = pattern;
            }
        }
        if (phase == Phase.ENTITIES && ClientBenchmark.ENTITY_DAMAGE) {
            if (damageTarget == null) {
                for (var entity : world.getLoadedEntityList()) {
                    if (entity instanceof EntityCow cow && cow.getCustomNameTag().equals("Exhibit 5")) {
                        damageTarget = cow;
                        break;
                    }
                }
            }
            if (damageTarget != null) {
                // Alternate the client-side flash without damaging the exhibit.
                damageTarget.hurtTime = mc.player.ticksExisted % 20 < 10 ? 1 : 0;
            }
        }
        if (phase == Phase.BLOCK_INTERACTION) {
            int nextStage = stage == Stage.WARMING_UP ? 0 : Math.clamp(
                    (int) (ClientBenchmark.phaseElapsedNanos() * 10 / TimeUnit.SECONDS.toNanos(seconds)), 0, 9);
            setBreakStage(mc, nextStage);
            if (stage == Stage.CAPTURING) {
                breakStageMask |= 1 << nextStage;
            }
        }
        if (phase == Phase.WORLD_BORDER) {
            updateBorder();
        }
        if (phase == Phase.THUNDERSTORM && !stormActive) {
            stormActive = true;
            world.setThunderStrength(1.0F);
        }
        if (stage != Stage.CAPTURING) {
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

    private void setBreakStage(Minecraft mc, int nextStage) {
        if (mc.world == world && world != null && breakStage != nextStage) {
            world.sendBlockBreakProgress(-1, BenchmarkWorld.BREAKING_SLAB, nextStage);
            world.sendBlockBreakProgress(-2, BenchmarkWorld.BREAKING_CHEST, nextStage);
            breakStage = nextStage;
        }
    }

    private void updateBorder() {
        var border = world.getWorldBorder();
        if (originalBorder == null) {
            originalBorder = new S44PacketWorldBorder(border, S44PacketWorldBorder.Action.INITIALIZE);
            // Both nearby walls fit in the view without restricting the integrated server's world.
            border.setCenter(Phase.WORLD_BORDER.x + 0.5 - 25, Phase.WORLD_BORDER.z + 0.5 - 25);
            border.setWarningDistance(12);
        }
        int section = stage == Stage.WARMING_UP ? 0 : (int) (ClientBenchmark.phaseElapsedNanos() * 3 / TimeUnit.SECONDS.toNanos(seconds));
        WorldBorderStage next = section == 0 ? WorldBorderStage.STATIONARY
                : section == 1 ? WorldBorderStage.SHRINKING : WorldBorderStage.GROWING;
        if (next == benchmarkBorderStage) {
            return;
        }
        benchmarkBorderStage = next;
        long duration = TimeUnit.SECONDS.toMillis(seconds) / 3;
        switch (next) {
            case STATIONARY -> border.setTransition(64);
            case SHRINKING -> border.setTransition(border.getDiameter(), 60, duration);
            case GROWING -> border.setTransition(border.getDiameter(), 64, duration);
        }
    }

    private void restoreBorder() {
        if (originalBorder != null) {
            originalBorder.func_179788_a(world.getWorldBorder());
            originalBorder = null;
            benchmarkBorderStage = null;
        }
    }

    private void clearStorm() {
        if (stormActive || !stormBolts.isEmpty()) {
            world.weatherEffects.removeAll(stormBolts);
            world.setThunderStrength(0.0F);
            world.setLastLightningBolt(0);
            stormBolts.clear();
            stormActive = false;
        }
    }

    void tick(Minecraft mc) {
        if ((stage != Stage.WARMING_UP && stage != Stage.CAPTURING) || mc.world != world || mc.currentScreen != null) {
            return;
        }
        Phase phase = ROUTE[routeIndex];
        if (phase == Phase.THUNDERSTORM) {
            if (stormTick++ % 16 == 0) {
                stormBolts.removeIf(bolt -> bolt.isDead);
                EntityLightningBolt bolt = new EntityLightningBolt(world, BenchmarkWorld.RAIN_X + 8.5,
                        EXHIBIT_Y, BenchmarkWorld.WEATHER_CAMERA_Z + 16.5);
                world.addWeatherEffect(bolt);
                stormBolts.add(bolt);
            }
            return;
        }
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

    boolean isEntityScene() {
        return (stage == Stage.WARMING_UP || stage == Stage.CAPTURING) && ROUTE[routeIndex] == Phase.ENTITIES;
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
        setBreakStage(mc, -1);
        restoreBorder();
        clearStorm();
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
        ABOVE_CLOUDS("above-clouds", BenchmarkWorld.SKY_X, 152, 0, 30, 0, -90, 500),
        CLOUD_LEVEL("cloud-level", BenchmarkWorld.SKY_X, 128, 0, 0, 36, -90, 500),
        SUNSET_SKY("sunset-sky", BenchmarkWorld.SKY_X, 108, 0, 0, 0, 90, 14_000),
        SUNRISE_SKY("sunrise-sky", BenchmarkWorld.SKY_X, 108, 0, 0, 0, -90, 22_000),
        ENTITIES("entities-and-block-entities", BenchmarkWorld.ENTITY_X, EXHIBIT_Y, 0, 3, 36),
        PARTICLES("particles", BenchmarkWorld.PARTICLE_X, EXHIBIT_Y, 0, 3, 22),
        RAIN("rain", BenchmarkWorld.RAIN_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_CAMERA_Z, 4, 25, 0, 6000, BiomeGenBase.PLAINS),
        SNOW("snow", BenchmarkWorld.SNOW_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_CAMERA_Z, 4, 25, 0, 6000, BiomeGenBase.ICE_PLAINS),
        THUNDERSTORM("thunderstorm", BenchmarkWorld.RAIN_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_CAMERA_Z, -12, 10, -22, 13000, BiomeGenBase.PLAINS),
        FIRST_PERSON_HAND("first-person-hand", BenchmarkWorld.RAIN_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_SHELTER_BACK_Z - 1, 3, 0),
        FIRST_PERSON_ITEM("first-person-held-item", BenchmarkWorld.RAIN_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_SHELTER_BACK_Z - 1, 3, 0),
        FIRST_PERSON_GLASS("first-person-held-glass", BenchmarkWorld.RAIN_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_SHELTER_BACK_Z - 1, 3, 0),
        WATER_OVERLAY("water-overlay", BenchmarkWorld.WATER_OVERLAY_X, EXHIBIT_Y, BenchmarkWorld.OVERLAY_FIXTURE_Z, 5, 25),
        BLOCK_OVERLAY("block-overlay", BenchmarkWorld.BLOCK_OVERLAY_X, EXHIBIT_Y, BenchmarkWorld.OVERLAY_FIXTURE_Z, 0, 0),
        FIRE_OVERLAY("fire-overlay", BenchmarkWorld.RAIN_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_CAMERA_Z, 4, 12),
        BLOCK_INTERACTION("block-interaction", BenchmarkWorld.BLOCK_INTERACTION_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_CAMERA_Z, 0, 0),
        WORLD_BORDER("world-border", BenchmarkWorld.RAIN_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_CAMERA_Z, 0, 12, -40, 6000),
        FIRST_PERSON_GLINT("first-person-glint", BenchmarkWorld.RAIN_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_CAMERA_Z, 4, 0),
        CREATIVE_INVENTORY("creative-inventory", BenchmarkWorld.RAIN_X, EXHIBIT_Y, BenchmarkWorld.WEATHER_CAMERA_Z, 4, 0),
        NETHER_FOG("nether-fog", BenchmarkWorld.DIMENSION_SKY_X, EXHIBIT_Y, 0, -5, 35, -1),
        END_SKY("end-sky", BenchmarkWorld.DIMENSION_SKY_X, EXHIBIT_Y, 0, -12, 55, 1);

        private final String scenario;
        private final int x;
        private final int y;
        private final int z;
        private final float pitch;
        private final float sweep;
        private final float yaw;
        private final long worldTime;
        private final BiomeGenBase precipitationBiome;
        private final int dimension;

        Phase(String scenario, int x, int y, int z, float pitch, float sweep) {
            this(scenario, x, y, z, pitch, sweep, 0, 6000);
        }

        Phase(String scenario, int x, int y, int z, float pitch, float sweep, int dimension) {
            this(scenario, x, y, z, pitch, sweep, 0, 6000, null, dimension);
        }

        Phase(String scenario, int x, int y, int z, float pitch, float sweep, float yaw, long worldTime) {
            this(scenario, x, y, z, pitch, sweep, yaw, worldTime, null);
        }

        Phase(String scenario, int x, int y, int z, float pitch, float sweep, float yaw, long worldTime,
              BiomeGenBase precipitationBiome) {
            this(scenario, x, y, z, pitch, sweep, yaw, worldTime, precipitationBiome, 0);
        }

        Phase(String scenario, int x, int y, int z, float pitch, float sweep, float yaw, long worldTime,
              BiomeGenBase precipitationBiome, int dimension) {
            this.scenario = scenario;
            this.x = x;
            this.y = y;
            this.z = z;
            this.pitch = pitch;
            this.sweep = sweep;
            this.yaw = yaw;
            this.worldTime = worldTime;
            this.precipitationBiome = precipitationBiome;
            this.dimension = dimension;
        }

        float rainStrength() {
            return precipitationBiome == null ? 0.0F : 1.0F;
        }
    }
}
