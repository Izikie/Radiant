package net.radiant.benchmark;

import com.google.gson.JsonArray;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.management.ThreadMXBean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.border.WorldBorderStage;
import net.optifine.Config;
import net.optifine.shaders.Shaders;
import net.radiant.lwjgl.opengl.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public final class ClientBenchmark {
    public static final boolean ENABLED = Boolean.getBoolean("radiant.benchmark");
    static final boolean ENTITY_DAMAGE = ENABLED && Boolean.getBoolean("radiant.benchmark.entityDamage");

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientBenchmark.class);
    private static final int MAX_FRAME_SAMPLES = 250_000;
    private static final int MAX_TICK_SAMPLES = 20_000;
    private static final boolean ENTITY_HITBOXES = ENABLED && Boolean.getBoolean("radiant.benchmark.entityHitboxes");

    private static long startupMillis = -1;
    private static int warmupSeconds;
    private static long warmupStart;
    private static WorldClient captureWorld;
    private static volatile Capture active;
    private static BenchmarkSuite suite;
    private static boolean autoStart = ENABLED && Boolean.getBoolean("radiant.benchmark.autoStart");

    private ClientBenchmark() {
    }

    public static void toggle(Minecraft mc) {
        if (suite != null) {
            stopSuite(mc, "cancelled with F8");
        } else if (captureWorld != null) {
            cancel("cancelled with F8");
        } else if (isInWorld(mc)) {
            captureWorld = mc.world;
            warmupSeconds = Math.clamp(Integer.getInteger("radiant.benchmark.warmup", 5), 0, 120);
            warmupStart = System.nanoTime();
            LOGGER.info("Benchmark armed. Capture starts in {} seconds.", warmupSeconds);
        }
    }

    public static void launchSuite(Minecraft mc) {
        if (ENABLED && suite == null && mc.world == null && mc.currentScreen instanceof GuiMainMenu) {
            boolean exitAfterSuite = autoStart;
            autoStart = false;
            suite = BenchmarkSuite.launch(mc, exitAfterSuite);
        }
    }

    public static void tick(Minecraft mc) {
        if (suite != null) {
            suite.tick(mc);
        }
    }

    public static void beforeRender(Minecraft mc) {
        if (suite != null) {
            suite.beforeRender(mc);
        }
    }

    public static boolean controlsCamera() {
        return ENABLED && suite != null;
    }

    public static boolean isEntityOutlineScene() {
        return suite != null && suite.isEntityScene();
    }

    public static boolean showsEntityHitboxes() {
        return ENTITY_HITBOXES && suite != null && suite.isEntityScene();
    }

    public static boolean isEntityOutlineTarget(Entity entity) {
        return isEntityOutlineScene() && (entity.getCustomNameTag().equals("Exhibit 15")
                || entity.getCustomNameTag().equals("Exhibit 43"));
    }

    static void stopSuite(Minecraft mc, String reason) {
        active = null;
        BenchmarkSuite finished = suite;
        suite = null;
        finished.finish(mc, reason);
        LOGGER.info("Benchmark suite ended: {}", reason);
    }

    public static void recordMainMenuPresented(GuiScreen screen) {
        if (startupMillis < 0 && screen instanceof GuiMainMenu) {
            startupMillis = ManagementFactory.getRuntimeMXBean().getUptime();
            LOGGER.info("Benchmark startup to first main menu frame: {} ms", startupMillis);
        }
    }

    public static void endFrame(Minecraft mc, long frameStart) {
        if (autoStart && startupMillis >= 0) {
            launchSuite(mc);
        }
        if (suite != null) {
            suite.endFrame(mc, frameStart);
            return;
        }
        if (captureWorld == null) {
            return;
        }
        if (mc.world != captureWorld || !isInWorld(mc)) {
            cancel("world or window focus changed, or a screen opened");
            return;
        }

        long now = System.nanoTime();
        Capture capture = active;
        if (capture == null) {
            if (now - warmupStart >= TimeUnit.SECONDS.toNanos(warmupSeconds)) {
                int seconds = Math.clamp(Integer.getInteger("radiant.benchmark.seconds", 30), 1, 180);
                capture = new Capture(mc, seconds, warmupSeconds, System.getProperty("radiant.benchmark.scenario", "manual"));
                LOGGER.info("Benchmark capture started for {} seconds.", seconds);
                capture.start();
                active = capture;
            }
            return;
        }
        if (mc.displayWidth != capture.width || mc.displayHeight != capture.height) {
            cancel("window size changed");
            return;
        }

        JsonObject report = sample(frameStart, now);
        if (report != null) {
            captureWorld = null;
            writeReport(report, "capture-");
        }
    }

    static void beginPhase(Minecraft mc, String name, int seconds, int warmup) {
        Capture capture = new Capture(mc, seconds, warmup, name);
        capture.start();
        active = capture;
    }

    static long phaseElapsedNanos() {
        Capture capture = active;
        return capture == null ? 0 : System.nanoTime() - capture.started;
    }

    static JsonObject sample(long frameStart, long now) {
        Capture capture = active;
        capture.frames[capture.frameCount++] = now - frameStart;
        if (now - capture.started >= capture.duration || capture.frameCount == capture.frames.length) {
            active = null;
            return capture.summarize(now);
        }
        return null;
    }

    static void writeReport(JsonObject report, String prefix) {
        try {
            Path directory = Files.createDirectories(Path.of("benchmarks"));
            Path output = Files.createTempFile(directory, prefix, ".json");
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(report);
            Files.writeString(output, json + System.lineSeparator());
            LOGGER.info("Benchmark report written to {}", output.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.warn("Could not write benchmark report", e);
        }
    }

    private static boolean isInWorld(Minecraft mc) {
        return mc.world != null && mc.player != null && mc.currentScreen == null && Display.isActive();
    }

    private static void cancel(String reason) {
        captureWorld = null;
        active = null;
        LOGGER.info("Benchmark capture discarded: {}", reason);
    }

    public static long startChunkWork() {
        return active == null ? 0 : System.nanoTime();
    }

    public static void recordChunkWork(long started, boolean upload) {
        Capture capture = active;
        if (started == 0 || capture == null || started - capture.started < 0) {
            return;
        }
        long elapsed = System.nanoTime() - started;
        synchronized (capture) {
            if (capture == active) {
                if (upload) {
                    capture.uploadCount++;
                    capture.uploadNanos += elapsed;
                } else {
                    capture.buildCount++;
                    capture.buildNanos += elapsed;
                }
            }
        }
    }

    public static void recordServerTick(MinecraftServer server, long started, long elapsed) {
        Capture capture = active;
        if (capture == null || capture.server != server || started - capture.started < 0) {
            return;
        }
        synchronized (capture) {
            if (capture == active && capture.tickCount < capture.ticks.length) {
                capture.ticks[capture.tickCount++] = elapsed;
            }
        }
    }

    public static void recordDrawCall() {
        if (ENABLED) {
            Capture capture = active;
            if (capture != null) {
                capture.drawCalls++;
            }
        }
    }

    public static void recordVulkanTerrainFrame(int draws, int quads, int cutoutQuads, int translucentQuads) {
        Capture capture = active;
        if (capture != null) {
            capture.vulkanTerrainFrames++;
            capture.vulkanTerrainDraws += draws;
            capture.vulkanTerrainQuads += quads;
            capture.vulkanTerrainCutoutQuads += cutoutQuads;
            capture.vulkanTerrainTranslucentQuads += translucentQuads;
        }
    }

    public static void recordVulkanObjectFrame(int entityTriangles, int blockEntityTriangles, int blockEntityGlyphs) {
        Capture capture = active;
        if (capture != null) {
            capture.vulkanEntityTriangles += entityTriangles;
            capture.vulkanBlockEntityTriangles += blockEntityTriangles;
            capture.vulkanBlockEntityGlyphs += blockEntityGlyphs;
        }
    }

    public static void recordVulkanEntityShadows(int quads) {
        Capture capture = active;
        if (capture != null && quads > 0) {
            capture.vulkanEntityShadowFrames++;
            capture.vulkanEntityShadowQuads += quads;
        }
    }

    public static void recordVulkanEntityFire(int quads) {
        Capture capture = active;
        if (capture != null && quads > 0) {
            capture.vulkanEntityFireFrames++;
            capture.vulkanEntityFireQuads += quads;
        }
    }

    public static void recordVulkanEntityHitboxes(int segments) {
        Capture capture = active;
        if (capture != null && segments > 0) {
            capture.vulkanEntityHitboxFrames++;
            capture.vulkanEntityHitboxSegments += segments;
        }
    }

    public static void recordVulkanEntityLeashes(int triangles) {
        Capture capture = active;
        if (capture != null && triangles > 0) {
            capture.vulkanEntityLeashFrames++;
            capture.vulkanEntityLeashTriangles += triangles;
        }
    }

    public static void recordVulkanEntityNameTags(int glyphs, int throughWallGlyphs) {
        Capture capture = active;
        if (capture != null && glyphs > 0) {
            capture.vulkanEntityNameTagFrames++;
            capture.vulkanEntityNameTagGlyphs += glyphs;
            capture.vulkanEntityNameTagThroughWallGlyphs += throughWallGlyphs;
        }
    }

    public static void recordVulkanEntityTint(int triangles) {
        Capture capture = active;
        if (capture != null && triangles > 0) {
            capture.vulkanEntityTintFrames++;
            capture.vulkanEntityTintTriangles += triangles;
        }
    }

    public static void recordVulkanParticleFrame(int triangles, int terrainTriangles, int translucentTriangles, int litTriangles) {
        Capture capture = active;
        if (capture != null) {
            capture.vulkanParticleTriangles += triangles;
            capture.vulkanTerrainParticleTriangles += terrainTriangles;
            capture.vulkanTranslucentParticleTriangles += translucentTriangles;
            capture.vulkanLitParticleTriangles += litTriangles;
        }
    }

    public static void recordVulkanPrecipitationFrame(int rainQuads, int snowQuads) {
        Capture capture = active;
        if (capture != null) {
            capture.vulkanRainQuads += rainQuads;
            capture.vulkanSnowQuads += snowQuads;
        }
    }

    public static void recordVulkanFirstPersonFrame(int handTriangles, int heldItemTriangles) {
        Capture capture = active;
        if (capture != null) {
            capture.vulkanHandTriangles += handTriangles;
            capture.vulkanHeldItemTriangles += heldItemTriangles;
            if (handTriangles > 0) {
                capture.vulkanHandFrames++;
            }
            if (heldItemTriangles > 0) {
                capture.vulkanHeldItemFrames++;
            }
        }
    }

    public static void recordVulkanItemGlint(int heldTriangles, int guiTriangles) {
        Capture capture = active;
        if (capture != null) {
            capture.vulkanHeldGlintTriangles += heldTriangles;
            capture.vulkanGuiGlintTriangles += guiTriangles;
            if (heldTriangles > 0) {
                capture.vulkanHeldGlintFrames++;
            }
            if (guiTriangles > 0) {
                capture.vulkanGuiGlintFrames++;
            }
        }
    }

    public static void recordVulkanEntityOutlines(int triangles) {
        Capture capture = active;
        if (capture != null && triangles > 0) {
            capture.vulkanEntityOutlineFrames++;
            capture.vulkanEntityOutlineTriangles += triangles;
        }
    }

    public static void recordVulkanLightning(int triangles, boolean flash) {
        Capture capture = active;
        if (capture != null) {
            if (triangles > 0) {
                capture.vulkanLightningFrames++;
                capture.vulkanLightningTriangles += triangles;
            }
            if (flash) {
                capture.vulkanLightningFlashFrames++;
            }
        }
    }

    public static void recordVulkanArmorGlint(int triangles) {
        Capture capture = active;
        if (capture != null && triangles > 0) {
            capture.vulkanArmorGlintFrames++;
            capture.vulkanArmorGlintTriangles += triangles;
        }
    }

    public static void recordVulkanCameraOverlays(int blockQuads, int waterQuads, int fireQuads) {
        Capture capture = active;
        if (capture != null) {
            capture.vulkanBlockOverlayQuads += blockQuads;
            capture.vulkanWaterOverlayQuads += waterQuads;
            capture.vulkanFireOverlayQuads += fireQuads;
        }
    }

    public static void recordVulkanBlockInteraction(int selectionSegments, int blockDamageQuads) {
        Capture capture = active;
        if (capture != null) {
            capture.vulkanSelectionSegments += selectionSegments;
            capture.vulkanBlockDamageQuads += blockDamageQuads;
        }
    }

    public static void recordVulkanWorldBorder(int quads, WorldBorderStage stage) {
        Capture capture = active;
        if (capture != null && quads > 0) {
            capture.vulkanWorldBorderFrames++;
            capture.vulkanWorldBorderQuads += quads;
            capture.vulkanWorldBorderStages.add(stage);
        }
    }

    public static void recordVulkanUiFrame(int hudTriangles, int hudItemTriangles, int guiTriangles, int guiItemTriangles, int guiGlyphs) {
        Capture capture = active;
        if (capture != null) {
            capture.vulkanHudTriangles += hudTriangles;
            capture.vulkanHudItemTriangles += hudItemTriangles;
            capture.vulkanGuiTriangles += guiTriangles;
            capture.vulkanGuiItemTriangles += guiItemTriangles;
            capture.vulkanGuiGlyphs += guiGlyphs;
            if (hudTriangles > 0) {
                capture.vulkanHudFrames++;
            }
            if (guiTriangles > 0) {
                capture.vulkanGuiFrames++;
            }
        }
    }

    public static void recordVulkanAtlasUpload(int sprites, int water, int lava, int bytes) {
        Capture capture = active;
        if (capture != null) {
            capture.vulkanAtlasSpriteUpdates += sprites;
            capture.vulkanWaterSpriteUpdates += water;
            capture.vulkanLavaSpriteUpdates += lava;
            capture.vulkanAtlasUploadBytes += bytes;
        }
    }

    public static void recordVulkanEnvironmentFrame(boolean sky, boolean clouds, boolean endSky, boolean fogBackground) {
        Capture capture = active;
        if (capture != null) {
            if (sky) {
                capture.vulkanSkyFrames++;
            }
            if (clouds) {
                capture.vulkanCloudFrames++;
            }
            if (endSky) {
                capture.vulkanEndSkyFrames++;
            }
            if (fogBackground) {
                capture.vulkanFogBackgroundFrames++;
            }
        }
    }

    private static ThreadMXBean allocationTracker() {
        if (ManagementFactory.getThreadMXBean() instanceof ThreadMXBean bean
                && bean.isThreadAllocatedMemorySupported()) {
            if (!bean.isThreadAllocatedMemoryEnabled()) {
                bean.setThreadAllocatedMemoryEnabled(true);
            }
            return bean;
        }
        return null;
    }

    static JsonObject summarizeTimes(long[] samples, int count) {
        JsonObject result = new JsonObject();
        result.addProperty("count", count);
        if (count > 0) {
            Arrays.sort(samples, 0, count);
            result.addProperty("p50Ms", percentileMillis(samples, count, 50));
            result.addProperty("p95Ms", percentileMillis(samples, count, 95));
            result.addProperty("p99Ms", percentileMillis(samples, count, 99));
        }
        return result;
    }

    private static double percentileMillis(long[] sorted, int count, int percentile) {
        return sorted[(count * percentile + 99) / 100 - 1] / 1_000_000.0;
    }

    private static final class Capture {
        private final JsonObject report = new JsonObject();
        private final long[] frames = new long[MAX_FRAME_SAMPLES];
        private final long[] ticks = new long[MAX_TICK_SAMPLES];
        private final ThreadMXBean allocation = allocationTracker();
        private final IntegratedServer server;
        private final long duration;
        private final int width;
        private final int height;
        private long started;
        private long allocatedAtStart;
        private int frameCount;
        private int tickCount;
        private long drawCalls;
        private long vulkanTerrainFrames;
        private long vulkanTerrainDraws;
        private long vulkanTerrainQuads;
        private long vulkanTerrainCutoutQuads;
        private long vulkanTerrainTranslucentQuads;
        private long vulkanEntityTriangles;
        private long vulkanEntityShadowFrames;
        private long vulkanEntityShadowQuads;
        private long vulkanEntityFireFrames;
        private long vulkanEntityFireQuads;
        private long vulkanEntityHitboxFrames;
        private long vulkanEntityHitboxSegments;
        private long vulkanEntityLeashFrames;
        private long vulkanEntityLeashTriangles;
        private long vulkanEntityNameTagFrames;
        private long vulkanEntityNameTagGlyphs;
        private long vulkanEntityTintFrames;
        private long vulkanEntityTintTriangles;
        private long vulkanEntityNameTagThroughWallGlyphs;
        private long vulkanBlockEntityTriangles;
        private long vulkanBlockEntityGlyphs;
        private long vulkanParticleTriangles;
        private long vulkanTerrainParticleTriangles;
        private long vulkanTranslucentParticleTriangles;
        private long vulkanLitParticleTriangles;
        private long vulkanRainQuads;
        private long vulkanSnowQuads;
        private long vulkanHandFrames;
        private long vulkanHandTriangles;
        private long vulkanHeldItemFrames;
        private long vulkanHeldItemTriangles;
        private long vulkanHeldGlintFrames;
        private long vulkanEntityOutlineFrames;
        private long vulkanEntityOutlineTriangles;
        private long vulkanLightningFrames;
        private long vulkanLightningTriangles;
        private long vulkanLightningFlashFrames;
        private long vulkanArmorGlintFrames;
        private long vulkanArmorGlintTriangles;
        private long vulkanHeldGlintTriangles;
        private long vulkanGuiGlintFrames;
        private long vulkanGuiGlintTriangles;
        private long vulkanBlockOverlayQuads;
        private long vulkanWaterOverlayQuads;
        private long vulkanFireOverlayQuads;
        private long vulkanSelectionSegments;
        private long vulkanBlockDamageQuads;
        private long vulkanWorldBorderFrames;
        private long vulkanWorldBorderQuads;
        private final EnumSet<WorldBorderStage> vulkanWorldBorderStages = EnumSet.noneOf(WorldBorderStage.class);
        private long vulkanHudFrames;
        private long vulkanHudTriangles;
        private long vulkanHudItemTriangles;
        private long vulkanGuiFrames;
        private long vulkanGuiTriangles;
        private long vulkanGuiItemTriangles;
        private long vulkanGuiGlyphs;
        private long vulkanAtlasSpriteUpdates;
        private long vulkanWaterSpriteUpdates;
        private long vulkanLavaSpriteUpdates;
        private long vulkanAtlasUploadBytes;
        private long vulkanSkyFrames;
        private long vulkanCloudFrames;
        private long vulkanEndSkyFrames;
        private long vulkanFogBackgroundFrames;
        private long buildCount;
        private long buildNanos;
        private long uploadCount;
        private long uploadNanos;

        private Capture(Minecraft mc, int seconds, int warmup, String scenario) {
            server = mc.getIntegratedServer();
            duration = TimeUnit.SECONDS.toNanos(seconds);
            width = mc.displayWidth;
            height = mc.displayHeight;
            report.addProperty("schemaVersion", 1);
            report.addProperty("scenario", scenario);
            report.addProperty("warmupSeconds", warmup);
            report.addProperty("targetSeconds", seconds);
            if (startupMillis >= 0) {
                report.addProperty("startupToMainMenuMs", startupMillis);
            }
            report.addProperty("javaVersion", System.getProperty("java.version"));
            report.addProperty("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            report.addProperty("gpu", Config.openGlRenderer);
            report.addProperty("glVersion", Config.openGlVersion);
            report.addProperty("integratedServer", server != null);
            report.addProperty("dimension", mc.player.dimension);
            report.addProperty("worldTime", mc.world.getWorldTime());
            JsonObject camera = new JsonObject();
            camera.addProperty("x", mc.player.posX);
            camera.addProperty("y", mc.player.posY);
            camera.addProperty("z", mc.player.posZ);
            camera.addProperty("yaw", mc.player.rotationYaw);
            camera.addProperty("pitch", mc.player.rotationPitch);
            report.add("playerAtStart", camera);

            GameSettings settings = mc.gameSettings;
            JsonObject graphics = new JsonObject();
            graphics.addProperty("width", width);
            graphics.addProperty("height", height);
            graphics.addProperty("renderDistanceChunks", settings.renderDistanceChunks);
            graphics.addProperty("ofClouds", settings.ofClouds);
            graphics.addProperty("ofRain", settings.ofRain);
            graphics.addProperty("vsync", settings.enableVsync);
            graphics.addProperty("fpsLimit", settings.limitFramerate);
            graphics.addProperty("fancyGraphics", settings.fancyGraphics);
            graphics.addProperty("entityShadows", settings.entityShadows);
            graphics.addProperty("ambientOcclusion", settings.ambientOcclusion);
            graphics.addProperty("particleSetting", settings.particleSetting);
            graphics.addProperty("vbo", OpenGlHelper.useVbo());
            graphics.addProperty("renderRegions", settings.ofRenderRegions);
            graphics.addProperty("fastRender", settings.ofFastRender);
            graphics.addProperty("dynamicLights", settings.ofDynamicLights);
            graphics.addProperty("shaderPack", Shaders.getShaderPackName());
            JsonArray packs = new JsonArray();
            settings.resourcePacks.forEach(packs::add);
            graphics.add("resourcePacks", packs);
            report.add("graphics", graphics);
        }

        private void start() {
            allocatedAtStart = allocatedBytes();
            started = System.nanoTime();
        }

        private long allocatedBytes() {
            return allocation == null ? -1 : allocation.getCurrentThreadAllocatedBytes();
        }

        private synchronized JsonObject summarize(long finished) {
            long allocatedAtEnd = allocatedBytes();
            double elapsedSeconds = (finished - started) / 1_000_000_000.0;
            report.addProperty("elapsedSeconds", elapsedSeconds);
            report.addProperty("frameSampleLimitReached", frameCount == frames.length);
            report.addProperty("fps", frameCount / elapsedSeconds);
            report.add("frameTime", summarizeTimes(frames, frameCount));
            report.add("integratedServerTickTime", summarizeTimes(ticks, tickCount));
            report.addProperty("glDrawSubmissions", drawCalls);
            report.addProperty("glDrawSubmissionsPerFrame", (double) drawCalls / frameCount);
            if (vulkanTerrainFrames > 0) {
                report.addProperty("vulkanTerrainFrames", vulkanTerrainFrames);
                report.addProperty("vulkanTerrainDrawCalls", vulkanTerrainDraws);
                report.addProperty("vulkanTerrainQuads", vulkanTerrainQuads);
                report.addProperty("vulkanTerrainCutoutQuads", vulkanTerrainCutoutQuads);
                report.addProperty("vulkanTerrainTranslucentQuads", vulkanTerrainTranslucentQuads);
                report.addProperty("vulkanEntityTriangles", vulkanEntityTriangles);
                report.addProperty("vulkanEntityShadowFrames", vulkanEntityShadowFrames);
                report.addProperty("vulkanEntityShadowQuads", vulkanEntityShadowQuads);
                report.addProperty("vulkanEntityFireFrames", vulkanEntityFireFrames);
                report.addProperty("vulkanEntityFireQuads", vulkanEntityFireQuads);
                report.addProperty("vulkanEntityHitboxFrames", vulkanEntityHitboxFrames);
                report.addProperty("vulkanEntityHitboxSegments", vulkanEntityHitboxSegments);
                report.addProperty("vulkanEntityLeashFrames", vulkanEntityLeashFrames);
                report.addProperty("vulkanEntityLeashTriangles", vulkanEntityLeashTriangles);
                report.addProperty("vulkanEntityNameTagFrames", vulkanEntityNameTagFrames);
                report.addProperty("vulkanEntityNameTagGlyphs", vulkanEntityNameTagGlyphs);
                report.addProperty("vulkanEntityTintFrames", vulkanEntityTintFrames);
                report.addProperty("vulkanEntityTintTriangles", vulkanEntityTintTriangles);
                report.addProperty("vulkanEntityNameTagThroughWallGlyphs", vulkanEntityNameTagThroughWallGlyphs);
                report.addProperty("vulkanBlockEntityTriangles", vulkanBlockEntityTriangles);
                report.addProperty("vulkanBlockEntityGlyphs", vulkanBlockEntityGlyphs);
                report.addProperty("vulkanParticleTriangles", vulkanParticleTriangles);
                report.addProperty("vulkanTerrainParticleTriangles", vulkanTerrainParticleTriangles);
                report.addProperty("vulkanTranslucentParticleTriangles", vulkanTranslucentParticleTriangles);
                report.addProperty("vulkanLitParticleTriangles", vulkanLitParticleTriangles);
                report.addProperty("vulkanRainQuads", vulkanRainQuads);
                report.addProperty("vulkanSnowQuads", vulkanSnowQuads);
                report.addProperty("vulkanHandFrames", vulkanHandFrames);
                report.addProperty("vulkanHandTriangles", vulkanHandTriangles);
                report.addProperty("vulkanHeldItemFrames", vulkanHeldItemFrames);
                report.addProperty("vulkanHeldItemTriangles", vulkanHeldItemTriangles);
                report.addProperty("vulkanHeldGlintFrames", vulkanHeldGlintFrames);
                report.addProperty("vulkanEntityOutlineFrames", vulkanEntityOutlineFrames);
                report.addProperty("vulkanEntityOutlineTriangles", vulkanEntityOutlineTriangles);
                report.addProperty("vulkanLightningFrames", vulkanLightningFrames);
                report.addProperty("vulkanLightningTriangles", vulkanLightningTriangles);
                report.addProperty("vulkanLightningFlashFrames", vulkanLightningFlashFrames);
                report.addProperty("vulkanArmorGlintFrames", vulkanArmorGlintFrames);
                report.addProperty("vulkanArmorGlintTriangles", vulkanArmorGlintTriangles);
                report.addProperty("vulkanHeldGlintTriangles", vulkanHeldGlintTriangles);
                report.addProperty("vulkanGuiGlintFrames", vulkanGuiGlintFrames);
                report.addProperty("vulkanGuiGlintTriangles", vulkanGuiGlintTriangles);
                report.addProperty("vulkanBlockOverlayQuads", vulkanBlockOverlayQuads);
                report.addProperty("vulkanWaterOverlayQuads", vulkanWaterOverlayQuads);
                report.addProperty("vulkanFireOverlayQuads", vulkanFireOverlayQuads);
                report.addProperty("vulkanSelectionSegments", vulkanSelectionSegments);
                report.addProperty("vulkanBlockDamageQuads", vulkanBlockDamageQuads);
                report.addProperty("vulkanWorldBorderFrames", vulkanWorldBorderFrames);
                report.addProperty("vulkanWorldBorderQuads", vulkanWorldBorderQuads);
                JsonArray borderStages = new JsonArray();
                vulkanWorldBorderStages.forEach(state -> borderStages.add(state.name()));
                report.add("vulkanWorldBorderStages", borderStages);
                report.addProperty("vulkanHudFrames", vulkanHudFrames);
                report.addProperty("vulkanHudTriangles", vulkanHudTriangles);
                report.addProperty("vulkanHudItemTriangles", vulkanHudItemTriangles);
                report.addProperty("vulkanGuiFrames", vulkanGuiFrames);
                report.addProperty("vulkanGuiTriangles", vulkanGuiTriangles);
                report.addProperty("vulkanGuiItemTriangles", vulkanGuiItemTriangles);
                report.addProperty("vulkanGuiGlyphs", vulkanGuiGlyphs);
                report.addProperty("vulkanAtlasSpriteUpdates", vulkanAtlasSpriteUpdates);
                report.addProperty("vulkanWaterSpriteUpdates", vulkanWaterSpriteUpdates);
                report.addProperty("vulkanLavaSpriteUpdates", vulkanLavaSpriteUpdates);
                report.addProperty("vulkanAtlasUploadBytes", vulkanAtlasUploadBytes);
                report.addProperty("vulkanSkyFrames", vulkanSkyFrames);
                report.addProperty("vulkanCloudFrames", vulkanCloudFrames);
                report.addProperty("vulkanEndSkyFrames", vulkanEndSkyFrames);
                report.addProperty("vulkanFogBackgroundFrames", vulkanFogBackgroundFrames);
            }
            report.addProperty("chunkBuildCount", buildCount);
            report.addProperty("chunkBuildTotalMs", buildNanos / 1_000_000.0);
            report.addProperty("chunkLayerUploadCount", uploadCount);
            report.addProperty("chunkLayerUploadTotalMs", uploadNanos / 1_000_000.0);
            if (allocatedAtStart >= 0 && allocatedAtEnd >= allocatedAtStart) {
                report.addProperty("clientThreadAllocatedBytesPerSecond", (allocatedAtEnd - allocatedAtStart) / elapsedSeconds);
            }

            return report;
        }
    }
}
